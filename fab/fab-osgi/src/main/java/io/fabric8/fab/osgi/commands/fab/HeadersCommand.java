/**
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

package io.fabric8.fab.osgi.commands.fab;

import jline.Terminal;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import io.fabric8.fab.osgi.FabBundleInfo;
import io.fabric8.fab.osgi.ServiceConstants;
import io.fabric8.fab.osgi.commands.CommandSupport;
import org.fusesource.common.util.Strings;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

import java.util.*;

/**
 * Shows the OSGi headers of a bundle or FAB via its url before it is deployed
 */
@Command(name = "headers", scope = "fab", description = "Display the headers of a bundle or FAB")
public class HeadersCommand extends CommandSupport {
    protected final static String BUNDLE_PREFIX = "Bundle-";
    protected final static String PACKAGE_SUFFFIX = "-Package";
    protected final static String SERVICE_SUFFIX = "-Service";
    protected final static String REQUIRE_BUNDLE_ATTRIB = "Require-Bundle";

    @Option(name = "--indent", description = "Indentation method")
    int indent = -1;

    @Argument(index = 0, name = "jar", description = "The Bundle URL or file", required = true)
    private String fab;

    @Override
    protected Object doExecute() throws Exception {
        Map attributes = new HashMap();

        // if the command is invoked using a URL, we resolve the FAB info using the service
        // for an existing bundle, we just use the bundle headers directly
        if (fab.matches("\\d+")) {
            Bundle bundle = getBundle(fab);
            if (bundle != null) {
                Dictionary<String, String> headers = bundle.getHeaders();
                Enumeration<String> enumeration = headers.keys();
                while (enumeration.hasMoreElements()) {
                    String key = enumeration.nextElement();
                    attributes.put(key, headers.get(key));
                }
            }
        } else {
            FabBundleInfo info = getFabBundleInfo(fab);
            if (info != null) {
                attributes = info.getManifest();
            }
        }

        println(generateFormattedOutput(attributes));
        return null;
    }

