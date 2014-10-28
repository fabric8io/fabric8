package io.fabric8.forge.camel.commands;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import io.fabric8.forge.camel.api.CamelSupportedTechnologyEnum;
import org.apache.commons.io.FileUtils;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.URLResource;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.rest.ClientFactory;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIPrompt;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.validate.UIValidator;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CamelNewComponentsCommand extends AbstractCamelCommand {

    //UIInput field names will automatically be the names for the command line flags

    @Inject
    @WithAttributes(label = "Camel components to add to the project", description = "Select the list of Camel component that you want to add to your project.", required = true)
    UIInput<String> named;

    @Inject
    @WithAttributes(label = "Version", description = "Version.", required = true)
    UIInput<String> version;

    @Inject
    private ClientFactory factory;

    @Inject
    TemplateFactory templateFactory;

    @Inject
    ResourceFactory resourceFactory;


    @Inject
    private DependencyInstaller dependencyInstaller;

    Client client;

    Set<String> availableComponents;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelNewComponentsCommand.class).name(
                "camel-new-component").category(Categories.create(CATEGORY))
                .description("Add new Camel components to the project.");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        initializeAvailableCamelComponents();

        //populate autocompletion options
        named.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {

                // filter
                List<String> filteredList = new ArrayList<String>(availableComponents.size());
                for (String s : availableComponents) {
                    if (!camelDepsInUse.contains(s)) {
                        filteredList.add(s);
                    }
                }

                // rename key
                List<String> noPrefixList = new ArrayList<String>(filteredList.size());
                for (String s : filteredList) {
                    noPrefixList.add(s.split("-")[1]);
                }

                // keep only relevant
                List<String> componentsList = new ArrayList<>(noPrefixList.size());
                for (String s : noPrefixList) {
                    //adding to the proposals only entries relevant to the current partially typed string
                    if (s.startsWith(value))
                        componentsList.add(s);
                }

                //sort
                Collections.sort(componentsList);
                return componentsList;
            }
        });

        version.setCompleter(new UIVersionCompleter(this));

        named.addValidator(new UIValidator() {
            @Override
            public void validate(UIValidationContext context) {
                if (availableComponents == null) {
                    availableComponents = initializeAvailableCamelComponents();
                }

                if (!availableComponents.contains("camel-" + context.getCurrentInputComponent().getValue())) {
                    context.addValidationError(context.getCurrentInputComponent(), "Selected Camel component is not available.");
                }
            }
        });

        builder.add(named).add(version);
    }


    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String component = named.getValue();
        component = "camel-" + component;
        String selectedVersion = version.getValue();

        Project project = getSelectedProject(context);

        installMavenDependency(component, selectedVersion, project);

        CamelSupportedTechnologyEnum tech = null;
        try {
            tech = detectTechnology(project);
        } catch (IllegalStateException e) {
            return Results.fail("Unable to identify project type.");
        }
        //add different samples based on technology
        switch (tech) {
            case BLUEPRINT: {
                ResourcesFacet facet = getSelectedProject(context).getFacet(ResourcesFacet.class);
                String outputFilePath = "OSGI-INF" + File.separator + "blueprint" + File.separator + "components.xml";
                FileResource<?> fileResource = facet.getResource(outputFilePath);
                Resource<URL> xml = resourceFactory.create(getClass().getResource("/templates/my_blueprint.ftl")).reify(URLResource.class);
                Template template = templateFactory.create(xml, FreemarkerTemplate.class);

                Map<String, Object> templateContext = new HashMap<>();
                String componentId = component.split("-")[1];
                templateContext.put("componentId", componentId);
                String componentClass = findComponentFQCN(component, selectedVersion);
                templateContext.put("componentClass", componentClass);

                UIPrompt prompt = context.getPrompt();
                if (!prompt.promptBoolean("File '" + outputFilePath + "' already exists. Do you want to overwrite it?")) {
                    return Results.fail("File '" + outputFilePath + "' already exists.");
                }

                fileResource.createNewFile();
                fileResource.setContents(template.process(templateContext));

                break;
            }
            case SPRING: {
                ResourcesFacet facet = getSelectedProject(context).getFacet(ResourcesFacet.class);
                String outputFilePath = "META-INF" + File.separator + "spring" + File.separator + "components.xml";
                FileResource<?> fileResource = facet.getResource(outputFilePath);
                Resource<URL> xml = resourceFactory.create(getClass().getResource("/templates/my_spring.ftl")).reify(URLResource.class);
                Template template = templateFactory.create(xml, FreemarkerTemplate.class);

                Map<String, Object> templateContext = new HashMap<>();
                String componentId = component.split("-")[1];
                templateContext.put("componentId", componentId);
                String componentClass = findComponentFQCN(component, selectedVersion);
                templateContext.put("componentClass", componentClass);

                UIPrompt prompt = context.getPrompt();
                if (!prompt.promptBoolean("File '" + outputFilePath + "' already exists. Do you want to overwrite it?")) {
                    return Results.fail("File '" + outputFilePath + "' already exists.");
                }

                fileResource.createNewFile();
                fileResource.setContents(template.process(templateContext));
                break;
            }
            case JAVA:
                break;
        }

        return Results.success(component + " component successfully installed!");

    }


    private String findComponentFQCN(String component, String selectedVersion) {
        String result = null;
        InputStream stream = null;
        try {
            File tmp = File.createTempFile("camel-dep", "jar");
            URL url = new URL(String.format("https://repo1.maven.org/maven2/org/apache/camel/%s/%s/%s-%s.jar", component, selectedVersion, component, selectedVersion));

            FileUtils.copyURLToFile(url, tmp);

            ZipFile zipFile = new ZipFile(tmp);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("META-INF/services/org/apache/camel/component/") && !entry.isDirectory()) {
                    stream = zipFile.getInputStream(entry);
                    Properties prop = new Properties();
                    prop.load(stream);
                    result = prop.getProperty("class");
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to inspect added component", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    private void installMavenDependency(String component, String selectedVersion, Project project) {
        Dependency dependency = DependencyBuilder.create().setGroupId(MVN_CAMEL_GROUPID).setVersion(selectedVersion).setArtifactId(component);

        dependencyInstaller.install(project, dependency);
    }

    private synchronized Set<String> initializeAvailableCamelComponents() {
        if (availableComponents == null) {
            String camelComponentsListLocation = "https://raw.githubusercontent.com/fusesource/fuseide/master/core/plugins/org.fusesource.ide.camel.model/components/components-2.13.2.xml";
            Set<String> result = new LinkedHashSet<String>();
            String xmlString = null;

            //xmlString = IOUtils.toString(new URL(camelComponentsListLocation));
            client = factory.createClient();
            try {
                xmlString = client.target(camelComponentsListLocation).request(MediaType.WILDCARD_TYPE).get(String.class);
            } catch (Exception e) {
                throw new RuntimeException("Unable to fetch list of Camel components from: " + camelComponentsListLocation, e);
            }

            XML xml = new XMLDocument(xmlString);
            List<String> connectors = xml.xpath("/connectors/connector/@id");
            for (String id : connectors) {
                result.add("camel-" + id);
            }
            availableComponents = Collections.unmodifiableSet(result);
        }

        return availableComponents;
    }

    private CamelSupportedTechnologyEnum detectTechnology(Project project) {
        if (probeForCDI(project)) {
            return CamelSupportedTechnologyEnum.JAVA;
        } else if (probeForBlueprint(project)) {
            return CamelSupportedTechnologyEnum.BLUEPRINT;
        } else if (probeForSpring(project)) {
            return CamelSupportedTechnologyEnum.SPRING;
        } else {
            throw new IllegalStateException("We couldn't identify Camel Project technology");
        }
    }

    private boolean probeForSpring(Project project) {
        ResourcesFacet facet = project.getFacet(ResourcesFacet.class);
        FileResource<?> resource = facet.getResource("META-INF" + File.separator + "spring");
        if (resource.isDirectory()) {
            return true;
        } else {
            return false;
        }

    }

    private boolean probeForBlueprint(Project project) {
        ResourcesFacet facet = project.getFacet(ResourcesFacet.class);
        FileResource<?> resource = facet.getResource("OSGI-INF");
        if (resource.isDirectory()) {
            return true;
        } else {
            return false;
        }

    }

    private boolean probeForCDI(Project project) {
        final boolean matched[] = new boolean[1];
        matched[0] = false;
        project.getFacet(JavaSourceFacet.class).visitJavaSources(
                new JavaResourceVisitor() {
                    @Override
                    public void visit(VisitContext context, JavaResource javaResource) {
                        try {
                            JavaSource<?> javaType = javaResource.getJavaType();
                            if (javaType.isClass()) {
                                JavaClassSource source = (JavaClassSource) javaType;
                                if (source.hasAnnotation(org.apache.camel.cdi.ContextName.class)
                                        || source.hasAnnotation(org.apache.camel.cdi.Uri.class)
                                        ) {
                                    matched[0] = true;
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
        return matched[0];
    }


}

class UIVersionCompleter implements UICompleter{
    AbstractCamelCommand command;

    public UIVersionCompleter(AbstractCamelCommand command){
        this.command = command;
    }

    @Override
    public Iterable getCompletionProposals(UIContext context, InputComponent input, String value) {
        List<String> result = new ArrayList<>(1);
        result.add(command.getCamelCoreVersion());
        return result;
    }
}