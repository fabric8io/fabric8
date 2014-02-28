# Example Camel SAP

This example performs a Flight Booking in SAP from Camel using the SAP Camel Component and the Flight Data Application in SAP. It demonstrates:

* How to configure the `SAP Camel Component`.
* How to process an inbound request from SAP through an `SAP Server Endpoint`.
* How to build and return an outbound response to SAP through an `SAP Server Endpoint`.
* How to build and send an outbound request to SAP through an `SAP Destination Endpoint`.
* How to process an inbound response from SAP through an `SAP Destination Endpoint`.
* How to unmarshal and marshal SAP requests and responses into custom JAXB beans. 
* How to establish an `SAP Transaction Context` within a Camel route.

There is one top-level route containing four sub-routes in this example:

* The top-level route, `route0`, implments the Function Module `BOOK_FLIGHT` which peforms a Flight Booking in SAP. This route receives, through the `SAP Server Endpoint` at the start of the route, sRFC requests from SAP and returns sRFC responses to SAP. It invokes the four sub-routes to perform the flight booking.
* The sub-route `route1` makes a series of calls into SAP, using `SAP Destination Endpoints`, to find a Flight Connection matching the Request for the Flight Booking
* The sub-route `route2` makes a call into SAP, using an `SAP Destination Endpoint`, to get detailed infomation about the Flight Customer (Travel Agent) requesting the Flight Booking
* The sub-route `route3` gathers information about the Passenger for the Flight Booking.
* Finally the sub-route `route4` makes a call into SAP, using an `SAP Destination Endpoint` in an `SAP Transaction Context`, to create the Flight Booking in SAP. 

