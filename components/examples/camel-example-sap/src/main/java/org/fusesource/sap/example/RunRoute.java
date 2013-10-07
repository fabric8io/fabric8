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
package org.fusesource.sap.example;

import org.apache.camel.spring.Main;

/**
 * Runs example sap camel route. 
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class RunRoute {

	/**
	 * Run example sap camel route
	 * 
	 * @param args - none
	 */
	public static void main(String[] args) throws Exception {
    	Main main = new Main();
    	main.setApplicationContextUri("classpath:META-INF/spring/camel-context.xml");
    	main.enableHangupSupport();
    	main.start();
	}

}
