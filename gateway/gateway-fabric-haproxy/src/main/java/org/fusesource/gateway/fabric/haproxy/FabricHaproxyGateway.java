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
package org.fusesource.gateway.fabric.haproxy;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.internal.Objects;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.gateway.fabric.haproxy.model.BackEndServer;
import org.fusesource.gateway.fabric.haproxy.model.FrontEnd;
import org.fusesource.gateway.fabric.haproxy.model.OnValue;
import org.fusesource.gateway.handlers.http.HttpMappingRule;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An HTTP gateway which listens on a port and applies a number of {@link HttpMappingRuleConfiguration} instances to bind
 * HTTP requests to different HTTP based services running within the fabric.
 */
@Service(FabricHaproxyGateway.class)
@Component(name = "io.fabric8.gateway.haproxy", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 HAProxy Gateway",
        description = "Automatically generates a haproxy configuration file to implement a reverse proxy from haproxy to any web services or web applications running inside the fabric")
public class FabricHaproxyGateway extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricHaproxyGateway.class);
    private static final String TEMPLATE_FILE_NAME = "io.fabric8.gateway.haproxy.config.mvel";

    @Reference
    private Configurer configurer;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setFabricService", unbind = "unsetFabricService")
    private FabricService fabricService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = "setCurator", unbind = "unsetCurator")
    private CuratorFramework curator;

    @Property(name = "configFile",
            label = "Config file location", description = "The full file path of the generated configuration file created for haproxy to reuse")
    private String configFile;

    @Property(name = "reloadCommand",
            label = "Reload Haproxy Command", description = "The command line to execute when the haproxy configuration file has been regenerated so that haproxy can be safely restarted")
    private String reloadCommand;

    @Property(name = "reloadCommandDirectory",
            label = "Reload Command Directory", description = "The directory that should be used to run the reload command in")
    private String reloadCommandDirectory;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Set<HttpMappingRule> mappingRuleConfigurations = new CopyOnWriteArraySet<HttpMappingRule>();
    private Runnable changeListener = new Runnable() {
        @Override
        public void run() {
            try {
                rewriteConfigurationFile();
                reloadHaproxy();
            } catch (Exception e) {
                LOG.warn("Failed to write haproxy config file: " + e, e);
            }
        }
    };

    private String templateText;
    @GuardedBy("this")
    private final ParserContext parserContext = new ParserContext();
    private CompiledTemplate template;

    public void rewriteConfigurationFile() throws IOException {
        LOG.info("Writing HAProxy file: " + configFile);
        File outFile = new File(configFile);
        outFile.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(new FileWriter(outFile));
        try {
            Map<String, MappedServices> mappedServices = getMappedServices();

            CompiledTemplate compiledTemplate = getTemplate();
            Map<String, ?> data = createTemplateData();

            String renderedTemplate = TemplateRuntime.execute(compiledTemplate, parserContext, data).toString();
            writer.println(renderedTemplate);
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                LOG.debug("Caught while closing: " + e, e);
            }
        }
    }

    public void reloadHaproxy() throws Exception {
        if (Strings.isNotBlank(reloadCommand)) {
            LOG.info("Executing command: " + reloadCommand);
            Map<String, String> envVars = new HashMap<String, String>();
            envVars.putAll(System.getenv());

            // TODO set some other env vars?
            envVars.put("FABRIC8_HAPROXY_CONFIG", configFile);
            List<String> envVarList = new ArrayList<String>();
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                envVarList.add(entry.getKey() + "=" + entry.getValue());
            }
            final String[] envVarArray = envVarList.toArray(new String[envVarList.size()]);

            executor.execute(new Runnable() {

                @Override
                public void run() {
                    Process process = null;
                    Runtime runtime = Runtime.getRuntime();
                    try {
                        if (Strings.isNotBlank(reloadCommandDirectory)) {
                            File directory = new File(reloadCommandDirectory);
                            process = runtime.exec(reloadCommand, envVarArray, directory);
                        } else {
                            process = runtime.exec(reloadCommand, envVarArray);
                        }

                        forEachLine(process.getInputStream(), "stdout", new OnValue<String>() {
                            @Override
                            public void onValue(String value) {
                                LOG.info(value);
                            }
                        });

                        forEachLine(process.getErrorStream(), "stderr", new OnValue<String>() {
                            @Override
                            public void onValue(String value) {
                                LOG.error(value);
                            }
                        });
                        try {
                            int exitCode = process.waitFor();
                            LOG.info("command exit code: " + exitCode);
                        } catch (InterruptedException e) {
                            LOG.warn("Failed to wait for process exit code: " + e, e);
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to create process: " + reloadCommand + ". " + e, e);
                    } finally {
                        if (process != null) {
                            try {
                                process.destroy();
                            } catch (Exception e) {
                                LOG.warn("Failed to destroy the process: " + e, e);
                            }
                        }
                    }
                }
            });
        }
    }

    public static void forEachLine(InputStream inputStream, String nameOfStream, OnValue<String> lineCallback) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                lineCallback.onValue(line);
            }
        } catch (Exception e) {
            LOG.error("Failed to process " +
                    nameOfStream +
                    ": " + e, e);
        } finally {
            Closeables.closeQuitely(reader);
        }
    }

    protected Map<String, ?> createTemplateData() {
        Map<String, List<FrontEnd>> answer = new HashMap<String, List<FrontEnd>>();
        List<FrontEnd> frontEnds = new ArrayList<FrontEnd>();
        Set<Map.Entry<String, MappedServices>> entries = getMappedServices().entrySet();
        for (Map.Entry<String, MappedServices> entry : entries) {
            String uri = entry.getKey();
            MappedServices services = entry.getValue();
            String id = "b" + uri.replace('/', '_').replace('-', '_');
            while (id.endsWith("_")) {
                id = id.substring(0, id.length() - 1);
            }

            List<BackEndServer> backends = new ArrayList<BackEndServer>();
            FrontEnd frontEnd = new FrontEnd(id, uri, services, backends);
            frontEnds.add(frontEnd);
            Collection<String> serviceUrls = services.getServiceUrls();
            for (String serviceUrl : serviceUrls) {
                URL url = null;
                try {
                    url = new URL(serviceUrl);
                } catch (MalformedURLException e) {
                    LOG.warn("Ignore bad URL: " + e);
                }
                if (url != null) {
                    backends.add(new BackEndServer(url));
                }
            }
        }
        answer.put("frontEnds", frontEnds);
        return answer;
    }


    protected CompiledTemplate getTemplate() {
        String oldTemplateText = templateText;

        // lets lazy load template text from the fabric so we can configure it
        // explicitly to make testing outside of fabric easier
        if (templateText == null && fabricService != null) {
            Container current = fabricService.getCurrentContainer();
            byte[] bytes = current.getOverlayProfile().getFileConfiguration(TEMPLATE_FILE_NAME);
            if (bytes != null) {
                templateText = new String(bytes);
            }
        }
        Objects.notNull(templateText, "Could not find template text in profile config file: " + TEMPLATE_FILE_NAME);

        if (template == null || oldTemplateText == null || !oldTemplateText.equals(templateText)) {
            template = TemplateCompiler.compileTemplate(templateText, parserContext);
        }
        return template;
    }

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
    }


    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        updateConfiguration(configuration);
    }


    @Deactivate
    void deactivate() {
        deactivateInternal();
        deactivateComponent();
    }

    protected void updateConfiguration(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
    }

    protected void deactivateInternal() {
    }


    public void addMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        mappingRuleConfiguration.addChangeListener(changeListener);
        mappingRuleConfigurations.add(mappingRuleConfiguration);
    }


    public void removeMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        mappingRuleConfigurations.remove(mappingRuleConfiguration);
    }

    public Map<String, MappedServices> getMappedServices() {
        Map<String, MappedServices> answer = new HashMap<String, MappedServices>();
        for (HttpMappingRule mappingRuleConfiguration : mappingRuleConfigurations) {
            mappingRuleConfiguration.appendMappedServices(answer);
        }
        return answer;
    }

    // Properties
    //-------------------------------------------------------------------------

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void unsetCurator(CuratorFramework curator) {
        this.curator = null;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public void unsetFabricService(FabricService fabricService) {
        this.fabricService = null;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getReloadCommand() {
        return reloadCommand;
    }

    public void setReloadCommand(String reloadCommand) {
        this.reloadCommand = reloadCommand;
    }

    public String getReloadCommandDirectory() {
        return reloadCommandDirectory;
    }

    public void setReloadCommandDirectory(String reloadCommandDirectory) {
        this.reloadCommandDirectory = reloadCommandDirectory;
    }

    /**
     * The source of the mvel template which is usually lazily
     * fetched from the fabric profile; though can be set explicitly
     * when testing this class outside of a fabric
     */
    public String getTemplateText() {
        return templateText;
    }

    public void setTemplateText(String templateText) {
        this.templateText = templateText;
    }

    /**
     * Returns the default profile version used to filter out the current versions of services
     * if no version expression is used the URI template
     */
    public String getGatewayVersion() {
        FabricService fabricService = getFabricService();
        if (fabricService != null) {
            Container currentContainer = fabricService.getCurrentContainer();
            if (currentContainer != null) {
                Version version = currentContainer.getVersion();
                if (version != null) {
                    return version.getId();
                }
            }
        }
        return null;
    }
}
