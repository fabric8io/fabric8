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
import java.util.List;


/**
 * Bean containing Flight Connection information
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class FlightConnectionInfo {
	
	private String travelAgencyNumber;
	
	private String flightConnectionNumber;
	
	private String numberOfHops;
	
	private String departureAirport;
	
	private String departureCity;
	
	private Date departureTime;
	
	private Date departureDate;
	
	private String arrivalAirport;
	
	private String arrivalCity;
	
	private Date arrivalTime;
	
	private Date arrivalDate;
	
	private String flightTime;
	
	private PriceInfo priceInfo;
	
	private List<FlightHop> flightHopList;
	
	private List<SeatAvailibility> seatAvailibilityList;

	public String getTravelAgencyNumber() {
		return travelAgencyNumber;
	}

	public void setTravelAgencyNumber(String travelAgencyNumber) {
		this.travelAgencyNumber = travelAgencyNumber;
	}

	public String getFlightConnectionNumber() {
		return flightConnectionNumber;
	}

	public void setFlightConnectionNumber(String flightConnectionNumber) {
		this.flightConnectionNumber = flightConnectionNumber;
	}

	public String getDepartureAirport() {
		return departureAirport;
	}

	public void setDepartureAirport(String departureAirport) {
		this.departureAirport = departureAirport;
	}

	public String getDepartureCity() {
		return departureCity;
	}

	public void setDepartureCity(String departureCity) {
		this.departureCity = departureCity;
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

	public String getNumberOfHops() {
		return numberOfHops;
	}

	public void setNumberOfHops(String numberOfHops) {
		this.numberOfHops = numberOfHops;
	}

	public Date getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(Date departureTime) {
		this.departureTime = departureTime;
	}

	public Date getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(Date departureDate) {
		this.departureDate = departureDate;
	}

	public Date getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Date arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public Date getArrivalDate() {
		return arrivalDate;
	}

	public void setArrivalDate(Date arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

	public String getFlightTime() {
		return flightTime;
	}

	public void setFlightTime(String flightTime) {
		this.flightTime = flightTime;
	}

	public PriceInfo getPriceInfo() {
		return priceInfo;
	}

	public void setPriceInfo(PriceInfo priceInfo) {
		this.priceInfo = priceInfo;
	}

	public List<FlightHop> getFlightHopList() {
		return flightHopList;
	}

	public void setFlightHopList(List<FlightHop> flightHopList) {
		this.flightHopList = flightHopList;
	}

	public List<SeatAvailibility> getSeatAvailibilityList() {
		return seatAvailibilityList;
	}

	public void setSeatAvailibilityList(List<SeatAvailibility> seatAvailibilityList) {
		this.seatAvailibilityList = seatAvailibilityList;
	}

	@Override
	public String toString() {
		return "FlightConnectionInfo [travelAgencyNumber=" + travelAgencyNumber + ", flightConnectionNumber=" + flightConnectionNumber + ", numberOfHops="
				+ numberOfHops + ", departureAirport=" + departureAirport + ", departureCity=" + departureCity + ", departureTime=" + departureTime
				+ ", departureDate=" + departureDate + ", arrivalAirport=" + arrivalAirport + ", arrivalCity=" + arrivalCity + ", arrivalTime=" + arrivalTime
				+ ", arrivalDate=" + arrivalDate + ", flightTime=" + flightTime + ", priceInfo=" + priceInfo + ", flightHopList=" + flightHopList
				+ ", availabilityList=" + seatAvailibilityList + "]";
	}

}
