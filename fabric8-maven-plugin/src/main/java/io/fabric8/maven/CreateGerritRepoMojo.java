package io.fabric8.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.repo.git.CreateRepositoryDTO;
import io.fabric8.repo.git.DtoSupport;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.ws.rs.*;
import java.util.List;

import static io.fabric8.utils.cxf.JsonHelper.toJson;
import static io.fabric8.utils.cxf.WebClients.configureUserAndPassword;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;
import static io.fabric8.utils.cxf.WebClients.enableDigestAuthenticaionType;

/**
 * Creates a Gerrit Git repository
 */
@Mojo(name = "create-gitrepo", requiresProject = false)
public class CreateGerritRepoMojo extends AbstractNamespacedMojo {

    /**
     * The gerrit git repo to create
     */
    @Parameter(property = "repo", required = true)
    private String repo;

    /**
     * The description of the git gerrit repo to be create
     */
    @Parameter(property = "description", required = false)
    private String description;

    /**
     * The user name to use in gerrit
     */
    @Parameter(property = "gerritAdminUsername", defaultValue = "${GERRIT_ADMIN_USER}")
    private String gerritAdminUsername;

    /**
     * The password to use in gerrit
     */
    @Parameter(property = "gerritAdminPassword", defaultValue = "${GERRIT_ADMIN_PWD}")
    private String gerritAdminPassword;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            KubernetesClient kubernetes = getKubernetes();
            Log log = getLog();
            String gerritUser = this.gerritAdminUsername;
            String gerritPwd = this.gerritAdminPassword;
            String repoName = this.repo;
            String description = this.description;

            createGerritRepo(kubernetes, log, gerritUser, gerritPwd, repoName, repoName, description);

        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

    private static boolean createGerritRepo(KubernetesClient kubernetes, Log log, String gerritUser, String gerritPwd, String repoName, String repoName1, String description) throws MojoExecutionException, JsonProcessingException {

        // lets add defaults if not env vars
        if (Strings.isNullOrBlank(gerritUser)) {
            gerritUser = "admin";
        }
        if (Strings.isNullOrBlank(gerritPwd)) {
            gerritPwd = "secret";
        }

        String namespace = kubernetes.getNamespace();
        // TODO Change gerrit service name after redeploying project on OS as it is still using gerrit-http name
        // String gerritAddress = kubernetes.getServiceURL(ServiceNames.GERRIT, namespace, "http", true);
        String gerritAddress = kubernetes.getServiceURL("gerrit-http", namespace, "http", true);
        log.info("Found gerrit address: " + gerritAddress + " for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getAddress());
        if (Strings.isNullOrBlank(gerritAddress)) {
            throw new MojoExecutionException("No address for service " + ServiceNames.GERRIT + " in namespace: "
                    + namespace + " on Kubernetes address: " + kubernetes.getAddress());
        }
        log.info("Querying Gerrit for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getAddress());

        List<Object> providers = WebClients.createProviders();
        WebClient webClient = WebClient.create(gerritAddress, providers);
        disableSslChecks(webClient);
        configureUserAndPassword(webClient, gerritUser, gerritPwd);
        enableDigestAuthenticaionType(webClient);
        GitApi gitApi = JAXRSClientFactory.fromClient(webClient, GitApi.class);

        CreateRepositoryDTO createRepoDTO = new CreateRepositoryDTO();
        createRepoDTO.setDescription(description);
        createRepoDTO.setName(repoName);

        RepositoryDTO repository = gitApi.createRepository(repoName, createRepoDTO);

        if (log.isDebugEnabled()) {
            log.debug("Got created web hook: " + toJson(repository));
        }
        log.info("Created git repo for " + repoName + " for namespace: " + namespace + " on gogs URL: " + gerritAddress);

        return true;
    }
    
    @Path("a")
    @Produces("application/json")
    @Consumes("application/json")
    protected interface GitApi {

        @POST
        @Path("projects/{repo}")
        public RepositoryDTO createRepository(@PathParam("repo") String repo, CreateRepositoryDTO dto);
    }
    
    protected class RepositoryDTO extends DtoSupport {
        private String name;
        private String description;
        private String id;
        private String parent;
        private String state;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

}