### How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. It is also assumed that you have an SAP instance available with the following installed:
	1. Ensure the [`Flight Data Application`](http://help.sap.com/saphelp_erp60_sp/helpdata/en/db/7c623cf568896be10000000a11405a/content.htm) of the `ABAP Workbench` has been setup. 
	1. Ensure that an RFC Destination has been [setup for registration of the SAP Component](http://help.sap.com/saphelp_nw73ehp1/helpdata/en/48/c7b790da5e31ebe10000000a42189b/content.htm?frameset=/en/48/a98f837e28674be10000000a421937/frameset.htm) with the instance's [SAP Gateway](http://help.sap.com/saphelp_nw70ehp3/helpdata/en/31/42f34a7cab4cb586177f85a0cf6780/frameset.htm). The destination should have a Connection Type `T` and the following `Technical Settings`:
		1. **Activation Type** : `Registered Server Program`
		1. **Program ID** : `JCO_SERVER`
1. Edit the `example-camel-sap` profile 

		fabric:profile-edit example-camel-sap
		
	1. And add the SAP Java Connector libraries to the profile.

			lib.sapjco3.jar=http://host/path/to/libraries/sapjco3.jar
			lib.sapjco3.jnilib=http://host/path/to/libraries/<native-lib>
		
	1. Ensure that the connection properties of the `destinationData` and `serverData` beans in the profile's `camel.xml` file match those of your SAP instance:
	
			<bean id="destinationData"
				class="org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataImpl">
				<property name="ashost" value="myhost" />
				<property name="sysnr" value="42" />
				<property name="client" value="001" />
				<property name="user" value="developer" />
				<property name="passwd" value="ch4ngeme" />
				<property name="lang" value="en" />
			</bean>

			<bean id="serverData"
				class="org.fusesource.camel.component.sap.model.rfc.impl.ServerDataImpl">
				<property name="gwhost" value="myhost" />
				<property name="gwserv" value="3342" />
				<property name="progid" value="JCO_SERVER" />
				<property name="repositoryDestination" value="nplDest" />
				<property name="connectionCount" value="2" />
				<property name="trace" value="1"/>
			</bean>

1. Enable DEBUG logging in the SAP Producer and Consumer to monitor message traffic:

		config:edit org.ops4j.pax.logging
		config:propappend log4j.logger.org.fusesource.camel.component.sap.SAPProducer debug
		config:propappend log4j.logger.org.fusesource.camel.component.sap.SAPProducer debug
		config:update	

1. Create a new child container and deploy the `example-camel-sap` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile example-camel-sap root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.

1. Log into the `sap` container using the `fabric:container-connect` command, as follows:

		fabric:container-connect mychild
				
1. Invoke the camel route from SAP using an ABAP program such as the following:

		*&---------------------------------------------------------------------*
		*& Report  ZBOOK_FLIGHT
		*&
		*&---------------------------------------------------------------------*
		*&
		*&
		*&---------------------------------------------------------------------*

		REPORT  ZBOOK_FLIGHT.

		TYPES: BEGIN OF FLTINFO_STRUCTURE,
        		FLIGHTTIME(10) TYPE N,
        		CITYFROM(20) TYPE C,
        		DEPDATE TYPE D,
        		DEPTIME TYPE T,
        		CITYTO(20) TYPE C,
        		ARRDATE TYPE D,
        		ARRTIME TYPE T,
  			END OF FLTINFO_STRUCTURE.
		
		TYPES: BEGIN OF CONNECTION_INFO_STRUCTURE,
        		CONNID(1) TYPE N,
        		AIRLINE(20) TYPE C,
        		PLANETYPE(10) TYPE C,
        		CITYFROM(20) TYPE C,
        		DEPDATE TYPE D,
        		DEPTIME TYPE T,
        		CITYTO(20) TYPE C,
        		ARRDATE TYPE D,
        		ARRTIME TYPE T,
  		END OF CONNECTION_INFO_STRUCTURE.

		DATA: RFCDEST LIKE RFCDES-RFCDEST VALUE 'NONE'.

		DATA: RFC_MESS(128).

		DATA:  TRIPNUMBER(8) TYPE N VALUE '01234567',
       		TICKET_PRICE(12) TYPE P DECIMALS 4,
       		TICKET_TAX(12) TYPE P DECIMALS 4,
       		CURRENCY(5) TYPE C,
       		FLTINFO TYPE FLTINFO_STRUCTURE.

		DATA:  CONNECTION_INFO TYPE ZCONNECTION_INFO_STRUCTURE.

		DATA: INT_CONNECTION_INFO TYPE ZCONNECTION_INFO_TABLE.
		
		PARAMETERS: AGENCYNM(8) TYPE N DEFAULT 00000110,
            		CUSTNAME(25) TYPE C DEFAULT 'James Legrand' LOWER CASE,
            		FLTDATE TYPE D DEFAULT '20140319',
            		DESTFROM(3)   TYPE C DEFAULT 'SFO',
           			DESTTO(3)  TYPE C DEFAULT 'FRA',
            		PASSFORM(15) TYPE C LOWER CASE,
            		PASSNAME(25) TYPE C LOWER CASE,
            		PASSBIRT TYPE D.

		RFCDEST = 'JCOSERVER01'.

		CALL FUNCTION 'BOOK_FLIGHT'
  		DESTINATION RFCDEST
  		EXPORTING
    		CUSTNAME = CUSTNAME
    		PASSFORM = PASSFORM
   		PASSNAME = PASSNAME
    		PASSBIRTH = PASSBIRT
    		FLIGHTDATE = FLTDATE
    		TRAVELAGENCYNUMBER = AGENCYNM
    		DESTINATION_FROM = DESTFROM
    		DESTINATION_TO = DESTTO
  		IMPORTING
    		TRIPNUMBER = TRIPNUMBER
    		TICKET_PRICE = TICKET_PRICE
    		TICKET_TAX = TICKET_TAX
    		CURRENCY = CURRENCY
    		PASSFORM = PASSFORM
    		PASSNAME = PASSNAME
    		PASSBIRTH = PASSBIRT
    		FLTINFO = FLTINFO
    		CONNINFO = INT_CONNECTION_INFO.

		IF SY-SUBRC NE 0.

		WRITE: / 'Call ZBOOK_FLIGHT SY-SUBRC = ', SY-SUBRC.
		WRITE: / RFC_MESS.

		ELSE.

		WRITE: / 'Passenger: ', PASSFORM RIGHT-JUSTIFIED, ' ', PASSNAME LEFT-JUSTIFIED.

		ULINE.

		WRITE:
       		/ 'Trip Number:  ', TRIPNUMBER LEFT-JUSTIFIED,
       		/ 'Ticket Price: ', TICKET_PRICE DECIMALS 2 LEFT-JUSTIFIED,
       		/ 'Ticket Tax: ', TICKET_TAX DECIMALS 2 LEFT-JUSTIFIED,
       		/ 'Currency: ', CURRENCY LEFT-JUSTIFIED.

		ULINE.

		WRITE: / 'Flight Information',
       		/5 'Flight Time', 25 'Departure City', 55 'Departure Date', 75 'Departure Time', 95 			'Arrival City', 125 'Arrival Date', 145 'Arrival Time',
       		/5(10) FLTINFO-FLIGHTTIME, 25(20) FLTINFO-CITYFROM, 55(10) FLTINFO-DEPDATE, 75(10) 		FLTINFO-DEPTIME, 95(20) FLTINFO-CITYTO, 125(10) FLTINFO-ARRDATE, 145(10) FLTINFO-ARRTIME.

		ULINE.
		WRITE: / 'Flight Connections'.

		WRITE: /5 'Connection ID', 20 'Airline', 40 'Plane Type', 60 'Departure City', 90 'Departure 			Date', 110 'Departure Time', 130 'Arrival City', 160 'Arrival Date', 180 'Arrival Time'.

		LOOP AT INT_CONNECTION_INFO INTO CONNECTION_INFO.

		WRITE: /5 CONNECTION_INFO-CONNID, 20 CONNECTION_INFO-AIRLINE, 40 CONNECTION_INFO-PLANETYPE, 			60 CONNECTION_INFO-CITYFROM, 90 CONNECTION_INFO-DEPDATE, 110 CONNECTION_INFO-DEPTIME, 130 		CONNECTION_INFO-CITYTO,
          160 CONNECTION_INFO-ARRDATE, 180 CONNECTION_INFO-ARRTIME.

		ENDLOOP.

		ENDIF.

1. View the container log using the `log:tail` command as follows:

		log:tail
		
	You should see output like the following in the log:

		2013-11-05 10:45:00,478 | INFO  | cherWorkerThread | SAPComponent                     | onent$ServerStateChangedListener  111 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | >>> Server state changed from STARTED to ALIVE on JCO_SERVER
		2013-11-05 11:09:50,643 | DEBUG | CoServerThread-1 | SAPConsumer                      | .camel.component.sap.SAPConsumer   67 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Handling request for RFC 'BOOK_FLIGHT'
		2013-11-05 11:09:50,687 | DEBUG | CoServerThread-1 | SAPConsumer                      | .camel.component.sap.SAPConsumer   78 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Request: <?xml version="1.0" encoding="ASCII"?>
		<BOOK_FLIGHT:Request xmlns:BOOK_FLIGHT="http://sap.fusesource.org/rfc/nplServer/BOOK_FLIGHT" CUSTNAME="James Legrand" PASSFORM="Mr" PASSNAME="Travelin Joe" PASSBIRTH="1990-03-17T00:00:00.000-0500" FLIGHTDATE="2014-03-19T00:00:00.000-0400" TRAVELAGENCYNUMBER="00000110" DESTINATION_FROM="SFO" DESTINATION_TO="FRA"/>

		2013-11-05 11:09:54,838 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   49 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Calling 'BAPI_FLCONN_GETLIST' RFC
		2013-11-05 11:09:54,839 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   50 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Request: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLCONN_GETLIST:Request xmlns:BAPI_FLCONN_GETLIST="http://sap.fusesource.org/rfc/NPL/BAPI_FLCONN_GETLIST" TRAVELAGENCY="00000110">
  			<DESTINATION_FROM AIRPORTID="SFO"/>
  			<DESTINATION_TO AIRPORTID="FRA"/>
  			<DATE_RANGE>
    			<row SIGN="I" OPTION="EQ" LOW="2014-03-19T00:00:00.000-0400"/>
  			</DATE_RANGE>
		</BAPI_FLCONN_GETLIST:Request>

		2013-11-05 11:09:55,383 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   58 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Response: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLCONN_GETLIST:Response xmlns:BAPI_FLCONN_GETLIST="http://sap.fusesource.org/rfc/NPL/BAPI_FLCONN_GETLIST">
  			<DATE_RANGE>
    			<row SIGN="I" OPTION="EQ" LOW="2014-03-19T00:00:00.000-0400"/>
  			</DATE_RANGE>
  			<EXTENSION_IN/>
  			<EXTENSION_OUT/>
  			<FLIGHT_CONNECTION_LIST>
    			<row AGENCYNUM="00000110" FLIGHTCONN="0002" FLIGHTDATE="2014-03-19T00:00:00.000-0400" AIRPORTFR="SFO" CITYFROM="SAN FRANCISCO" AIRPORTTO="FRA" CITYTO="FRANKFURT" NUMHOPS="2" DEPTIME="1970-01-01T16:00:00.000-0500" ARRTIME="1970-01-01T05:35:00.000-0500" ARRDATE="2014-03-22T00:00:00.000-0400" FLIGHTTIME="3155"/>
  			</FLIGHT_CONNECTION_LIST>
  			<RETURN>
    			<row TYPE="S" ID="BC_IBF" NUMBER="000" MESSAGE="Method was executed successfully" LOG_NO="" LOG_MSG_NO="000000" MESSAGE_V1="" MESSAGE_V2="" MESSAGE_V3="" MESSAGE_V4="" PARAMETER="" FIELD="" SYSTEM="NPLCLNT001"/>
  			</RETURN>
		</BAPI_FLCONN_GETLIST:Response>

		2013-11-05 11:09:55,661 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   49 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Calling 'BAPI_FLCONN_GETDETAIL' RFC
		2013-11-05 11:09:55,661 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   50 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Request: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLCONN_GETDETAIL:Request xmlns:BAPI_FLCONN_GETDETAIL="http://sap.fusesource.org/rfc/NPL/BAPI_FLCONN_GETDETAIL" CONNECTIONNUMBER="0002" FLIGHTDATE="2014-03-19T00:00:00.000-0400" NO_AVAILIBILITY="" TRAVELAGENCYNUMBER="00000110"/>

		2013-11-05 11:09:55,938 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   58 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Response: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLCONN_GETDETAIL:Response xmlns:BAPI_FLCONN_GETDETAIL="http://sap.fusesource.org/rfc/NPL/BAPI_FLCONN_GETDETAIL">
  			<CONNECTION_DATA AGENCYNUM="00000110" FLIGHTCONN="0002" FLIGHTDATE="2014-03-19T00:00:00.000-0400" AIRPORTFR="SFO" CITYFROM="SAN FRANCISCO" AIRPORTTO="FRA" CITYTO="FRANKFURT" NUMHOPS="2" DEPTIME="1970-01-01T16:00:00.000-0500" ARRTIME="1970-01-01T05:35:00.000-0500" ARRDATE="2014-03-22T00:00:00.000-0400" FLIGHTTIME="3155"/>
  			<PRICE_INFO PRICE_ECO1="1878.5700" PRICE_ECO2="1408.9300" PRICE_ECO3="375.7100" PRICE_BUS1="3757.1400" PRICE_BUS2="2817.8600" PRICE_BUS3="751.4200" PRICE_FST1="5635.7100" PRICE_FST2="4226.7900" PRICE_FST3="1127.1500" TAX="93.9300" CURR="EUR" CURR_ISO="EUR"/>
  			<AVAILIBILITY>
    			<row HOP="1" ECONOMAX="380" ECONOFREE="270" BUSINMAX="41" BUSINFREE="38" FIRSTMAX="18" FIRSTFREE="17"/>
    			<row HOP="2" ECONOMAX="280" ECONOFREE="177" BUSINMAX="22" BUSINFREE="21" FIRSTMAX="10" FIRSTFREE="9"/>
  			</AVAILIBILITY>
  			<EXTENSION_IN/>
  			<EXTENSION_OUT/>
  			<FLIGHT_HOP_LIST>
    			<row HOP="1" AIRLINEID="SQ" AIRLINE="Singapore Airlines" CONNECTID="0015" AIRPORTFR="SFO" CITYFROM="SAN FRANCISCO" CTRYFR="US" CTRYFR_ISO="US" AIRPORTTO="SIN" CITYTO="SINGAPORE" CTRYTO="SG" CTRYTO_ISO="SG" DEPDATE="2014-03-19T00:00:00.000-0400" DEPTIME="1970-01-01T16:00:00.000-0500" ARRDATE="2014-03-21T00:00:00.000-0400" ARRTIME="1970-01-01T02:45:00.000-0500" PLANETYPE="DC-10-10"/>
    			<row HOP="2" AIRLINEID="QF" AIRLINE="Qantas Airways" CONNECTID="0005" AIRPORTFR="SIN" CITYFROM="SINGAPORE" CTRYFR="SG" CTRYFR_ISO="SG" AIRPORTTO="FRA" CITYTO="FRANKFURT" CTRYTO="DE" CTRYTO_ISO="DE" DEPDATE="2014-03-21T00:00:00.000-0400" DEPTIME="1970-01-01T22:50:00.000-0500" ARRDATE="2014-03-22T00:00:00.000-0400" ARRTIME="1970-01-01T05:35:00.000-0500" PLANETYPE="A310-300"/>
  			</FLIGHT_HOP_LIST>
  			<RETURN>
    			<row TYPE="S" ID="BC_IBF" NUMBER="000" MESSAGE="Method was executed successfully" LOG_NO="" LOG_MSG_NO="000000" MESSAGE_V1="" MESSAGE_V2="" MESSAGE_V3="" MESSAGE_V4="" PARAMETER="" FIELD="" SYSTEM="NPLCLNT001"/>
  			</RETURN>
		</BAPI_FLCONN_GETDETAIL:Response>

		2013-11-05 11:09:56,423 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   49 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Calling 'BAPI_FLCUST_GETLIST' RFC
		2013-11-05 11:09:56,423 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   50 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Request: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLCUST_GETLIST:Request xmlns:BAPI_FLCUST_GETLIST="http://sap.fusesource.org/rfc/NPL/BAPI_FLCUST_GETLIST" CUSTOMER_NAME="James Legrand"/>

		2013-11-05 11:09:56,561 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   58 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Response: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLCUST_GETLIST:Response xmlns:BAPI_FLCUST_GETLIST="http://sap.fusesource.org/rfc/NPL/BAPI_FLCUST_GETLIST">
  			<CUSTOMER_LIST>
    			<row CUSTOMERID="00001523" CUSTNAME="James Legrand" FORM="Herr" STREET="53 Golden Gate Drive" POBOX="" POSTCODE="22334" CITY="San Francisco" COUNTR="US" COUNTR_ISO="US" REGION="" PHONE="+1 240 27589 29874" EMAIL="James_Legrand@SanFrancisco.net"/>
  			</CUSTOMER_LIST>
  			<CUSTOMER_RANGE/>
  			<EXTENSION_IN/>
  			<EXTENSION_OUT/>
  			<RETURN>
    			<row TYPE="S" ID="BC_IBF" NUMBER="000" MESSAGE="Method was executed successfully" LOG_NO="" LOG_MSG_NO="000000" MESSAGE_V1="" MESSAGE_V2="" MESSAGE_V3="" MESSAGE_V4="" PARAMETER="" FIELD="" SYSTEM="NPLCLNT001"/>
  			</RETURN>
		</BAPI_FLCUST_GETLIST:Response>

		2013-11-05 11:09:56,824 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   49 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Calling 'BAPI_FLTRIP_CREATE' RFC
		2013-11-05 11:09:56,825 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   50 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Request: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLTRIP_CREATE:Request xmlns:BAPI_FLTRIP_CREATE="http://sap.fusesource.org/rfc/NPL/BAPI_FLTRIP_CREATE">
  			<FLIGHT_TRIP_DATA AGENCYNUM="00000110" CUSTOMERID="00001523" FLCONN1="0002" FLDATE1="2014-03-19T00:00:00.000-0400" CLASS="Y"/>
  			<PASSENGER_LIST>
    			<row PASSNAME="Travelin Joe" PASSFORM="Mr" PASSBIRTH="1990-03-17T00:00:00.000-0500"/>
  			</PASSENGER_LIST>
		</BAPI_FLTRIP_CREATE:Request>

		2013-11-05 11:09:57,061 | DEBUG | CoServerThread-1 | SAPProducer                      | .camel.component.sap.SAPProducer   58 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Response: <?xml version="1.0" encoding="ASCII"?>
		<BAPI_FLTRIP_CREATE:Response xmlns:BAPI_FLTRIP_CREATE="http://sap.fusesource.org/rfc/NPL/BAPI_FLTRIP_CREATE" TRAVELAGENCYNUMBER="00000110" TRIPNUMBER="00000148">
  			<TICKET_PRICE TRIPPRICE="1878.5700" TRIPTAX="93.9200" CURR="EUR" CURR_ISO="EUR"/>
  			<EXTENSION_IN/>
  			<PASSENGER_LIST>
    			<row PASSNAME="Travelin Joe" PASSFORM="Mr" PASSBIRTH="1990-03-17T00:00:00.000-0500"/>
  			</PASSENGER_LIST>
  			<RETURN>
    			<row TYPE="S" ID="BAPI" NUMBER="000" MESSAGE="FlightTrip 0000011000000148 has been created. External reference: 0000011000022014031900001523" LOG_NO="" LOG_MSG_NO="000000" MESSAGE_V1="FlightTrip" MESSAGE_V2="0000011000000148" MESSAGE_V3="" MESSAGE_V4="0000011000022014031900001523" PARAMETER="" FIELD="" SYSTEM="NPLCLNT001"/>
  			</RETURN>
		</BAPI_FLTRIP_CREATE:Response>

		2013-11-05 11:09:57,766 | DEBUG | CoServerThread-1 | SAPConsumer                      | .camel.component.sap.SAPConsumer  106 | 88 - org.fusesource.camel-sap - 7.3.0.redhat-SNAPSHOT | Response: <?xml version="1.0" encoding="ASCII"?>
		<BOOK_FLIGHT:Response xmlns:BOOK_FLIGHT="http://sap.fusesource.org/rfc/nplServer/BOOK_FLIGHT" TRIPNUMBER="00000148" TICKET_PRICE="1878.5700" TICKET_TAX="93.9200" CURRENCY="EUR" PASSFORM="Mr" PASSNAME="Travelin Joe" PASSBIRTH="1990-03-17T00:00:00.000-0500">
  			<FLTINFO FLIGHTTIME="3155" CITYFROM="SAN FRANCISCO" DEPDATE="2014-03-19T00:00:00.000-0400" DEPTIME="1970-01-01T16:00:00.000-0500" CITYTO="FRANKFURT" ARRDATE="2014-03-22T00:00:00.000-0400" ARRTIME="1970-01-01T05:35:00.000-0500"/>
  			<CONNINFO>
    			<row CONNID="1" AIRLINE="Singapore Airlines" PLANETYPE="DC-10-10" CITYFROM="SAN FRANCISCO" DEPDATE="2014-03-19T00:00:00.000-0400" DEPTIME="1970-01-01T16:00:00.000-0500" CITYTO="SINGAPORE" ARRDATE="2014-03-21T00:00:00.000-0400" ARRTIME="1970-01-01T02:45:00.000-0500"/>
    			<row CONNID="2" AIRLINE="Qantas Airways" PLANETYPE="A310-300" CITYFROM="SINGAPORE" DEPDATE="2014-03-21T00:00:00.000-0400" DEPTIME="1970-01-01T22:50:00.000-0500" CITYTO="FRANKFURT" ARRDATE="2014-03-22T00:00:00.000-0400" ARRTIME="1970-01-01T05:35:00.000-0500"/>
  			</CONNINFO>
		</BOOK_FLIGHT:Response>

1. To escape the log view, type Ctrl-C.
	
1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Delete the child container by entering the following command at the console:

		fabric:container-delete mychild