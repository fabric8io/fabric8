package com.fusesource.bai.backend.mongo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.dataformat.xmljson.XmlJsonDataFormat;
import org.apache.camel.management.event.ExchangeCompletedEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeFailedEvent;
import org.apache.camel.management.event.ExchangeRedeliveryEvent;
import org.apache.camel.management.event.ExchangeSendingEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fusesource.bai.backend.BAIAuditBackend;
import com.fusesource.bai.event.AuditEvent;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

/**
 * MongoDB Business Activity Insight backend
 * @author Raul Kripalani
 *
 */
public class MongoDBBackend implements BAIAuditBackend {

    private final static Logger LOG = LoggerFactory.getLogger(MongoDBBackend.class);
    
	private Mongo mongo;
	private String dbname;
	private DB db;
	private CamelContext context;
	private TypeConverter typeConverter;
	private Properties typeHints;
	private XmlJsonDataFormat xmlJson = new XmlJsonDataFormat();
	
	@Override
	public void audit(AuditEvent event) {
	    String endpointId = event.endpointURI;
	    String srcContextId = event.getExchange().getContext().getName();
	    String srcRouteId = event.getExchange().getFromRouteId();

	    LOG.info("Received AuditEvent: " + event + " | Extracted data: " + endpointId + ", " + srcContextId + ", " + srcRouteId);
        LOG.info("Breadcrumb ID for: " + event + " is: " + event.breadCrumbId, String.class);

        boolean handled = false;
		// a message is being sent
		if (event.event instanceof ExchangeSendingEvent || event.event instanceof ExchangeSentEvent || 
		        event.event instanceof ExchangeCreatedEvent || event.event instanceof ExchangeCompletedEvent) {
			digestExchangeEvent(event);
			handled = true;
		} 
		// a message has failed
		else if (event.event instanceof ExchangeFailedEvent) {
		    digestFailureEvent(event);
	        handled = true;
		} 
		// a message is being redelivered
		else if (event.event instanceof ExchangeRedeliveryEvent) {
		    digestRedeliveryEvent(event);
	        handled = true;
		}
		
		if (handled) {
		    addToMetaCollection(event);
		}
	}
	
