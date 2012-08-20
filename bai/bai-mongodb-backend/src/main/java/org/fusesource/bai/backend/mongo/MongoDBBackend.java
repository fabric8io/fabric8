/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.bai.backend.mongo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.camel.*;
import org.apache.camel.dataformat.xmljson.XmlJsonDataFormat;
import org.apache.camel.management.event.*;
import org.apache.camel.util.CamelContextHelper;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.bai.AuditConstants;
import org.fusesource.bai.backend.BAIAuditBackend;
import org.fusesource.bai.backend.BAIAuditBackendSupport;
import org.fusesource.bai.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

/**
 * MongoDB Business Activity Insight backend
 * Manages three types of collections: per-route collection, x-ray collection and debug
 * The per-route collection is the heart of BAI, and contains records following this structure:
 * { breadcrumbId: ,
 *   input: { },
 *   exchanges: { },
 *   endpointFailures: { },
 *   processorFailures: { },
 *   endpointRedeliveries: { },
 *   processorRedeliveries: { }
 * }
 * @author Raul Kripalani
 *
 */
public class MongoDBBackend extends BAIAuditBackendSupport implements BAIAuditBackend {

    private final static Logger LOG = LoggerFactory.getLogger(MongoDBBackend.class);
    
	private Mongo mongo;
	private String dbname;
	private DB db;
	private CamelContext context;
	private TypeConverter typeConverter;
	private Properties typeHints;
	private XmlJsonDataFormat xmlJson = new XmlJsonDataFormat();
    private boolean debug = true;

	@Override
	public void audit(AuditEvent ev) {
	    String endpointId = ev.endpointURI;
	    String srcContextId = ev.getExchange().getContext().getName();
	    String srcRouteId = ev.getExchange().getFromRouteId();

	    // do some basic logging
	    LOG.info("Received AuditEvent: " + ev + " | Extracted data: " + endpointId + ", " + srcContextId + ", " + srcRouteId);
        LOG.info("Breadcrumb ID for: " + ev + " is: " + ev.breadCrumbId, String.class);

        boolean handled = false;
		// a message is being sent
		if (ev.event instanceof ExchangeSendingEvent || ev.event instanceof ExchangeSentEvent || 
		        ev.event instanceof ExchangeCreatedEvent || ev.event instanceof ExchangeCompletedEvent) {
			digestExchangeEvent(ev);
			handled = true;
		} 
		// a message has failed
		else if (ev.event instanceof ExchangeFailedEvent) {
		    digestEndpointFailureEvent(ev);
	        handled = true;
		} 
		// a message is being redelivered
		else if (ev.event instanceof ExchangeRedeliveryEvent) {
		    if (ev.endpointURI != null) {
		        digestEndpointRedeliveryEvent(ev);
		    } else {
		        digestProcessorRedeliveryEvent(ev);
		    }
	        handled = true;
		}
		
		// add the entry to the meta collection - which tells us which routes a breadcrumbId has passed through
		if (handled) {
		    addToMetaCollection(ev);
		}
		
		// if debug is enabled, insert a record in the debug collection
		if (debug) {
		    createDebugRecord(ev);
		}
		
	}

    private void createDebugRecord(AuditEvent ev) {
        BasicDBObject object = new BasicDBObject();
        object.append("breadCrumbId", ev.breadCrumbId);
        object.append("eventtype", (ev.event).getClass().getName());
        object.append("endpointURI", ev.endpointURI);
        object.append("exchangeId", ev.getExchange().getExchangeId());
        object.append("exception", ev.exception == null ? null : ev.exception.toString());
        object.append("redelivered", ev.redelivered);
        object.append("timestamp", ev.timestamp);
        object.append("sourceContextId", ev.sourceContextId);
        object.append("sourceRouteId", ev.sourceRouteId);
        object.append("inBody", ev.event.getExchange().getIn().getBody(String.class));
        object.append("outBody", ev.event.getExchange().hasOut() ? ev.event.getExchange().getOut().getBody(String.class) : null);

        db.getCollection("baievents").insert(object);
    }

