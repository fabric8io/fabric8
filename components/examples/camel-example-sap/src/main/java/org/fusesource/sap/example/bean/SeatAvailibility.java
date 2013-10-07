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

/**
 * Bean containing Seat Availability information.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class SeatAvailibility {
	
	private String hopNumber;
	
	private String economyClassSeatCapacity;
	
	private String economyClassFreeSeats;

	private String businessClassSeatCapacity;
	
	private String businessClassFreeSeats;

	private String firstClassClassSeatCapacity;
	
	private String firstClassFreeSeats;

	public String getHopNumber() {
		return hopNumber;
	}

	public void setHopNumber(String hopNumber) {
		this.hopNumber = hopNumber;
	}

	public String getEconomyClassSeatCapacity() {
		return economyClassSeatCapacity;
	}

	public void setEconomyClassSeatCapacity(String economyClassSeatCapacity) {
		this.economyClassSeatCapacity = economyClassSeatCapacity;
	}

	public String getEconomyClassFreeSeats() {
		return economyClassFreeSeats;
	}

	public void setEconomyClassFreeSeats(String economyClassFreeSeats) {
		this.economyClassFreeSeats = economyClassFreeSeats;
	}

	public String getBusinessClassSeatCapacity() {
		return businessClassSeatCapacity;
	}

	public void setBusinessClassSeatCapacity(String businessClassSeatCapacity) {
		this.businessClassSeatCapacity = businessClassSeatCapacity;
	}

	public String getBusinessClassFreeSeats() {
		return businessClassFreeSeats;
	}

	public void setBusinessClassFreeSeats(String businessClassFreeSeats) {
		this.businessClassFreeSeats = businessClassFreeSeats;
	}

	public String getFirstClassClassSeatCapacity() {
		return firstClassClassSeatCapacity;
	}

	public void setFirstClassClassSeatCapacity(String firstClassClassSeatCapacity) {
		this.firstClassClassSeatCapacity = firstClassClassSeatCapacity;
	}

	public String getFirstClassFreeSeats() {
		return firstClassFreeSeats;
	}

	public void setFirstClassFreeSeats(String firstClassFreeSeats) {
		this.firstClassFreeSeats = firstClassFreeSeats;
	}

	@Override
	public String toString() {
		return "Availability [hopNumber=" + hopNumber + ", economyClassSeatCapacity=" + economyClassSeatCapacity + ", economyClassFreeSeats="
				+ economyClassFreeSeats + ", businessClassSeatCapacity=" + businessClassSeatCapacity + ", businessClassFreeSeats=" + businessClassFreeSeats
				+ ", firstClassClassSeatCapacity=" + firstClassClassSeatCapacity + ", firstClassFreeSeats=" + firstClassFreeSeats + "]";
	}


}
