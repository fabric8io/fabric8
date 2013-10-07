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
 * Bean containing Airfare Price Information.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class PriceInfo {
	
	private String economyClassAirfare;

	private String economyClassChildAirfare;

	private String economyClassSmallChildAirfare;

	private String businessClassAirfare;

	private String businessClassChildAirfare;

	private String businessClassSmallChildAirfare;

	private String firstClassAirfare;

	private String firstClassChildAirfare;

	private String firstClassSmallChildAirfare;
	
	private String flightTaxes;
	
	private String currency;
	
	private String currencyIso;

	public String getEconomyClassAirfare() {
		return economyClassAirfare;
	}

	public void setEconomyClassAirfare(String economyClassAirfare) {
		this.economyClassAirfare = economyClassAirfare;
	}

	public String getEconomyClassChildAirfare() {
		return economyClassChildAirfare;
	}

	public void setEconomyClassChildAirfare(String economyClassChildAirfare) {
		this.economyClassChildAirfare = economyClassChildAirfare;
	}

	public String getEconomyClassSmallChildAirfare() {
		return economyClassSmallChildAirfare;
	}

	public void setEconomyClassSmallChildAirfare(String economyClassSmallChildAirfare) {
		this.economyClassSmallChildAirfare = economyClassSmallChildAirfare;
	}

	public String getBusinessClassAirfare() {
		return businessClassAirfare;
	}

	public void setBusinessClassAirfare(String businessClassAirfare) {
		this.businessClassAirfare = businessClassAirfare;
	}

	public String getBusinessClassChildAirfare() {
		return businessClassChildAirfare;
	}

	public void setBusinessClassChildAirfare(String businessClassChildAirfare) {
		this.businessClassChildAirfare = businessClassChildAirfare;
	}

	public String getBusinessClassSmallChildAirfare() {
		return businessClassSmallChildAirfare;
	}

	public void setBusinessClassSmallChildAirfare(String businessClassSmallChildAirfare) {
		this.businessClassSmallChildAirfare = businessClassSmallChildAirfare;
	}

	public String getFirstClassAirfare() {
		return firstClassAirfare;
	}

	public void setFirstClassAirfare(String firstClassAirfare) {
		this.firstClassAirfare = firstClassAirfare;
	}

	public String getFirstClassChildAirfare() {
		return firstClassChildAirfare;
	}

	public void setFirstClassChildAirfare(String firstClassChildAirfare) {
		this.firstClassChildAirfare = firstClassChildAirfare;
	}

	public String getFirstClassSmallChildAirfare() {
		return firstClassSmallChildAirfare;
	}

	public void setFirstClassSmallChildAirfare(String firstClassSmallChildAirfare) {
		this.firstClassSmallChildAirfare = firstClassSmallChildAirfare;
	}

	public String getFlightTaxes() {
		return flightTaxes;
	}

	public void setFlightTaxes(String flightTaxes) {
		this.flightTaxes = flightTaxes;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getCurrencyIso() {
		return currencyIso;
	}

	public void setCurrencyIso(String currencyIso) {
		this.currencyIso = currencyIso;
	}

	@Override
	public String toString() {
		return "PriceInfo [economyClassAirfare=" + economyClassAirfare + ", economyClassChildAirfare=" + economyClassChildAirfare
				+ ", economyClassSmallChildAirfare=" + economyClassSmallChildAirfare + ", businessClassAirfare=" + businessClassAirfare
				+ ", businessClassChildAirfare=" + businessClassChildAirfare + ", businessClassSmallChildAirfare=" + businessClassSmallChildAirfare
				+ ", firstClassAirfare=" + firstClassAirfare + ", firstClassChildAirfare=" + firstClassChildAirfare + ", firstClassSmallChildAirfare="
				+ firstClassSmallChildAirfare + ", flightTaxes=" + flightTaxes + ", currency=" + currency + ", currencyIso=" + currencyIso + "]";
	}

}