    /*
     * Assumes that events are processed in order, i.e. the first ExchangeCreatedEvent received is the one from the route's consumer, ExchangeSending events are not received before
     * the ExchangeCreated, etc.
     */
    
    private void digestExchangeEvent(AuditEvent ev) {
        AbstractExchangeEvent event = ev.event;
        if (event instanceof ExchangeCreatedEvent) {
            digestExchangeCreatedEvent(ev);
        }
        
        // if the Exchange that has just completed is the same that started the route (i.e. the one from the ExchangeCreated event we accepted)
        if (event instanceof ExchangeCompletedEvent) {
            digestExchangeCompletedEvent(ev);
        }
        
        if (event instanceof ExchangeSendingEvent) {
            digestExchangeSendingEvent(ev);
        }
        
        if (event instanceof ExchangeSentEvent) {
            digestExchangeSentEvent(ev);
        }
        
	}

    

    private void digestExchangeCreatedEvent(AuditEvent ev) {
        // filter: { _id : <breadcrumbId> }
        // updateObj: { $push : { exchanges: { in: <inmessage>, inTimestamp: <inTimestamp> } } }
        // an exchange has been created (by a consumer or by an EIP - we probably don't want to track the latter, so we need to find a way)
        // perhaps: if an exchange already exists with the same unit of work, of if an exchange with created='yes' already exists for that route, discard this message
        
        // ExchangeCreated will create a record in the per-route collection, *ONLY IF* a record doesn't already exist
        // if a record exists, it means that an EIP or processor has created a new Exchange, which will be sent later on, so we'll intercept it at that event
        Object inMessage = null;
        try {
            inMessage = convertPayload(ev.getExchange().getIn().getBody(), ev.getExchange());
        } catch (Exception e) {
            // nothing
        }
    
        DBObject exchObj = BasicDBObjectBuilder.start()
                .append("endpointUri", ev.endpointURI)
                .append("startTimestamp", ev.timestamp)
                .append("status", "in_progress")
                .append("exchangeId", ev.event.getExchange().getExchangeId())
                .append("exchangePattern", ev.event.getExchange().getPattern().toString())
                .append("in", inMessage)
                .append("dispatchId", ev.event.getExchange().getProperty(AuditConstants.DISPATCH_ID, String.class)).get();
        
        DBObject toInsert = BasicDBObjectBuilder.start()
                .append("_id", ev.breadCrumbId)
                .append("input", 
                        Arrays.asList(exchObj)).get();
        
        addCurrentRouteIdIfNeeded(ev, exchObj);
        // insert the record => if it already exists, Mongo will ignore the insert
        collectionFor(ev).insert(toInsert);
    }

    private void digestExchangeCompletedEvent(AuditEvent ev) {
        DBObject filter = BasicDBObjectBuilder.start()
                .append("_id", ev.breadCrumbId)
                .append("input.endpointUri", ev.endpointURI)
                .append("input.exchangeId", ev.getExchange().getExchangeId())
                .append("exchanges.dispatchId", ev.event.getExchange().getProperty(AuditConstants.DISPATCH_ID, String.class)).get();
        
        DBObject toApply = new BasicDBObject();
        toApply.put("$set", new BasicDBObject());
        DBObject toSet = (BasicDBObject) toApply.get("$set");
        toSet.put("input.$.endTimestamp", ev.timestamp);
        toSet.put("input.$.status", "finished");
        
        // TODO: what if the exchange pattern changed while routing?
        // if the exchange pattern is InOut, first check if there's an out message, if not, dump the in message as the out (since this is what Camel's PipelineProcessor
        // will do internally anyway)
        if (ev.event.getExchange().getPattern() == ExchangePattern.InOut) {
            Object outBody = null;
            try {
                outBody = ev.getExchange().hasOut() ? ev.getExchange().getOut().getBody() : ev.getExchange().getIn().getBody();
                // it's okay to insert a null value if the he pattern was InOut
                toSet.put("input.$.out", convertPayload(outBody, ev.getExchange()));
                toSet.put("input.$.originalOut", typeConverter.convertTo(String.class, outBody));
            } catch (Exception e) {
                // nothing
            }
        }
        // update the record, only if the filter criteria is met
        collectionFor(ev).update(filter, toApply);
    }

