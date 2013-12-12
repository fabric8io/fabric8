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
package io.fabric8.agent.resolver;

import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResourceBuilder {

    public static final String RESOLUTION_DYNAMIC = "dynamic";

    public static Resource build(String uri, Map<String, String> headerMap)
            throws BundleException {

        // Verify that only manifest version 2 is specified.
        String manifestVersion = getManifestVersion(headerMap);
        if (manifestVersion == null || !manifestVersion.equals("2")) {
            throw new BundleException("Unsupported 'Bundle-ManifestVersion' value: " + manifestVersion);
        }

        //
        // Parse bundle version.
        //

        Version bundleVersion = Version.emptyVersion;
        if (headerMap.get(Constants.BUNDLE_VERSION) != null) {
            bundleVersion = Version.parseVersion(headerMap.get(Constants.BUNDLE_VERSION));
        }

        //
        // Parse bundle symbolic name.
        //

        String bundleSymbolicName = null;
        ParsedHeaderClause bundleCap = parseBundleSymbolicName(headerMap);
        if (bundleCap == null) {
            throw new BundleException("Bundle manifest must include bundle symbolic name");
        }
        bundleSymbolicName = (String) bundleCap.attrs.get(BundleRevision.BUNDLE_NAMESPACE);

        // Now that we have symbolic name and version, create the resource
        ResourceImpl resource = new ResourceImpl(bundleSymbolicName, bundleVersion);
        if (uri != null) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(UriNamespace.URI_NAMESPACE, uri);
            resource.addCapability(new CapabilityImpl(resource, UriNamespace.URI_NAMESPACE, Collections.<String, String>emptyMap(), attrs));
        }

        // Add a bundle and host capability to all
        // non-fragment bundles. A host capability is the same
        // as a require capability, but with a different capability
        // namespace. Bundle capabilities resolve required-bundle
        // dependencies, while host capabilities resolve fragment-host
        // dependencies.
        if (headerMap.get(Constants.FRAGMENT_HOST) == null) {
            // All non-fragment bundles have bundle capability.
            resource.addCapability(new CapabilityImpl(resource, BundleRevision.BUNDLE_NAMESPACE, bundleCap.dirs, bundleCap.attrs));
            // A non-fragment bundle can choose to not have a host capability.
            String attachment = bundleCap.dirs.get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
            attachment = (attachment == null) ? Constants.FRAGMENT_ATTACHMENT_RESOLVETIME : attachment;
            if (!attachment.equalsIgnoreCase(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
                Map<String, Object> hostAttrs = new HashMap<String, Object>(bundleCap.attrs);
                Object value = hostAttrs.remove(BundleRevision.BUNDLE_NAMESPACE);
                hostAttrs.put(BundleRevision.HOST_NAMESPACE, value);
                resource.addCapability(new CapabilityImpl(
                        resource, BundleRevision.HOST_NAMESPACE,
                        bundleCap.dirs,
                        hostAttrs));
            }
        }

        //
        // Parse Fragment-Host.
        //

        List<RequirementImpl> hostReqs = parseFragmentHost(resource, headerMap);

        //
        // Parse Require-Bundle
        //

        List<ParsedHeaderClause> rbClauses = parseStandardHeader(headerMap.get(Constants.REQUIRE_BUNDLE));
        rbClauses = normalizeRequireClauses(rbClauses);
        List<Requirement> rbReqs = convertRequires(rbClauses, resource);

        //
        // Parse Import-Package.
        //

        List<ParsedHeaderClause> importClauses = parseStandardHeader(headerMap.get(Constants.IMPORT_PACKAGE));
        importClauses = normalizeImportClauses(importClauses);
        List<Requirement> importReqs = convertImports(importClauses, resource);

        //
        // Parse DynamicImport-Package.
        //

        List<ParsedHeaderClause> dynamicClauses = parseStandardHeader(headerMap.get(Constants.DYNAMICIMPORT_PACKAGE));
        dynamicClauses = normalizeDynamicImportClauses(dynamicClauses);
        List<Requirement> dynamicReqs = convertImports(dynamicClauses, resource);

        //
        // Parse Require-Capability.
        //

        List<ParsedHeaderClause> requireClauses = parseStandardHeader(headerMap.get(Constants.REQUIRE_CAPABILITY));
        requireClauses = normalizeRequireCapabilityClauses(requireClauses);
        List<Requirement> requireReqs = convertRequireCapabilities(requireClauses, resource);

        //
        // Parse Export-Package.
        //

        List<ParsedHeaderClause> exportClauses = parseStandardHeader(headerMap.get(Constants.EXPORT_PACKAGE));
        exportClauses = normalizeExportClauses(exportClauses, bundleSymbolicName, bundleVersion);
        List<Capability> exportCaps = convertExports(exportClauses, resource);

        //
        // Parse Provide-Capability.
        //

        List<ParsedHeaderClause> provideClauses = parseStandardHeader(headerMap.get(Constants.PROVIDE_CAPABILITY));
        provideClauses = normalizeProvideCapabilityClauses(provideClauses);
        List<Capability> provideCaps = convertProvideCapabilities(provideClauses, resource);

        //
        // Parse Import-Service and Export-Service
        // if Require-Capability and Provide-Capability are not set for services
        //

        boolean hasServiceReferenceCapability = false;
        for (Capability cap : exportCaps) {
            hasServiceReferenceCapability |= ServiceNamespace.SERVICE_NAMESPACE.equals(cap.getNamespace());
        }
        if (!hasServiceReferenceCapability) {
            List<ParsedHeaderClause> exportServices = parseStandardHeader(headerMap.get(Constants.EXPORT_SERVICE));
            List<Capability> caps = convertExportService(exportServices, resource);
            provideCaps.addAll(caps);
        }

        boolean hasServiceReferenceRequirement = false;
        for (Requirement req : requireReqs) {
            hasServiceReferenceRequirement |= ServiceNamespace.SERVICE_NAMESPACE.equals(req.getNamespace());
        }
        if (!hasServiceReferenceRequirement) {
            List<ParsedHeaderClause> importServices = parseStandardHeader(headerMap.get(Constants.IMPORT_SERVICE));
            List<Requirement> reqs = convertImportService(importServices, resource);
            requireReqs.addAll(reqs);
        }

        // Combine all capabilities.
        resource.addCapabilities(exportCaps);
        resource.addCapabilities(provideCaps);

        // Combine all requirements.
        resource.addRequirements(hostReqs);
        resource.addRequirements(importReqs);
        resource.addRequirements(rbReqs);
        resource.addRequirements(requireReqs);
        resource.addRequirements(dynamicReqs);

        return resource;
    }

    public static List<Requirement> parseImport(Resource resource, String imports) throws BundleException {
        List<ParsedHeaderClause> importClauses = parseStandardHeader(imports);
        importClauses = normalizeImportClauses(importClauses);
        List<Requirement> importReqs = convertImports(importClauses, resource);
        return importReqs;
    }

    public static List<Requirement> parseRequirement(Resource resource, String requirement) throws BundleException {
        List<ParsedHeaderClause> requireClauses = parseStandardHeader(requirement);
        requireClauses = normalizeRequireCapabilityClauses(requireClauses);
        List<Requirement> requireReqs = convertRequireCapabilities(requireClauses, resource);
        return requireReqs;
    }

    public static List<Capability> parseExport(Resource resource, String bundleSymbolicName, Version bundleVersion, String exports) throws BundleException {
        List<ParsedHeaderClause> exportClauses = parseStandardHeader(exports);
        exportClauses = normalizeExportClauses(exportClauses, bundleSymbolicName, bundleVersion);
        List<Capability> exportCaps = convertExports(exportClauses, resource);
        return exportCaps;
    }

    public static List<Capability> parseCapability(Resource resource, String capability) throws BundleException {
        List<ParsedHeaderClause> provideClauses = parseStandardHeader(capability);
        provideClauses = normalizeProvideCapabilityClauses(provideClauses);
        List<Capability> provideCaps = convertProvideCapabilities(provideClauses, resource);
        return provideCaps;
    }

    @SuppressWarnings( "deprecation" )
    private static List<ParsedHeaderClause> normalizeImportClauses(
            List<ParsedHeaderClause> clauses)
            throws BundleException {
        // Verify that the values are equals if the package specifies
        // both version and specification-version attributes.
        Set<String> dupeSet = new HashSet<String>();
        for (ParsedHeaderClause clause : clauses) {
            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            Object v = clause.attrs.get(Constants.VERSION_ATTRIBUTE);
            Object sv = clause.attrs.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null)) {
                // Verify they are equal.
                if (!((String) v).trim().equals(((String) sv).trim())) {
                    throw new IllegalArgumentException(
                            "Both version and specification-version are specified, but they are not equal.");
                }
            }

            // Ensure that only the "version" attribute is used and convert
            // it to the VersionRange type.
            if ((v != null) || (sv != null)) {
                clause.attrs.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
                v = (v == null) ? sv : v;
                clause.attrs.put(Constants.VERSION_ATTRIBUTE, VersionRange.parseVersionRange(v.toString()));
            }

            // If bundle version is specified, then convert its type to VersionRange.
            v = clause.attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            if (v != null) {
                clause.attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, VersionRange.parseVersionRange(v.toString()));
            }

            // Verify java.* is not imported, nor any duplicate imports.
            for (String pkgName : clause.paths) {
                if (!dupeSet.contains(pkgName)) {
                    // Verify that java.* packages are not imported.
                    if (pkgName.startsWith("java.")) {
                        throw new BundleException("Importing java.* packages not allowed: " + pkgName);
                    }
                    // The character "." has no meaning in the OSGi spec except
                    // when placed on the bundle class path. Some people, however,
                    // mistakenly think it means the default package when imported
                    // or exported. This is not correct. It is invalid.
                    else if (pkgName.equals(".")) {
                        throw new BundleException("Importing '.' is invalid.");
                    }
                    // Make sure a package name was specified.
                    else if (pkgName.length() == 0) {
                        throw new BundleException(
                                "Imported package names cannot be zero length.");
                    }
                    dupeSet.add(pkgName);
                } else {
                    throw new BundleException("Duplicate import: " + pkgName);
                }
            }
        }

        return clauses;
    }

    private static List<Capability> convertExportService(List<ParsedHeaderClause> clauses, Resource resource) {
        List<Capability> capList = new ArrayList<Capability>();
        for (ParsedHeaderClause clause : clauses) {
            for (String path : clause.paths) {
                Map<String, String> dirs = new LinkedHashMap<String, String>();
                dirs.put(ServiceNamespace.CAPABILITY_EFFECTIVE_DIRECTIVE, ServiceNamespace.EFFECTIVE_ACTIVE);
                Map<String, Object> attrs = new LinkedHashMap<String, Object>();
                attrs.put(Constants.OBJECTCLASS, path);
                attrs.putAll(clause.attrs);
                capList.add(new CapabilityImpl(
                                resource,
                                ServiceNamespace.SERVICE_NAMESPACE,
                                dirs,
                                attrs));
            }
        }
        return capList;
    }

    private static List<Requirement> convertImportService(List<ParsedHeaderClause> clauses, Resource resource) throws BundleException {
        try {
            List<Requirement> reqList = new ArrayList<Requirement>();
            for (ParsedHeaderClause clause : clauses) {
                for (String path : clause.paths) {
                    String multiple = clause.dirs.get("multiple");
                    String avail    = clause.dirs.get("availability");
                    String filter   = (String) clause.attrs.get("filter");
                    Map<String, String> dirs = new LinkedHashMap<String, String>();
                    dirs.put(ServiceNamespace.REQUIREMENT_EFFECTIVE_DIRECTIVE, ServiceNamespace.EFFECTIVE_ACTIVE);
                    if ("optional".equals(avail)) {
                        dirs.put(ServiceNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE, ServiceNamespace.RESOLUTION_OPTIONAL);
                    }
                    if ("true".equals(multiple)) {
                        dirs.put(ServiceNamespace.REQUIREMENT_CARDINALITY_DIRECTIVE, ServiceNamespace.CARDINALITY_MULTIPLE);
                    }
                    if (filter == null) {
                        filter = "(" + Constants.OBJECTCLASS + "=" + path + ")";
                    } else if (!filter.startsWith("(") && !filter.endsWith(")")) {
                        filter = "(&(" + Constants.OBJECTCLASS + "=" + path + ")(" + filter + "))";
                    } else {
                        filter = "(&(" + Constants.OBJECTCLASS + "=" + path + ")" + filter + ")";
                    }
                    dirs.put(ServiceNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
                    reqList.add(new RequirementImpl(
                                    resource,
                                    ServiceNamespace.SERVICE_NAMESPACE,
                                    dirs,
                                    Collections.<String, Object>emptyMap(),
                                    SimpleFilter.parse(filter)));
                }
            }
            return reqList;
        } catch (Exception ex) {
            throw new BundleException("Error creating requirement: " + ex, ex);
        }
    }

    private static List<Requirement> convertImports(List<ParsedHeaderClause> clauses, Resource resource) {
        // Now convert generic header clauses into requirements.
        List<Requirement> reqList = new ArrayList<Requirement>();
        for (ParsedHeaderClause clause : clauses) {
            for (String path : clause.paths) {
                // Prepend the package name to the array of attributes.
                Map<String, Object> attrs = clause.attrs;
                // Note that we use a linked hash map here to ensure the
                // package attribute is first, which will make indexing
                // more efficient.
    // TODO: OSGi R4.3 - This is ordering is kind of hacky.
                // Prepend the package name to the array of attributes.
                Map<String, Object> newAttrs = new LinkedHashMap<String, Object>(attrs.size() + 1);
                // We want this first from an indexing perspective.
                newAttrs.put(BundleRevision.PACKAGE_NAMESPACE, path);
                newAttrs.putAll(attrs);
                // But we need to put it again to make sure it wasn't overwritten.
                newAttrs.put(BundleRevision.PACKAGE_NAMESPACE, path);

                // Create filter now so we can inject filter directive.
                SimpleFilter sf = SimpleFilter.convert(newAttrs);

                // Inject filter directive.
    // TODO: OSGi R4.3 - Can we insert this on demand somehow?
                Map<String, String> dirs = clause.dirs;
                Map<String, String> newDirs = new HashMap<String, String>(dirs.size() + 1);
                newDirs.putAll(dirs);
                newDirs.put(Constants.FILTER_DIRECTIVE, sf.toString());

                // Create package requirement and add to requirement list.
                reqList.add(
                        new RequirementImpl(
                                resource,
                                BundleRevision.PACKAGE_NAMESPACE,
                                newDirs,
                                Collections.<String, Object>emptyMap(),
                                sf));
            }
        }

        return reqList;
    }

    @SuppressWarnings( "deprecation" )
    private static List<ParsedHeaderClause> normalizeDynamicImportClauses(
            List<ParsedHeaderClause> clauses)
            throws BundleException {
        // Verify that the values are equals if the package specifies
        // both version and specification-version attributes.
        for (ParsedHeaderClause clause : clauses) {
            // Add the resolution directive to indicate that these are
            // dynamic imports.
            clause.dirs.put(Constants.RESOLUTION_DIRECTIVE, RESOLUTION_DYNAMIC);

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            Object v = clause.attrs.get(Constants.VERSION_ATTRIBUTE);
            Object sv = clause.attrs.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null)) {
                // Verify they are equal.
                if (!((String) v).trim().equals(((String) sv).trim())) {
                    throw new IllegalArgumentException(
                            "Both version and specification-version are specified, but they are not equal.");
                }
            }

            // Ensure that only the "version" attribute is used and convert
            // it to the VersionRange type.
            if ((v != null) || (sv != null)) {
                clause.attrs.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
                v = (v == null) ? sv : v;
                clause.attrs.put(Constants.VERSION_ATTRIBUTE, VersionRange.parseVersionRange(v.toString()));
            }

            // If bundle version is specified, then convert its type to VersionRange.
            v = clause.attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            if (v != null) {
                clause.attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, VersionRange.parseVersionRange(v.toString()));
            }

            // Dynamic imports can have duplicates, so verify that java.*
            // packages are not imported.
            for (String pkgName : clause.paths) {
                if (pkgName.startsWith("java.")) {
                    throw new BundleException("Dynamically importing java.* packages not allowed: " + pkgName);
                } else if (!pkgName.equals("*") && pkgName.endsWith("*") && !pkgName.endsWith(".*")) {
                    throw new BundleException("Partial package name wild carding is not allowed: " + pkgName);
                }
            }
        }

        return clauses;
    }

    private static List<ParsedHeaderClause> normalizeRequireCapabilityClauses(
            List<ParsedHeaderClause> clauses)
            throws BundleException {

        return clauses;
    }

    private static List<ParsedHeaderClause> normalizeProvideCapabilityClauses(
            List<ParsedHeaderClause> clauses)
            throws BundleException
    {

        // Convert attributes into specified types.
        for (ParsedHeaderClause clause : clauses)
        {
            for (Map.Entry<String, String> entry : clause.types.entrySet())
            {
                String type = entry.getValue();
                if (!type.equals("String"))
                {
                    if (type.equals("Double"))
                    {
                        clause.attrs.put(
                                entry.getKey(),
                                new Double(clause.attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.equals("Version"))
                    {
                        clause.attrs.put(
                                entry.getKey(),
                                new Version(clause.attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.equals("Long"))
                    {
                        clause.attrs.put(
                                entry.getKey(),
                                new Long(clause.attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.startsWith("List"))
                    {
                        int startIdx = type.indexOf('<');
                        int endIdx = type.indexOf('>');
                        if (((startIdx > 0) && (endIdx <= startIdx))
                                || ((startIdx < 0) && (endIdx > 0)))
                        {
                            throw new BundleException(
                                    "Invalid Provide-Capability attribute list type for '"
                                            + entry.getKey()
                                            + "' : "
                                            + type);
                        }

                        String listType = "String";
                        if (endIdx > startIdx)
                        {
                            listType = type.substring(startIdx + 1, endIdx).trim();
                        }

                        List<String> tokens = parseDelimitedString(
                                clause.attrs.get(entry.getKey()).toString(), ",", false);
                        List<Object> values = new ArrayList<Object>(tokens.size());
                        for (String token : tokens)
                        {
                            if (listType.equals("String"))
                            {
                                values.add(token);
                            }
                            else if (listType.equals("Double"))
                            {
                                values.add(new Double(token.trim()));
                            }
                            else if (listType.equals("Version"))
                            {
                                values.add(new Version(token.trim()));
                            }
                            else if (listType.equals("Long"))
                            {
                                values.add(new Long(token.trim()));
                            }
                            else
                            {
                                throw new BundleException(
                                        "Unknown Provide-Capability attribute list type for '"
                                                + entry.getKey()
                                                + "' : "
                                                + type);
                            }
                        }
                        clause.attrs.put(
                                entry.getKey(),
                                values);
                    }
                    else
                    {
                        throw new BundleException(
                                "Unknown Provide-Capability attribute type for '"
                                        + entry.getKey()
                                        + "' : "
                                        + type);
                    }
                }
            }
        }

        return clauses;
    }

    private static List<Requirement> convertRequireCapabilities(
            List<ParsedHeaderClause> clauses, Resource resource)
            throws BundleException {
        // Now convert generic header clauses into requirements.
        List<Requirement> reqList = new ArrayList<Requirement>();
        for (ParsedHeaderClause clause : clauses) {
            try {
                String filterStr = clause.dirs.get(Constants.FILTER_DIRECTIVE);
                SimpleFilter sf = (filterStr != null)
                        ? SimpleFilter.parse(filterStr)
                        : new SimpleFilter(null, null, SimpleFilter.MATCH_ALL);
                for (String path : clause.paths) {
                    // Create requirement and add to requirement list.
                    reqList.add(new RequirementImpl(
                                    resource, path, clause.dirs, clause.attrs, sf));
                }
            } catch (Exception ex) {
                throw new BundleException("Error creating requirement: " + ex, ex);
            }
        }

        return reqList;
    }

    private static List<Capability> convertProvideCapabilities(
            List<ParsedHeaderClause> clauses, Resource resource)
            throws BundleException {
        List<Capability> capList = new ArrayList<Capability>();
        for (ParsedHeaderClause clause : clauses) {
            for (String path : clause.paths) {
                if (path.startsWith("osgi.wiring.")) {
//                    throw new BundleException("Manifest cannot use Provide-Capability for '" + path + "' namespace.");
                }

                // Create package capability and add to capability list.
                capList.add(new CapabilityImpl(resource, path, clause.dirs, clause.attrs));
            }
        }

        return capList;
    }

    @SuppressWarnings( "deprecation" )
    private static List<ParsedHeaderClause> normalizeExportClauses(
            List<ParsedHeaderClause> clauses,
            String bsn, Version bv)
            throws BundleException {
        // Verify that "java.*" packages are not exported.
        for (ParsedHeaderClause clause : clauses) {
            // Verify that the named package has not already been declared.
            for (String pkgName : clause.paths) {
                // Verify that java.* packages are not exported.
                if (pkgName.startsWith("java.")) {
                    throw new BundleException("Exporting java.* packages not allowed: " + pkgName);
                }
                // The character "." has no meaning in the OSGi spec except
                // when placed on the bundle class path. Some people, however,
                // mistakenly think it means the default package when imported
                // or exported. This is not correct. It is invalid.
                else if (pkgName.equals(".")) {
                    throw new BundleException("Exporing '.' is invalid.");
                }
                // Make sure a package name was specified.
                else if (pkgName.length() == 0) {
                    throw new BundleException("Exported package names cannot be zero length.");
                }
            }

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            Object v = clause.attrs.get(Constants.VERSION_ATTRIBUTE);
            Object sv = clause.attrs.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null)) {
                // Verify they are equal.
                if (!((String) v).trim().equals(((String) sv).trim())) {
                    throw new IllegalArgumentException("Both version and specification-version are specified, but they are not equal.");
                }
            }

            // Always add the default version if not specified.
            if ((v == null) && (sv == null)) {
                v = Version.emptyVersion;
            }

            // Ensure that only the "version" attribute is used and convert
            // it to the appropriate type.
            if ((v != null) || (sv != null)) {
                // Convert version attribute to type Version.
                clause.attrs.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
                v = (v == null) ? sv : v;
                clause.attrs.put(Constants.VERSION_ATTRIBUTE, Version.parseVersion(v.toString()));
            }

            // Find symbolic name and version attribute, if present.
            if (clause.attrs.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE)
                    || clause.attrs.containsKey(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)) {
                throw new BundleException("Exports must not specify bundle symbolic name or bundle version.");
            }

            // Now that we know that there are no bundle symbolic name and version
            // attributes, add them since the spec says they are there implicitly.
            clause.attrs.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bsn);
            clause.attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bv);
        }

        return clauses;
    }

    private static List<Capability> convertExports(
            List<ParsedHeaderClause> clauses, Resource resource) {
        List<Capability> capList = new ArrayList<Capability>();
        for (ParsedHeaderClause clause : clauses) {
            for (String pkgName : clause.paths) {
                // Prepend the package name to the array of attributes.
                Map<String, Object> attrs = clause.attrs;
                Map<String, Object> newAttrs = new HashMap<String, Object>(attrs.size() + 1);
                newAttrs.putAll(attrs);
                newAttrs.put(BundleRevision.PACKAGE_NAMESPACE, pkgName);

                // Create package capability and add to capability list.
                capList.add(new CapabilityImpl(resource, BundleRevision.PACKAGE_NAMESPACE, clause.dirs, newAttrs));
            }
        }

        return capList;
    }

    private static String getManifestVersion(Map<String, String> headerMap) {
        String manifestVersion = headerMap.get(Constants.BUNDLE_MANIFESTVERSION);
        return (manifestVersion == null) ? "1" : manifestVersion.trim();
    }

    private static List<ParsedHeaderClause> calculateImplicitImports(
            List<BundleCapability> exports, List<ParsedHeaderClause> imports)
            throws BundleException {
        List<ParsedHeaderClause> clauseList = new ArrayList<ParsedHeaderClause>();

        // Since all R3 exports imply an import, add a corresponding
        // requirement for each existing export capability. Do not
        // duplicate imports.
        Map<String, String> map = new HashMap<String, String>();
        // Add existing imports.
        for (ParsedHeaderClause anImport : imports) {
            for (int pathIdx = 0; pathIdx < anImport.paths.size(); pathIdx++) {
                map.put(anImport.paths.get(pathIdx), anImport.paths.get(pathIdx));
            }
        }
        // Add import requirement for each export capability.
        for (BundleCapability export : exports) {
            if (map.get(export.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString()) == null) {
                // Convert Version to VersionRange.
                Object version = export.getAttributes().get(Constants.VERSION_ATTRIBUTE);
                ParsedHeaderClause clause = new ParsedHeaderClause();
                if (version != null) {
                    clause.attrs.put(Constants.VERSION_ATTRIBUTE, VersionRange.parseVersionRange(version.toString()));
                }
                clause.paths.add((String) export.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE));
                clauseList.add(clause);
            }
        }

        return clauseList;
    }

    private static List<Capability> calculateImplicitUses(
            List<Capability> exports, List<ParsedHeaderClause> imports)
            throws BundleException {
        // Add a "uses" directive onto each export of R3 bundles
        // that references every other import (which will include
        // exports, since export implies import); this is
        // necessary since R3 bundles assumed a single class space,
        // but R4 allows for multiple class spaces.
        String usesValue = "";
        for (ParsedHeaderClause anImport : imports) {
            for (int pathIdx = 0; pathIdx < anImport.paths.size(); pathIdx++) {
                usesValue = usesValue
                        + ((usesValue.length() > 0) ? "," : "")
                        + anImport.paths.get(pathIdx);
            }
        }
        for (int i = 0; i < exports.size(); i++) {
            Map<String, String> dirs = new HashMap<String, String>(1);
            dirs.put(Constants.USES_DIRECTIVE, usesValue);
            exports.set(i, new CapabilityImpl(
                    exports.get(i).getResource(),
                    BundleRevision.PACKAGE_NAMESPACE,
                    dirs,
                    exports.get(i).getAttributes()));
        }

        return exports;
    }

    private static ParsedHeaderClause parseBundleSymbolicName(Map<String, String> headerMap)
            throws BundleException {
        List<ParsedHeaderClause> clauses = parseStandardHeader(headerMap.get(Constants.BUNDLE_SYMBOLICNAME));
        if (clauses.size() > 0) {
            if (clauses.size() > 1 || clauses.get(0).paths.size() > 1) {
                throw new BundleException("Cannot have multiple symbolic names: " + headerMap.get(Constants.BUNDLE_SYMBOLICNAME));
            }

            // Get bundle version.
            Version bundleVersion = Version.emptyVersion;
            if (headerMap.get(Constants.BUNDLE_VERSION) != null) {
                bundleVersion = Version.parseVersion(headerMap.get(Constants.BUNDLE_VERSION));
            }

            // Create a require capability and return it.
            ParsedHeaderClause clause = clauses.get(0);
            String symName = clause.paths.get(0);
            clause.attrs.put(BundleRevision.BUNDLE_NAMESPACE, symName);
            clause.attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion);
            return clause;
        }

        return null;
    }

    private static List<RequirementImpl> parseFragmentHost(
            Resource resource, Map<String, String> headerMap)
            throws BundleException {
        List<RequirementImpl> reqs = new ArrayList<RequirementImpl>();

        List<ParsedHeaderClause> clauses = parseStandardHeader(headerMap.get(Constants.FRAGMENT_HOST));
        if (clauses.size() > 0) {
            // Make sure that only one fragment host symbolic name is specified.
            if (clauses.size() > 1 || clauses.get(0).paths.size() > 1) {
                throw new BundleException("Fragments cannot have multiple hosts: " + headerMap.get(Constants.FRAGMENT_HOST));
            }

            // If the bundle-version attribute is specified, then convert
            // it to the proper type.
            Object value = clauses.get(0).attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            value = (value == null) ? "0.0.0" : value;
            clauses.get(0).attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, VersionRange.parseVersionRange(value.toString()));

            // Note that we use a linked hash map here to ensure the
            // host symbolic name is first, which will make indexing
            // more efficient.
    // TODO: OSGi R4.3 - This is ordering is kind of hacky.
            // Prepend the host symbolic name to the map of attributes.
            Map<String, Object> attrs = clauses.get(0).attrs;
            Map<String, Object> newAttrs = new LinkedHashMap<String, Object>(attrs.size() + 1);
            // We want this first from an indexing perspective.
            newAttrs.put(BundleRevision.HOST_NAMESPACE, clauses.get(0).paths.get(0));
            newAttrs.putAll(attrs);
            // But we need to put it again to make sure it wasn't overwritten.
            newAttrs.put(BundleRevision.HOST_NAMESPACE, clauses.get(0).paths.get(0));

            // Create filter now so we can inject filter directive.
            SimpleFilter sf = SimpleFilter.convert(newAttrs);

            // Inject filter directive.
    // TODO: OSGi R4.3 - Can we insert this on demand somehow?
            Map<String, String> dirs = clauses.get(0).dirs;
            Map<String, String> newDirs = new HashMap<String, String>(dirs.size() + 1);
            newDirs.putAll(dirs);
            newDirs.put(Constants.FILTER_DIRECTIVE, sf.toString());

            reqs.add(new RequirementImpl(
                    resource, BundleRevision.HOST_NAMESPACE,
                    newDirs,
                    newAttrs));
        }

        return reqs;
    }

    private static List<ParsedHeaderClause> normalizeRequireClauses(List<ParsedHeaderClause> clauses) {
        // Convert bundle version attribute to VersionRange type.
        for (ParsedHeaderClause clause : clauses) {
            Object value = clause.attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            if (value != null) {
                clause.attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, VersionRange.parseVersionRange(value.toString()));
            }
        }

        return clauses;
    }

    private static List<Requirement> convertRequires(List<ParsedHeaderClause> clauses, Resource resource) {
        List<Requirement> reqList = new ArrayList<Requirement>();
        for (ParsedHeaderClause clause : clauses) {
            for (String path : clause.paths) {
                // Prepend the bundle symbolic name to the array of attributes.
                Map<String, Object> attrs = clause.attrs;
                // Note that we use a linked hash map here to ensure the
                // symbolic name attribute is first, which will make indexing
                // more efficient.
    // TODO: OSGi R4.3 - This is ordering is kind of hacky.
                // Prepend the symbolic name to the array of attributes.
                Map<String, Object> newAttrs = new LinkedHashMap<String, Object>(attrs.size() + 1);
                // We want this first from an indexing perspective.
                newAttrs.put(BundleRevision.BUNDLE_NAMESPACE, path);
                newAttrs.putAll(attrs);
                // But we need to put it again to make sure it wasn't overwritten.
                newAttrs.put(BundleRevision.BUNDLE_NAMESPACE, path);

                // Create filter now so we can inject filter directive.
                SimpleFilter sf = SimpleFilter.convert(newAttrs);

                // Inject filter directive.
    // TODO: OSGi R4.3 - Can we insert this on demand somehow?
                Map<String, String> dirs = clause.dirs;
                Map<String, String> newDirs = new HashMap<String, String>(dirs.size() + 1);
                newDirs.putAll(dirs);
                newDirs.put(Constants.FILTER_DIRECTIVE, sf.toString());

                // Create package requirement and add to requirement list.
                reqList.add(new RequirementImpl(resource, BundleRevision.BUNDLE_NAMESPACE, newDirs, newAttrs));
            }
        }

        return reqList;
    }

    private static final char EOF = (char) -1;

    private static char charAt(int pos, String headers, int length)
    {
        if (pos >= length)
        {
            return EOF;
        }
        return headers.charAt(pos);
    }

    private static final int CLAUSE_START = 0;
    private static final int PARAMETER_START = 1;
    private static final int KEY = 2;
    private static final int DIRECTIVE_OR_TYPEDATTRIBUTE = 4;
    private static final int ARGUMENT = 8;
    private static final int VALUE = 16;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static List<ParsedHeaderClause> parseStandardHeader(String header)
    {
        List<ParsedHeaderClause> clauses = new ArrayList<ParsedHeaderClause>();
        if (header == null)
        {
            return clauses;
        }
        ParsedHeaderClause clause = null;
        String key = null;
        Map targetMap = null;
        int state = CLAUSE_START;
        int currentPosition = 0;
        int startPosition = 0;
        int length = header.length();
        boolean quoted = false;
        boolean escaped = false;

        char currentChar = EOF;
        do
        {
            currentChar = charAt(currentPosition, header, length);
            switch (state)
            {
                case CLAUSE_START:
                    clause = new ParsedHeaderClause();
                    clauses.add(clause);
                    state = PARAMETER_START;
                case PARAMETER_START:
                    startPosition = currentPosition;
                    state = KEY;
                case KEY:
                    switch (currentChar)
                    {
                        case ':':
                        case '=':
                            key = header.substring(startPosition, currentPosition).trim();
                            startPosition = currentPosition + 1;
                            targetMap = clause.attrs;
                            state = currentChar == ':' ? DIRECTIVE_OR_TYPEDATTRIBUTE : ARGUMENT;
                            break;
                        case EOF:
                        case ',':
                        case ';':
                            clause.paths.add(header.substring(startPosition, currentPosition).trim());
                            state = currentChar == ',' ? CLAUSE_START : PARAMETER_START;
                            break;
                        default:
                            break;
                    }
                    currentPosition++;
                    break;
                case DIRECTIVE_OR_TYPEDATTRIBUTE:
                    switch(currentChar)
                    {
                        case '=':
                            if (startPosition != currentPosition)
                            {
                                clause.types.put(key, header.substring(startPosition, currentPosition).trim());
                            }
                            else
                            {
                                targetMap = clause.dirs;
                            }
                            state = ARGUMENT;
                            startPosition = currentPosition + 1;
                            break;
                        default:
                            break;
                    }
                    currentPosition++;
                    break;
                case ARGUMENT:
                    if (currentChar == '\"')
                    {
                        quoted = true;
                        currentPosition++;
                    }
                    else
                    {
                        quoted = false;
                    }
                    if (!Character.isWhitespace(currentChar)) {
                        state = VALUE;
                    }
                    else {
                        currentPosition++;
                    }
                    break;
                case VALUE:
                    if (escaped)
                    {
                        escaped = false;
                    }
                    else
                    {
                        if (currentChar == '\\' )
                        {
                            escaped = true;
                        }
                        else if (quoted && currentChar == '\"')
                        {
                            quoted = false;
                        }
                        else if (!quoted)
                        {
                            String value = null;
                            switch(currentChar)
                            {
                                case EOF:
                                case ';':
                                case ',':
                                    value = header.substring(startPosition, currentPosition).trim();
                                    if (value.startsWith("\"") && value.endsWith("\""))
                                    {
                                        value = value.substring(1, value.length() - 1);
                                    }
                                    if (targetMap.put(key, value) != null)
                                    {
                                        throw new IllegalArgumentException(
                                                "Duplicate '" + key + "' in: " + header);
                                    }
                                    state = currentChar == ';' ? PARAMETER_START : CLAUSE_START;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    currentPosition++;
                    break;
                default:
                    break;
            }
        } while ( currentChar != EOF);

        if (state > PARAMETER_START)
        {
            throw new IllegalArgumentException("Unable to parse header: " + header);
        }
        return clauses;
    }

    public static List<String> parseDelimitedString(String value, String delim)
    {
        return parseDelimitedString(value, delim, true);
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return a list of string or an empty list if there are none.
     **/
    public static List<String> parseDelimitedString(String value, String delim, boolean trim)
    {
        if (value == null)
        {
            value = "";
        }

        List<String> list = new ArrayList();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        boolean isEscaped = false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);

            if (!isEscaped && (c == '\\'))
            {
                isEscaped = true;
                continue;
            }

            if (isEscaped)
            {
                sb.append(c);
            }
            else if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                if (trim)
                {
                    list.add(sb.toString().trim());
                }
                else
                {
                    list.add(sb.toString());
                }
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if ((c == '"') && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if ((c == '"') && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }

            isEscaped = false;
        }

        if (sb.length() > 0)
        {
            if (trim)
            {
                list.add(sb.toString().trim());
            }
            else
            {
                list.add(sb.toString());
            }
        }

        return list;
    }


    static class ParsedHeaderClause {
        public final List<String> paths = new ArrayList<String>();
        public final Map<String, String> dirs = new LinkedHashMap<String, String>();
        public final Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        public final Map<String, String> types = new LinkedHashMap<String, String>();
    }
}
