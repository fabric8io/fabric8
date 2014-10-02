/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.model;

import javax.xml.namespace.QName;

/**
 * Provides features XML/XSD constants.
 */
public interface FeaturesNamespaces {

    String URI_0_0_0 = "";
    String URI_1_0_0 = "http://karaf.apache.org/xmlns/features/v1.0.0";
    String URI_1_1_0 = "http://karaf.apache.org/xmlns/features/v1.1.0";
    String URI_1_2_0 = "http://karaf.apache.org/xmlns/features/v1.2.0";
	String URI_1_2_1 = "http://karaf.apache.org/xmlns/features/v1.2.1";
    String URI_1_3_0 = "http://karaf.apache.org/xmlns/features/v1.3.0";

    String URI_CURRENT = URI_1_3_0;

    QName FEATURES_0_0_0 = new QName("features");
    QName FEATURES_1_0_0 = new QName(URI_1_0_0, "features");
    QName FEATURES_1_1_0 = new QName(URI_1_1_0, "features");
    QName FEATURES_1_2_0 = new QName(URI_1_2_0, "features");
	QName FEATURES_1_2_1 = new QName(URI_1_2_1, "features");
    QName FEATURES_1_3_0 = new QName(URI_1_3_0, "features");

    QName FEATURES_CURRENT = FEATURES_1_3_0;

}
