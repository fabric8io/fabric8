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
package org.fusesource.sap.example.jaxb;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * JAXB Bean containing SAP BOOK_FLIGHT request data.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
@XmlRootElement(name="Request", namespace="http://sap.fusesource.org/rfc/nplServer/BOOK_FLIGHT")
@XmlAccessorType(XmlAccessType.FIELD)
public class BookFlightRequest {
	
	@XmlAttribute(name="CUSTNAME")
	private String customerName;
	
	@XmlAttribute(name="FLIGHTDATE")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date flightDate;
	
	@XmlAttribute(name="TRAVELAGENCYNUMBER")
	private String travelAgencyNumber;
	
	@XmlAttribute(name="DESTINATION_FROM")
	private String startAirportCode;
	
	@XmlAttribute(name="DESTINATION_TO")
	private String endAirportCode;
	
	@XmlAttribute(name="PASSFORM")
	private String passengerFormOfAddress;
	
	@XmlAttribute(name="PASSNAME")
	private String passengerName;
	
	@XmlAttribute(name="PASSBIRTH")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date passengerDateOfBirth;
	
	@XmlAttribute(name="CLASS")
	private String flightClass;
	
	public Date getFlightDate() {
		return flightDate;
	}
	public void setFlightDate(Date flightDate) {
		this.flightDate = flightDate;
	}
	public String getTravelAgencyNumber() {
		return travelAgencyNumber;
	}
	public void setTravelAgencyNumber(String travelAgencyNumber) {
		this.travelAgencyNumber = travelAgencyNumber;
	}
	
	public String getStartAirportCode() {
		return startAirportCode;
	}
	public void setStartAirportCode(String startAirportCode) {
		this.startAirportCode = startAirportCode;
	}
	public String getEndAirportCode() {
		return endAirportCode;
	}
	public void setEndAirportCode(String endAirportCode) {
		this.endAirportCode = endAirportCode;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public String getPassengerFormOfAddress() {
		return passengerFormOfAddress;
	}
	public void setPassengerFormOfAddress(String passengerFormOfAddress) {
		this.passengerFormOfAddress = passengerFormOfAddress;
	}
	public String getPassengerName() {
		return passengerName;
	}
	public void setPassengerName(String passengerName) {
		this.passengerName = passengerName;
	}
	public Date getPassengerDateOfBirth() {
		return passengerDateOfBirth;
	}
	public void setPassengerDateOfBirth(Date passengerDateOfBirth) {
		this.passengerDateOfBirth = passengerDateOfBirth;
	}
	public String getFlightClass() {
		return flightClass;
	}
	public void setFlightClass(String flightClass) {
		this.flightClass = flightClass;
	}
	@Override
	public String toString() {
		return "BookFlightRequest [customerName=" + customerName + ", flightDate=" + flightDate + ", travelAgencyNumber=" + travelAgencyNumber
				+ ", startAirportCode=" + startAirportCode + ", endAirportCode=" + endAirportCode + ", passengerFormOfAddress=" + passengerFormOfAddress
				+ ", passengerName=" + passengerName + ", passengerDateOfBirth=" + passengerDateOfBirth + ", flightClass=" + flightClass + "]";
	}

}