    private void digestExchangeSendingEvent(AuditEvent ev) {
        DBObject filter = new BasicDBObject();
        filter.put("_id", ev.breadCrumbId);
        DBObject toApply = new BasicDBObject();
        toApply.put("$push", new BasicDBObject("exchanges", new BasicDBObject()));
        DBObject exchangeToPush = (BasicDBObject) ((BasicDBObject) toApply.get("$push")).get("exchanges");
        exchangeToPush.put("endpointUri", ev.endpointURI);
        exchangeToPush.put("startTimestamp", ev.timestamp);
        exchangeToPush.put("status", "in_progress");
        exchangeToPush.put("exchangeId", ev.event.getExchange().getExchangeId());
        exchangeToPush.put("exchangePattern", ev.event.getExchange().getPattern().toString());
        exchangeToPush.put("dispatchId", ev.event.getExchange().getProperty(AuditConstants.DISPATCH_ID, String.class));
        addCurrentRouteIdIfNeeded(ev, exchangeToPush);
        try {
            exchangeToPush.put("in", convertPayload(ev.getExchange().getIn().getBody(), ev.getExchange()));
        } catch (Exception e) {
            // nothing
        }
         
        // update the record
        collectionFor(ev).update(filter, toApply);
    }

    private void digestExchangeSentEvent(AuditEvent ev) {
        DBObject filter = BasicDBObjectBuilder.start()
                .append("_id", ev.breadCrumbId)
                .append("exchanges.endpointUri", ev.endpointURI)
                .append("exchanges.exchangeId", ev.getExchange().getExchangeId())
                .append("exchanges.dispatchId", ev.event.getExchange().getProperty(AuditConstants.DISPATCH_ID, String.class)).get();
        
        DBObject toApply = BasicDBObjectBuilder.start()
                .push("$set")
                    .append("exchanges.$.endTimestamp", ev.timestamp)
                    .append("exchanges.$.status", "finished").get();
        
        // if the exchange pattern is InOut, first check if there's an out message, if not, dump the in message as the out (since this is what Camel's PipelineProcessor
        // will do internally anyway)
        if (ev.event.getExchange().getPattern() == ExchangePattern.InOut) {
            Object outBody = null;
            try {
               outBody = ev.getExchange().hasOut() ? ev.getExchange().getOut().getBody() : ev.getExchange().getIn().getBody();
               // it's okay to insert a null value if the he pattern was InOut
               ((BasicDBObject) toApply.get("$set")).put("exchanges.$.out", convertPayload(outBody, ev.getExchange()));                  
            } catch (Exception e) {
                // nothing
            }
        }
        // update the record, only if the filter criteria is met
        collectionFor(ev).update(filter, toApply);
    }
    