	private void digestExchangeEvent(AuditEvent ev) {	    
        DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
        DBObject toApply = new BasicDBObject();
        // filter: { _id : <breadcrumbId> }
        // updateObj: { $push : { exchanges: { in: <inmessage>, inTimestamp: <inTimestamp> } } }
        if (ev.event instanceof ExchangeSendingEvent || ev.event instanceof ExchangeCreatedEvent) {
            toApply.put("endpointUri", ev.endpointURI);
            toApply.put("startTimestamp", ev.timestamp);
            toApply.put("status", "in_progress");
            try {
                toApply.put("in", convertPayload(ev.getExchange().getIn().getBody(), ev.getExchange()));
            } catch (Exception e) {
                // TODO: handle exception
            }
            DBObject exchanges = new BasicDBObject("exchanges", toApply);
            DBObject updateObj = new BasicDBObject("$push", exchanges);
            // if the exchange is being created, 
            updateDatabase(ev, filter, updateObj);
        }
        // filter: { _id : <breadcrumbId>, exchanges.endpointUri: <endpointUri> }
        // updateObj: { $set : { exchanges.$.in: { in: <inmessage>, inTimestamp: <inTimestamp> } } }
        else if (ev.event instanceof ExchangeSentEvent || ev.event instanceof ExchangeCompletedEvent) {
            // if the same endpoint URI is hit twice, two exchange entries will be created, but when marking one as 'finished', we won't know which one is the correct one
            // correlate by exchange id won't work because it may be the same exchange; think of <to uri="log:blah" /> followed by <to uri="log:blah" />
            filter.put("exchanges.endpointUri", ev.endpointURI);
            filter.put("exchanges.endTimestamp", new BasicDBObject("$exists", false));
            toApply.put("exchanges.$.endTimestamp", ev.timestamp);
            toApply.put("exchanges.$.status", "finished");
            try {
                Object outBody = ev.getExchange().hasOut() ? ev.getExchange().getOut().getBody() : null;
                if (outBody != null) {
                    toApply.put("exchanges.$.out", convertPayload(outBody, ev.getExchange()));
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
            DBObject updateObj = new BasicDBObject("$set", toApply);
            updateDatabase(ev, filter, updateObj);
        }
	}
	
	private void digestFailureEvent(AuditEvent ev) {
        DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
        // 1. Add an array element to failures with the exception
        DBObject dboj = new BasicDBObject("endpointUri", ev.endpointURI);
        dboj.put("exception", ev.exception.toString());
        dboj.put("timestamp", ev.timestamp);
        DBObject updateObj = new BasicDBObject("$push", new BasicDBObject("failures", dboj));
        updateDatabase(ev, filter, updateObj);
        
        // 2. Then set the status of the exchange to failed
        filter.put("exchanges.endpointUri", ev.endpointURI);
        dboj = new BasicDBObject("exchanges.$.status", "failed");
        dboj.put("exchanges.$.failTimestamp", ev.timestamp);
        updateObj = new BasicDBObject("$set", dboj);
        updateDatabase(ev, filter, updateObj);
	}
	
	private void digestRedeliveryEvent(AuditEvent ev) {
	    DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
	    // failure endpoint
        filter.put("redeliveries.endpointUri", ev.endpointURI);
        DBCollection collection = db.getCollection(ev.sourceContextId + "." + ev.sourceRouteId);
        DBObject updateObj = new BasicDBObject();
        // TODO: need to figure out how to make this atomic, otherwise it is easily subject to dirty read
        // some redeliveries for that endpoint have already happened, just increment the counter
        if (collection.count(filter) > 0) {
            updateObj.put("$inc", new BasicDBObject("redeliveries.$.attempts", 1));
        } else {
            filter.removeField("redeliveries.endpointUri");
            DBObject content = new BasicDBObject("endpointUri", ev.endpointURI);
            content.put("attempts", 1);
            updateObj.put("$push", new BasicDBObject("redeliveries", content));
        }
        updateDatabase(ev, filter, updateObj);

	}
	
	private void addToMetaCollection(AuditEvent ev) {
	    DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
	    DBObject dbo = new BasicDBObject("$addToSet", new BasicDBObject("routes", ev.sourceContextId + "." + ev.sourceRouteId));
	    // possible collection names: eagleView, hawkView
	    DBCollection collection = db.getCollection("exchangeXray");
	    collection.update(filter, dbo, true, false);
	}
	
	private void updateDatabase(AuditEvent ev, DBObject filter, DBObject dbo) {
		// obtain a collection with name contextId.routeId
		DBCollection collection = db.getCollection(ev.sourceContextId + "." + ev.sourceRouteId);
		// update the collection using upsert
		collection.update(filter, dbo, true, false);
	}
	

	/**
	 * Converts the payload into something that we know for sure can go into Mongo (DBObject), primitive value, etc.
	 * @param payload
	 * @param exchange
	 * @return
	 * @throws Exception
	 */
	private Object convertPayload(Object payload, Exchange exchange) throws Exception {
	    // have a String representation handy
	    String s = typeConverter.convertTo(String.class, payload);
        // 1. JSON if it starts with {
	    if (s.startsWith("{")) {
	        Object answer = null;
    	    try {
    	        answer = JSON.parse(s);
    	    } catch (JSONParseException ex) {
    	        LOG.info("Attempt to convert " + payload + " to JSON failed");
    	        // do nothing
    	    }
    	    
    	    if (answer != null) {
    	        return answer;
    	    }
	    }
	    
	    // 2. XML if it starts with <
	    if (s.startsWith("<")) {
    	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	    try {
    	        xmlJson.marshal(exchange, payload, baos);
    	        if (baos != null && baos.size() > 0) {
    	            // first convert to String, then to DBObject
    	            String json = typeConverter.convertTo(String.class, baos);
    	            return JSON.parse(json);
    	        }
    	    } catch (Exception e) {
    	        System.out.println(e);
    	    }
	    }
	    // 3. String, if it was originally a String
	    if (payload instanceof String) {
	        return payload;
	    }
	    // 4. Object
	    // MongoDbBasicConverters has a converter that uses Jackson to convert any object to JSON, by first turning it into a Map
	    return typeConverter.convertTo(DBObject.class, payload);
    }

	public void init() throws Exception {
		db = mongo.getDB(dbname);
		typeConverter = context.getTypeConverter();
		// load the type hints
		typeHints = new Properties();
		try {
			typeHints.load(this.getClass().getClassLoader().getResourceAsStream("typeHints.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		xmlJson.setForceTopLevelObject(true);
		ServiceHelper.startService(xmlJson);
	}
	
	
	public Mongo getMongo() {
		return mongo;
	}

	public void setMongo(Mongo mongo) {
		this.mongo = mongo;
	}

	public String getDbname() {
		return dbname;
	}

	public void setDbname(String dbname) {
		this.dbname = dbname;
	}

	public CamelContext getContext() {
		return context;
	}

	public void setContext(CamelContext context) {
		this.context = context;
	}
   
}
