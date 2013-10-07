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
package org.fusesource.sap.example.bean;

import java.util.Date;

/**
 * Bean containing Flight Hop information.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class FlightHop {
	
	private String hopNumber;
	
	private String airlineCode;
	
	private String airlineName;
	
	private String flightConnectionNumber;
	
	private Date depatureDate;
	
	private Date depatureTime;
	
	private String depatureAirport;
	
	private String depatureCity;
	
	private String departureCountry;
	
	private String departureCountryIso;

	private Date arrivalDate;

	private Date arrivalTime;
	
	private String arrivalAirport;
	
	private String arrivalCity;
	
	private String arrivalCountry;
	
	private String arrivalCountryIso;
	
	private String aircraftType;

	public String getHopNumber() {
		return hopNumber;
	}

	public void setHopNumber(String hopNumber) {
		this.hopNumber = hopNumber;
	}

	public String getAirlineCode() {
		return airlineCode;
	}

	public void setAirlineCode(String airlineCode) {
		this.airlineCode = airlineCode;
	}

	public String getAirlineName() {
		return airlineName;
	}

	public void setAirlineName(String airlineName) {
		this.airlineName = airlineName;
	}

	public String getFlightConnectionNumber() {
		return flightConnectionNumber;
	}

	public void setFlightConnectionNumber(String flightConnectionNumber) {
		this.flightConnectionNumber = flightConnectionNumber;
	}

	public Date getDepatureDate() {
		return depatureDate;
	}

	public void setDepatureDate(Date depatureDate) {
		this.depatureDate = depatureDate;
	}

	public Date getDepatureTime() {
		return depatureTime;
	}

	public void setDepatureTime(Date depatureTime) {
		this.depatureTime = depatureTime;
	}

	public String getDepatureAirport() {
		return depatureAirport;
	}

	public void setDepatureAirport(String depatureAirport) {
		this.depatureAirport = depatureAirport;
	}

	public String getDepatureCity() {
		return depatureCity;
	}

	public void setDepatureCity(String depatureCity) {
		this.depatureCity = depatureCity;
	}

	public String getDepartureCountry() {
		return departureCountry;
	}

	public void setDepartureCountry(String departureCountry) {
		this.departureCountry = departureCountry;
	}

	public String getDepartureCountryIso() {
		return departureCountryIso;
	}

	public void setDepartureCountryIso(String departureCountryIso) {
		this.departureCountryIso = departureCountryIso;
	}

	public Date getArrivalDate() {
		return arrivalDate;
	}

	public void setArrivalDate(Date arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

	public Date getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Date arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public String getArrivalAirport() {
		return arrivalAirport;
	}

	public void setArrivalAirport(String arrivalAirport) {
		this.arrivalAirport = arrivalAirport;
	}

	public String getArrivalCity() {
		return arrivalCity;
	}

	public void setArrivalCity(String arrivalCity) {
		this.arrivalCity = arrivalCity;
	}

	public String getArrivalCountry() {
		return arrivalCountry;
	}

	public void setArrivalCountry(String arrivalCountry) {
		this.arrivalCountry = arrivalCountry;
	}

	public String getArrivalCountryIso() {
		return arrivalCountryIso;
	}

	public void setArrivalCountryIso(String arrivalCountryIso) {
		this.arrivalCountryIso = arrivalCountryIso;
	}

	public String getAircraftType() {
		return aircraftType;
	}

	public void setAircraftType(String aircraftType) {
		this.aircraftType = aircraftType;
	}

	@Override
	public String toString() {
		return "FlightHop [number=" + hopNumber + ", airlineCode=" + airlineCode + ", airlineName=" + airlineName + ", flightConnectionNumber="
				+ flightConnectionNumber + ", depatureDate=" + depatureDate + ", depatureTime=" + depatureTime + ", depatureAirport=" + depatureAirport
				+ ", depatureCity=" + depatureCity + ", departureCountry=" + departureCountry + ", departureContryIso=" + departureCountryIso + ", arrivalDate="
				+ arrivalDate + ", arrivalTime=" + arrivalTime + ", arrivalAirport=" + arrivalAirport + ", arrivalCity=" + arrivalCity + ", arrivalCountry="
				+ arrivalCountry + ", arrivalContryIso=" + arrivalCountryIso + ", aircraftType=" + aircraftType + "]";
	}
	
	
}