    protected String generateFormattedOutput(Map attributes) {
        StringBuilder output = new StringBuilder();
        Map<String, Object> otherAttribs = new HashMap<String, Object>();
        Map<String, Object> bundleAttribs = new HashMap<String, Object>();
        Map<String, Object> serviceAttribs = new HashMap<String, Object>();
        Map<String, Object> packagesAttribs = new HashMap<String, Object>();
        Set<Object> keys = attributes.keySet();
        for (Object key : keys) {
            String k = key.toString();
            Object v = attributes.get(key);
            if (k.startsWith(BUNDLE_PREFIX)) {
                // starts with Bundle-xxx
                bundleAttribs.put(k, v);
            } else if (k.endsWith(SERVICE_SUFFIX)) {
                // ends with xxx-Service
                serviceAttribs.put(k, v);
            } else if (k.endsWith(PACKAGE_SUFFFIX)) {
                // ends with xxx-Package
                packagesAttribs.put(k, v);
            } else if (k.endsWith(REQUIRE_BUNDLE_ATTRIB)) {
                // require bundle statement
                packagesAttribs.put(k, v);
            } else {
                // the remaining attribs
                otherAttribs.put(k, v);
            }
        }

        // we will display the formatted result like this:
        // Bundle-Name (ID)
        // -----------------------
        // all other attributes
        //
        // all Bundle attributes
        //
        // all Service attributes
        //
        // all Package attributes
        Iterator<Map.Entry<String, Object>> it = otherAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), Strings.toString(e.getValue())));
        }
        if (otherAttribs.size() > 0) {
            output.append('\n');
        }

        it = bundleAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(String.format("%s = %s\n", e.getKey(), Strings.toString(e.getValue())));
        }
        if (bundleAttribs.size() > 0) {
            output.append('\n');
        }

        it = serviceAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(e.getKey());
            output.append(" = \n");
            formatHeader(Strings.toString(e.getValue()), null, output, indent);
            output.append("\n");
        }
        if (serviceAttribs.size() > 0) {
            output.append('\n');
        }

        Map<String, ClauseFormatter> formatters = new HashMap<String, ClauseFormatter>();
        formatters.put(ServiceConstants.INSTR_REQUIRE_BUNDLE, new ClauseFormatter() {
            public void pre(Clause clause, StringBuilder output) {
                boolean isSatisfied = checkBundle(clause.getName(), clause.getAttribute("version"));
                Ansi.ansi(output).fg(isSatisfied ? Ansi.Color.DEFAULT : Ansi.Color.RED).a("");
            }

            public void post(Clause clause, StringBuilder output) {
                Ansi.ansi(output).reset().a("");
            }
        });
        formatters.put(ServiceConstants.INSTR_IMPORT_PACKAGE, new ClauseFormatter() {
            public void pre(Clause clause, StringBuilder output) {
                boolean isSatisfied = checkPackage(clause.getName(), clause.getAttribute("version"));
                boolean isOptional = "optional".equals(clause.getDirective("resolution"));
                Ansi.ansi(output).fg(isSatisfied ? Ansi.Color.DEFAULT : Ansi.Color.RED)
                        .a(isSatisfied || isOptional ? Ansi.Attribute.INTENSITY_BOLD_OFF : Ansi.Attribute.INTENSITY_BOLD)
                        .a("");
            }

            public void post(Clause clause, StringBuilder output) {
                Ansi.ansi(output).reset().a("");
            }
        });

        it = packagesAttribs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            output.append(e.getKey());
            output.append(" = \n");
            formatHeader(Strings.toString(e.getValue()), formatters.get(e.getKey()), output, indent);
            output.append("\n");
        }
        if (packagesAttribs.size() > 0) {
            output.append('\n');
        }

        return output.toString();
    }

    protected interface ClauseFormatter {
        void pre(Clause clause, StringBuilder output);

        void post(Clause clause, StringBuilder output);
    }

    protected void formatHeader(String header, ClauseFormatter formatter, StringBuilder builder, int indent) {
        Clause[] clauses = Parser.parseHeader(header);
        formatClauses(clauses, formatter, builder, indent);
    }

    protected void formatClauses(Clause[] clauses, ClauseFormatter formatter, StringBuilder builder, int indent) {
        boolean first = true;
        for (Clause clause : clauses) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }
            formatClause(clause, formatter, builder, indent);
        }
    }

    protected void formatClause(Clause clause, ClauseFormatter formatter, StringBuilder builder, int indent) {
        builder.append("\t");
        if (formatter != null) {
            formatter.pre(clause, builder);
        }
        formatClause(clause, builder, indent);
        if (formatter != null) {
            formatter.post(clause, builder);
        }
    }

    protected int getTermWidth() {
        Terminal term = (Terminal) session.get(".jline.terminal");
        return term != null ? term.getWidth() : 80;

    }

    protected void formatClause(Clause clause, StringBuilder builder, int indent) {
        if (indent < 0) {
            if (clause.toString().length() < getTermWidth() - 8) { // -8 for tabs
                indent = 1;
            } else {
                indent = 3;
            }
        }
        String name = clause.getName();
        Directive[] directives = clause.getDirectives();
        Attribute[] attributes = clause.getAttributes();
        Arrays.sort(directives, new Comparator<Directive>() {
            public int compare(Directive o1, Directive o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Arrays.sort(attributes, new Comparator<Attribute>() {
            public int compare(Attribute o1, Attribute o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        builder.append(name);
        for (int i = 0; directives != null && i < directives.length; i++) {
            builder.append(";");
            if (indent > 1) {
                builder.append("\n\t\t");
            }
            builder.append(directives[i].getName()).append(":=");
            String v = directives[i].getValue();
            if (v.contains(",")) {
                if (indent > 2 && v.length() > 20) {
                    v = v.replace(",", ",\n\t\t\t");
                }
                builder.append("\"").append(v).append("\"");
            } else {
                builder.append(v);
            }
        }
        for (int i = 0; attributes != null && i < attributes.length; i++) {
            builder.append(";");
            if (indent > 1) {
                builder.append("\n\t\t");
            }
            builder.append(attributes[i].getName()).append("=");
            String v = attributes[i].getValue();
            if (v.contains(",")) {
                if (indent > 2 && v.length() > 20) {
                    v = v.replace(",", ",\n\t\t\t");
                }
                builder.append("\"").append(v).append("\"");
            } else {
                builder.append(v);
            }
        }
    }


    private boolean checkBundle(String bundleName, String version) {
        PackageAdmin admin = getPackageAdmin();
        if (admin != null) {
            Bundle[] bundles = admin.getBundles(bundleName, version);
            return bundles != null && bundles.length > 0;
        }
        return false;
    }

    private boolean checkPackage(String packageName, String version) {
        VersionRange range = VersionRange.parseVersionRange(version);
        PackageAdmin admin = getPackageAdmin();
        if (admin != null) {
            ExportedPackage[] packages = admin.getExportedPackages(packageName);
            if (packages != null) {
                for (ExportedPackage export : packages) {
                    if (range.contains(export.getVersion())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
