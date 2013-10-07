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
 * JAXB Bean containing Flight Information data from a SAP BOOK_FLIGHT response.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
@XmlRootElement(name="FLTINFO", namespace="http://sap.fusesource.org/rfc/nplServer/BOOK_FLIGHT")
@XmlAccessorType(XmlAccessType.FIELD)
public class FlightInfo {

	@XmlAttribute(name="FLIGHTTIME")
	private String flightTime;

	@XmlAttribute(name="CITYFROM")
	private String cityFrom;

	@XmlAttribute(name="DEPDATE")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date departureDate;

	@XmlAttribute(name="DEPTIME")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date departureTime;

	@XmlAttribute(name="CITYTO")
	private String cityTo;

	@XmlAttribute(name="ARRDATE")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date arrivalDate;

	@XmlAttribute(name="ARRTIME")
	@XmlJavaTypeAdapter(DateAdapter.class)
	private Date arrivalTime;
	
	public String getFlightTime() {
		return flightTime;
	}

	public void setFlightTime(String flightTime) {
		this.flightTime = flightTime;
	}

	public String getCityFrom() {
		return cityFrom;
	}

	public void setCityFrom(String cityFrom) {
		this.cityFrom = cityFrom;
	}

	public Date getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(Date departureDate) {
		this.departureDate = departureDate;
	}

	public Date getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(Date departureTime) {
		this.departureTime = departureTime;
	}

	public String getCityTo() {
		return cityTo;
	}

	public void setCityTo(String cityTo) {
		this.cityTo = cityTo;
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

	@Override
	public String toString() {
		return "FlightInfo [flightTime=" + flightTime + ", cityFrom=" + cityFrom + ", departureDate=" + departureDate + ", departureTime=" + departureTime
				+ ", cityTo=" + cityTo + ", arrivalDate=" + arrivalDate + ", arrivalTime=" + arrivalTime + "]";
	}

}
