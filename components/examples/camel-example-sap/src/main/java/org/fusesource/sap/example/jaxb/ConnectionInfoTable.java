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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAXB Bean containing Connection Information Table data from a SAP BOOK_FLIGHT response.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
@XmlRootElement(name="CONNINFO_TABLE", namespace="http://sap.fusesource.org/rfc/nplServer/BOOK_FLIGHT")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConnectionInfoTable {
	
	@XmlElement(name="row")
	List<ConnectionInfo> rows;

	public List<ConnectionInfo> getRows() {
		return rows;
	}

	public void setRows(List<ConnectionInfo> rows) {
		this.rows = rows;
	}

	@Override
	public String toString() {
		return "ConnectionInfoList [rows=" + rows + "]";
	}
	
}
