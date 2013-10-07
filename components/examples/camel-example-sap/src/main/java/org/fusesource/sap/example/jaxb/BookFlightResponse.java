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

import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * JAXB Bean containing SAP BOOK_FLIGHT response data.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
@XmlRootElement(name="Response", namespace="http://sap.fusesource.org/rfc/nplServer/BOOK_FLIGHT")
@XmlAccessorType(XmlAccessType.FIELD)
public class BookFlightResponse {
	
	@XmlAttribute(name="TRIPNUMBER")
	private String tripNumber;

	@XmlAttribute(name="TICKET_PRICE")
	private BigDecimal ticketPrice;
	
	@XmlAttribute(name="TICKET_TAX")
	private BigDecimal ticketTax;
	
	@XmlAttribute(name="CURRENCY")
	private String currency;

	@XmlAttribute(name="PASSFORM")
	private String passengerFormOfAddress;
	
	@XmlAttribute(name="PASSNAME")
	private String passengerName;
	
	@XmlAttribute(name="PASSBIRTH")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date passengerDateOfBirth;
	
	@XmlElement(name="FLTINFO")
	private FlightInfo flightInfo;

	@XmlElement(name="CONNINFO")
	private ConnectionInfoTable connectionInfo;

	public String getTripNumber() {
		return tripNumber;
	}
	public void setTripNumber(String tripNumber) {
		this.tripNumber = tripNumber;
	}
	public BigDecimal getTicketPrice() {
		return ticketPrice;
	}
	public void setTicketPrice(BigDecimal ticketPrice) {
		this.ticketPrice = ticketPrice;
	}
	public BigDecimal getTicketTax() {
		return ticketTax;
	}
	public void setTicketTax(BigDecimal ticketTax) {
		this.ticketTax = ticketTax;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
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
	public FlightInfo getFlightInfo() {
		return flightInfo;
	}
	public void setFlightInfo(FlightInfo flightInfo) {
		this.flightInfo = flightInfo;
	}
	public void setConnectionInfo(ConnectionInfoTable connectionInfo) {
		this.connectionInfo = connectionInfo;
	}
	public ConnectionInfoTable getConnectionInfo() {
		return connectionInfo;
	}
	@Override
	public String toString() {
		return "BookFlightResponse [ticketPrice=" + ticketPrice + ", ticketTax=" + ticketTax + ", currency=" + currency + ", passengerFormOfAddress="
				+ passengerFormOfAddress + ", passengerName=" + passengerName + ", passengerDateOfBirth=" + passengerDateOfBirth + ", flightInfo=" + flightInfo
				+ ", connectionInfo=" + connectionInfo + "]";
	}
}
