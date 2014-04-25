/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api.jmx;

/**
 * A JMX API for working with the OSGi MetaType API
 */
public interface MetaTypeFacadeMXBean {
    /**
     * Returns the MetaType information for the given bundle symbolic name
     */
    MetaTypeInformationDTO getMetaTypeInformation(String bundleSymbolicName);

    /**
     * Returns the MetaType information for the given bundle ID
     */
    MetaTypeInformationDTO getMetaTypeInformationForBundleId(long bundleId);

    MetaTypeSummaryDTO metaTypeSummary();

    MetaTypeObjectDTO getPidMetaTypeObject(String pid, String locale);

    MetaTypeObjectDTO getMetaTypeObject(long bundleId, String pid, String locale);
}
