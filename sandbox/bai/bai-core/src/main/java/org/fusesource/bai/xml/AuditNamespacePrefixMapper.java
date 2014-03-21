/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.bai.xml;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.apache.camel.builder.xml.Namespaces;
import org.fusesource.bai.AuditConstants;

/**
 * Use nicer namespace prefixes when marshalling
 */
public class AuditNamespacePrefixMapper extends NamespacePrefixMapper {
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (AuditConstants.AUDIT_NAMESPACE.equals(namespaceUri)) return "";
        if (Namespaces.DEFAULT_NAMESPACE.equals(namespaceUri)) return AuditConstants.EXPRESSION_NAMESPACE_PREFIX;
        return suggestion;
    }
}
