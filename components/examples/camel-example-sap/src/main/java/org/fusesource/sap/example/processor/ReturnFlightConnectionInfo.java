/**
 * Copyright 2013 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.sap.example.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.sap.example.bean.FlightConnectionInfo;
import org.fusesource.sap.example.bean.FlightHop;
import org.fusesource.sap.example.bean.PriceInfo;
import org.fusesource.sap.example.bean.SeatAvailibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor that builds Flight Connection Info object.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ReturnFlightConnectionInfo {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnFlightConnectionInfo.class);

	/**
	 * Builds Flight Connection Info bean from BAPI_FLCONN_GETDETAIL response in
	 * exchange message body and adds to exchange message's header.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void createFlightConnectionInfo(Exchange exchange) throws Exception {
		
		Structure flightConnectionGetDetailResponse = exchange.getIn().getBody(Structure.class);

		if (flightConnectionGetDetailResponse == null) {
			throw new Exception("No Flight Connection Get Detail Response");
		}
		
		// Create flight connection info bean.
		FlightConnectionInfo flightConnectionInfo = new FlightConnectionInfo();

		//
		// Add connection data to flight connection info bean.
		// 
		
		Structure connectionData = flightConnectionGetDetailResponse.get("CONNECTION_DATA", Structure.class);
		if (connectionData != null) {

			// Add travel agency number to flight connection info if set.
			String travelAgencyNumber = connectionData.get("AGENCYNUM", String.class);
			if (travelAgencyNumber != null) {
				flightConnectionInfo.setTravelAgencyNumber(travelAgencyNumber);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set travel agency number = '{}' in flight connection info", travelAgencyNumber);
				}
			}

			// Add flight connection number to flight connection info if set.
			String flightConnectionNumber = connectionData.get("FLIGHTCONN", String.class);
			if (flightConnectionNumber != null) {
				flightConnectionInfo.setFlightConnectionNumber(flightConnectionNumber);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set flight connection number = '{}' in flight connection info", flightConnectionNumber);
				}
			}

			// Add departure date to flight connection info if set.
			Date departureDate = connectionData.get("FLIGHTDATE", Date.class);
			if (departureDate != null) {
				flightConnectionInfo.setDepartureDate(departureDate);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set depature date = '{}' in flight connection info", departureDate);
				}
			}

			// Add departure airport to flight connection info if set.
			String departureAirport = connectionData.get("AIRPORTFR", String.class);
			if (departureAirport != null) {
				flightConnectionInfo.setDepartureAirport(departureAirport);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure airport = '{}' in flight connection info", departureAirport);
				}
			}

			// Add departure city to flight connection info if set.
			String departureCity = connectionData.get("CITYFROM", String.class);
			if (departureCity != null) {
				flightConnectionInfo.setDepartureCity(departureCity);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure city = '{}' in flight connection info", departureCity);
				}
			}

			// Add arrival airport to flight connection info if set.
			String arrivalAirport = connectionData.get("AIRPORTTO", String.class);
			if (arrivalAirport != null) {
				flightConnectionInfo.setArrivalAirport(arrivalAirport);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival airport = '{}' in flight connection info", arrivalAirport);
				}
			}

			// Add arrival city to flight connection info if set.
			String arrivalCity = connectionData.get("CITYTO", String.class);
			if (arrivalCity != null) {
				flightConnectionInfo.setArrivalCity(arrivalCity);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival city = '{}' in flight connection info", arrivalCity);
				}
			}

			// Add number of hops to flight connection info if set.
			String numberOfHops = connectionData.get("NUMHOPS", String.class);
			if (numberOfHops != null) {
				flightConnectionInfo.setNumberOfHops(numberOfHops);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set number of hops = '{}' in flight connection info", numberOfHops);
				}
			}

			// Add departure time to flight connection info if set.
			Date departureTime = connectionData.get("DEPTIME", Date.class);
			if (departureTime != null) {
				flightConnectionInfo.setDepartureTime(departureTime);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure time = '{}' in flight connection info", departureTime);
				}
			}

			// Add arrival time to flight connection info if set.
			Date arrivalTime = connectionData.get("ARRTIME", Date.class);
			if (arrivalTime != null) {
				flightConnectionInfo.setArrivalTime(arrivalTime);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival time = '{}' in flight connection info", arrivalTime);
				}
			}

			// Add arrival date to flight connection info if set.
			Date arrivalDate = connectionData.get("ARRDATE", Date.class);
			if (arrivalDate != null) {
				flightConnectionInfo.setArrivalDate(arrivalDate);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival date = '{}' in flight connection info", arrivalDate);
				}
			}

			// Add flight time to flight connection info if set.
			Integer flightTime = connectionData.get("FLIGHTTIME", Integer.class);
			if (flightTime != null) {
				flightConnectionInfo.setFlightTime(flightTime.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set flight time = '{}' in flight connection info", flightTime);
				}
			}

		}

		//
		// Add flight hop list to flight connection info bean.
		// 
		
		@SuppressWarnings("unchecked")
		Table<Structure> hopList = flightConnectionGetDetailResponse.get("FLIGHT_HOP_LIST", Table.class);
		List<FlightHop> flightHopList = new ArrayList<FlightHop>();

		for (Structure hop : hopList) {
			
			// Create flight hop object.
			FlightHop flightHop = new FlightHop();

			// Add hop number to flight hop if set.
			String hopNumber = hop.get("HOP", String.class);
			if (hopNumber != null) {
				flightHop.setHopNumber(hopNumber);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set hop number = '{}' in flight hop", hopNumber);
				}
			}

			// Add airline code to flight hop if set.
			String airlineCode = hop.get("AIRLINEID", String.class);
			if (airlineCode != null) {
				flightHop.setAirlineCode(airlineCode);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set airline code = '{}' in flight hop", airlineCode);
				}
			}

			// Add airline name to flight hop if set.
			String airlineName = hop.get("AIRLINE", String.class);
			if (airlineName != null) {
				flightHop.setAirlineName(airlineName);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set airline name = '{}' in flight hop", airlineName);
				}
			}

			// Add flight connection number to flight hop if set.
			String flightConnectionNumber = hop.get("CONNECTID", String.class);
			if (flightConnectionNumber != null) {
				flightHop.setFlightConnectionNumber(flightConnectionNumber);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set flight connection number = '{}' in flight hop", flightConnectionNumber);
				}
			}

			// Add departure airport to flight hop if set.
			String depatureAirport = hop.get("AIRPORTFR", String.class);
			if (depatureAirport != null) {
				flightHop.setDepatureAirport(depatureAirport);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure airport = '{}' in flight hop", depatureAirport);
				}
			}

			// Add departure city to flight hop if set.
			String depatureCity = hop.get("CITYFROM", String.class);
			if (depatureCity != null) {
				flightHop.setDepatureCity(depatureCity);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure city = '{}' in flight hop", depatureCity);
				}
			}

			// Add departure country to flight hop if set.
			String departureCountry = hop.get("CTRYFR", String.class);
			if (departureCountry != null) {
				flightHop.setDepartureCountry(departureCountry);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure country = '{}' in flight hop", departureCountry);
				}
			}

			// Add departure country code to flight hop if set.
			String departureCountryIso = hop.get("CTRYFR_ISO", String.class);
			if (departureCountryIso != null) {
				flightHop.setDepartureCountryIso(departureCountryIso);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure iso country code = '{}' in flight hop", departureCountryIso);
				}
			}

			// Add arrival airport to flight hop if set.
			String arrivalAirport = hop.get("AIRPORTTO", String.class);
			if (arrivalAirport != null) {
				flightHop.setArrivalAirport(arrivalAirport);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival airport = '{}' in flight hop", arrivalAirport);
				}
			}

			// Add arrival city to flight hop if set.
			String arrivalCity = hop.get("CITYTO", String.class);
			if (arrivalCity != null) {
				flightHop.setArrivalCity(arrivalCity);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival city = '{}' in flight hop", arrivalCity);
				}
			}

			// Add arrival country to flight hop if set.
			String arrivalCountry = hop.get("CTRYTO", String.class);
			if (arrivalCountry != null) {
				flightHop.setArrivalCountry(arrivalCountry);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival country = '{}' in flight hop", arrivalCountry);
				}
			}

			// Add arrival country code to flight hop if set.
			String arrivalCountryIso = hop.get("CTRYTO_ISO", String.class);
			if (arrivalCountryIso != null) {
				flightHop.setArrivalCountryIso(arrivalCountryIso);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival iso country code = '{}' in flight hop", arrivalCountryIso);
				}
			}

			// Add departure date to flight hop if set.
			Date departureDate = hop.get("DEPDATE", Date.class);
			if (departureDate != null) {
				flightHop.setDepatureDate(departureDate);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure date = '{}' in flight hop", departureDate);
				}
			}

			// Add departure time to flight hop if set.
			Date departureTime = hop.get("DEPTIME", Date.class);
			if (departureTime != null) {
				flightHop.setDepatureTime(departureTime);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set departure time = '{}' in flight hop", departureTime);
				}
			}

			// Add arrival date to flight hop if set.
			Date arrivalDate = hop.get("ARRDATE", Date.class);
			if (arrivalDate != null) {
				flightHop.setArrivalDate(arrivalDate);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival date = '{}' in flight hop", arrivalDate);
				}
			}

			// Add arrival time to flight hop if set.
			Date arrivalTime = hop.get("ARRTIME", Date.class);
			if (arrivalTime != null) {
				flightHop.setArrivalTime(arrivalTime);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set arrival time = '{}' in flight hop", arrivalTime);
				}
			}

			// Add aircraft type to flight hop if set.
			String aircraftType = hop.get("PLANETYPE", String.class);
			if (aircraftType != null) {
				flightHop.setAircraftType(aircraftType);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set aircraft type = '{}' in flight hop", aircraftType);
				}
			}

			// Add flight hop
			flightHopList.add(flightHop);
		}

		// Add flight hop list to flight connection info.
		flightConnectionInfo.setFlightHopList(flightHopList);

		//
		// Add availability list to flight connection info bean.
		// 
		
		@SuppressWarnings("unchecked")
		Table<Structure> availibilityList = flightConnectionGetDetailResponse.get("AVAILIBILITY", Table.class);
		List<SeatAvailibility> seatAvailiblityList = new ArrayList<SeatAvailibility>();

		for (Structure availibility : availibilityList) {

			// Create seat availability object.
			SeatAvailibility seatAvailiblity = new SeatAvailibility();

			// Add hop number to availability if set.
			String hopNumber = availibility.get("HOP", String.class);
			if (hopNumber != null) {
				seatAvailiblity.setHopNumber(hopNumber);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set hop number = '{}' in seat availibility", hopNumber);
				}
			}

			// Add economy class seat capacity to availability if set.
			Integer economyClassSeatCapacity =  availibility.get("ECONOMAX", Integer.class);
			if (economyClassSeatCapacity != null) {
				seatAvailiblity.setEconomyClassSeatCapacity(economyClassSeatCapacity.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set economy class seat capacity = '{}' in seat availibility", economyClassSeatCapacity);
				}
			}

			// Add economy class free seats to availability if set.
			Integer economyClassFreeSeats =  availibility.get("ECONOFREE", Integer.class);
			if (economyClassFreeSeats != null) {
				seatAvailiblity.setEconomyClassFreeSeats(economyClassFreeSeats.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set economy class free seats = '{}' in seat availibility", economyClassFreeSeats);
				}
			}

			// Add business class seat capacity to availability if set.
			Integer businessClassSeatCapacity =  availibility.get("BUSINMAX", Integer.class);
			if (businessClassSeatCapacity != null) {
				seatAvailiblity.setBusinessClassSeatCapacity(businessClassSeatCapacity.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set business class seat capacity = '{}' in seat availibility", businessClassSeatCapacity);
				}
			}

			// Add business class free seats to availability if set.
			Integer businessClassFreeSeats =  availibility.get("BUSINFREE", Integer.class);
			if (businessClassFreeSeats != null) {
				seatAvailiblity.setBusinessClassFreeSeats(businessClassFreeSeats.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set business class free seats = '{}' in seat availibility", businessClassFreeSeats);
				}
			}

			// Add first class seat capacity to availability if set.
			Integer firstClassClassSeatCapacity =  availibility.get("FIRSTMAX", Integer.class);
			if (firstClassClassSeatCapacity != null) {
				seatAvailiblity.setFirstClassClassSeatCapacity(firstClassClassSeatCapacity.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set first class seat capacity = '{}' in seat availibility", firstClassClassSeatCapacity);
				}
			}

			// Add first class free seats to availability if set.
			Integer firstClassFreeSeats =  availibility.get("FIRSTFREE", Integer.class);
			if (firstClassFreeSeats != null) {
				seatAvailiblity.setFirstClassFreeSeats(firstClassFreeSeats.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set first class free seats = '{}' in seat availibility", firstClassFreeSeats);
				}
			}

			// Add availability to list.
			seatAvailiblityList.add(seatAvailiblity);
		}

		// Add availability list to flight connection info.
		flightConnectionInfo.setSeatAvailibilityList(seatAvailiblityList);

		//
		// Add price info to flight connection info bean.
		// 
		
		Structure prices = (Structure) flightConnectionGetDetailResponse.get("PRICE_INFO", Structure.class);

		if (prices != null) {
			
			// Create price info object.
			PriceInfo priceInfo = new PriceInfo();

			// Add economy class airfare to availability if set.
			BigDecimal economyClassAirfare =  prices.get("PRICE_ECO1", BigDecimal.class);
			if (economyClassAirfare != null) {
				priceInfo.setEconomyClassAirfare(economyClassAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set economy class airfare = '{}' in seat availibility", economyClassAirfare);
				}
			}

			// Add economy class child airfare to availability if set.
			BigDecimal economyClassChildAirfare =  prices.get("PRICE_ECO2", BigDecimal.class);
			if (economyClassChildAirfare != null) {
				priceInfo.setEconomyClassChildAirfare(economyClassChildAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set economy class child airfare = '{}' in seat availibility", economyClassChildAirfare);
				}
			}

			// Add economy class small child airfare to availability if set.
			BigDecimal economyClassSmallChildAirfare =  prices.get("PRICE_ECO3", BigDecimal.class);
			if (economyClassSmallChildAirfare != null) {
				priceInfo.setEconomyClassSmallChildAirfare(economyClassSmallChildAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set economy class small child airfare = '{}' in seat availibility", economyClassSmallChildAirfare);
				}
			}

			// Add business class airfare to availability if set.
			BigDecimal businessClassAirfare =  prices.get("PRICE_BUS1", BigDecimal.class);
			if (businessClassAirfare != null) {
				priceInfo.setBusinessClassAirfare(businessClassAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set business class airfare = '{}' in seat availibility", businessClassAirfare);
				}
			}

			// Add business class child airfare to availability if set.
			BigDecimal businessClassChildAirfare =  prices.get("PRICE_BUS2", BigDecimal.class);
			if (businessClassChildAirfare != null) {
				priceInfo.setBusinessClassChildAirfare(businessClassChildAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set business class child airfare = '{}' in seat availibility", businessClassChildAirfare);
				}
			}

			// Add business class small child airfare to availability if set.
			BigDecimal businessClassSmallChildAirfare =  prices.get("PRICE_BUS3", BigDecimal.class);
			if (businessClassSmallChildAirfare != null) {
				priceInfo.setBusinessClassSmallChildAirfare(businessClassSmallChildAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set business class small child airfare = '{}' in seat availibility", businessClassSmallChildAirfare);
				}
			}

			// Add first class airfare to availability if set.
			BigDecimal firstClassAirfare =  prices.get("PRICE_FST1", BigDecimal.class);
			if (firstClassAirfare != null) {
				priceInfo.setBusinessClassAirfare(firstClassAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set first class airfare = '{}' in seat availibility", firstClassAirfare);
				}
			}

			// Add first class child airfare to availability if set.
			BigDecimal firstClassChildAirfare =  prices.get("PRICE_FST2", BigDecimal.class);
			if (firstClassChildAirfare != null) {
				priceInfo.setBusinessClassChildAirfare(firstClassChildAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set first class child airfare = '{}' in seat availibility", firstClassChildAirfare);
				}
			}

			// Add first class child airfare to availability if set.
			BigDecimal firstClassSmallChildAirfare =  prices.get("PRICE_FST3", BigDecimal.class);
			if (firstClassSmallChildAirfare != null) {
				priceInfo.setBusinessClassSmallChildAirfare(firstClassSmallChildAirfare.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set first class small child airfare = '{}' in seat availibility", firstClassSmallChildAirfare);
				}
			}

			// Add first class small child airfare to availability if set.
			BigDecimal flightTaxes =  prices.get("TAX", BigDecimal.class);
			if (flightTaxes != null) {
				priceInfo.setFlightTaxes(flightTaxes.toString());
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set flight taxes = '{}' in seat availibility", flightTaxes);
				}
			}

			// Add currency type to availability if set.
			String currency = prices.get("CURR", String.class);
			if (currency != null) {
				priceInfo.setCurrency(currency);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set local currency = '{}' in seat availibility", currency);
				}
			}

			// Add currency code to availability if set.
			String currencyIso = prices.get("CURR_ISO", String.class);
			if (currencyIso != null) {
				priceInfo.setCurrencyIso(currencyIso);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Set local currency iso code = '{}' in seat availibility", currencyIso);
				}
			}

			// Add price info.
			flightConnectionInfo.setPriceInfo(priceInfo);
		}
		
		// Put flight connection info object into header of exchange message.
		exchange.getIn().setHeader("flightConnectionInfo", flightConnectionInfo);
	}

}