    private void digestEndpointFailureEvent(AuditEvent ev) {
        DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
        // 1. push the failure into endpointFailures
        DBObject toUpdate = BasicDBObjectBuilder.start()
                .push("$push")
                    .push("endpointFailures")
                        .append("endpointUri", ev.endpointURI)
                        .append("exception", ev.exception.toString())
                        .append("timestamp", ev.timestamp).get();

        collectionFor(ev).update(filter, toUpdate);
        addCurrentRouteIdIfNeeded(ev, (DBObject) ((DBObject) toUpdate.get("$push")).get("endpointFailures"));
        
        // 2. Then set the status of the exchange to failed - if it was an exchange sent from this route
        filter.put("exchanges.endpointUri", ev.endpointURI);
        filter.put("exchanges.exchangeId", ev.event.getExchange().getExchangeId());
        filter.put("exchanges.dispatchId", ev.event.getExchange().getProperty(AuditConstants.DISPATCH_ID, String.class));
        toUpdate = BasicDBObjectBuilder.start()
                .push("$set")
                .append("exchanges.$.status", "failed")
                .append("exchanges.$.failTimestamp", ev.timestamp).get();
        
        // if an exception is informed, we add it too
        if (ev.exception != null) {
            ((BasicDBObject) toUpdate.get("$set")).put("exchanges.$.exception", ev.exception.toString());
        }
        
        collectionFor(ev).update(filter, toUpdate);
        
        // 3. Then set the status of the exchange to failed - if it was the incoming exchange into the route
        filter.put("in.endpointUri", ev.endpointURI);
        filter.put("in.exchangeId", ev.event.getExchange().getExchangeId());
        filter.put("in.dispatchId", ev.event.getExchange().getProperty(AuditConstants.DISPATCH_ID, String.class));
        toUpdate = BasicDBObjectBuilder.start()
                .push("$set")
                .append("in.$.status", "failed")
                .append("in.$.failTimestamp", ev.timestamp).get();
        
        // if an exception is informed, we add it too
        if (ev.exception != null) {
            ((BasicDBObject) toUpdate.get("$set")).put("in.$.exception", ev.exception.toString());
        }
                
        collectionFor(ev).update(filter, toUpdate);
        
	}
	
	private void digestEndpointRedeliveryEvent(AuditEvent ev) {
	    DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
        // we don't know what processor caused it, because this info is not on the event, so just push an element into the processorRedeliveries array for the time being
        DBObject toPush = BasicDBObjectBuilder.start()
                .push("$push")
                    .push("endpointRedeliveries")
                        .append("exchangeId", ev.getExchange().getExchangeId())
                        .append("endpointURI", ev.endpointURI)
                        .append("timestamp", ev.timestamp)
                        .append("exception", ev.exception.toString())
                        .append("attempt", ev.getExchange().getProperty(Exchange.REDELIVERY_COUNTER)).get();
        collectionFor(ev).update(filter, toPush);
	}
	
	   private void digestProcessorRedeliveryEvent(AuditEvent ev) {
	       DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
	       // we don't know what processor caused it, because this info is not on the event, so just push an element into the processorRedeliveries array for the time being
	       DBObject toPush = BasicDBObjectBuilder.start()
	               .push("$push")
	                   .push("processorRedeliveries")
	                       .append("exchangeId", ev.getExchange().getExchangeId())
	                       .append("timestamp", ev.timestamp)
	                       .append("exception", ev.exception.toString())
	                       .append("attempt", ev.getExchange().getProperty(Exchange.REDELIVERY_COUNTER)).get();
	       
	       collectionFor(ev).update(filter, toPush);
	           
	   }
	
	private void addCurrentRouteIdIfNeeded(AuditEvent ev, DBObject dbo) {
        if (ev.currentRouteId != null && !ev.currentRouteId.equals(ev.sourceRouteId)) {
            dbo.put("currentRouteId", ev.currentRouteId);
        }
    }

    private void addToMetaCollection(AuditEvent ev) {
	    DBObject filter = new BasicDBObject("_id", ev.breadCrumbId);
	    DBObject dbo = new BasicDBObject("$addToSet", new BasicDBObject("routes", ev.sourceContextId + "." + ev.sourceRouteId));
	    // possible collection names: eagleView, hawkView
	    DBCollection collection = db.getCollection("exchangeXray");
	    collection.update(filter, dbo, true, false);
	}
	
    private DBCollection collectionFor(AuditEvent ev) {
        return db.getCollection(ev.sourceContextId + "." + ev.sourceRouteId);
    }

	/**
	 * Converts the payload into something that we know for sure can go into Mongo (DBObject), primitive value, etc.
	 * @param payload
	 * @param exchange
	 * @return
	 * @throws Exception
	 */
	private Object convertPayload(Object payload, Exchange exchange) throws Exception {
        Expression expression = getStoreBodyExpression();
        if (expression != null) {
            payload = expression.evaluate(exchange, Object.class);
        }
	    if (payload == null) {
	        return null;
	    }
        // TODO should we try convert to a DBObject first?

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
