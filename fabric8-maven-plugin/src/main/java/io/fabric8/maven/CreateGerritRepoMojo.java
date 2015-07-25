package io.fabric8.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.gerrit.ProjectInfoDTO;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.gerrit.GitApi;
import io.fabric8.gerrit.CreateRepositoryDTO;
import io.fabric8.gerrit.RepositoryDTO;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Priority;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.*;
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

    private static final String JSON_MAGIC = ")]}'";

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
     * The empty commit to be created 
     */
    @Parameter(property = "empty_commit", required = false, defaultValue = "true")
    private String empty_commit;

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
            String empty_commit = this.empty_commit;

            createGerritRepo(kubernetes, getNamespace(), log, gerritUser, gerritPwd, repoName, description, empty_commit);

        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load environment schemas: " + e, e);
        }
    }

    private static boolean createGerritRepo(KubernetesClient kubernetes, String namespace, Log log, String gerritUser, String gerritPwd, String repoName, String description, String empty_commit) throws MojoExecutionException, JsonProcessingException {

        // lets add defaults if not env vars
        if (Strings.isNullOrBlank(gerritUser)) {
            gerritUser = "admin";
        }
        if (Strings.isNullOrBlank(gerritPwd)) {
            gerritPwd = "secret";
        }

        String gerritAddress = KubernetesHelper.getServiceURL(kubernetes,ServiceNames.GERRIT, namespace, "http", true);
        log.info("Found gerrit address: " + gerritAddress + " for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        if (Strings.isNullOrBlank(gerritAddress)) {
            throw new MojoExecutionException("No address for service " + ServiceNames.GERRIT + " in namespace: "
                    + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        }
        log.info("Querying Gerrit for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());

        List<Object> providers = WebClients.createProviders();
        providers.add(new RemovePrefix());
        WebClient webClient = WebClient.create(gerritAddress, providers);
        disableSslChecks(webClient);
        configureUserAndPassword(webClient, gerritUser, gerritPwd);
        enableDigestAuthenticaionType(webClient);
        GitApi gitApi = JAXRSClientFactory.fromClient(webClient, GitApi.class);
        
        // Check first if a Git repo already exists in Gerrit
        ProjectInfoDTO project = null;
        try {
            project = gitApi.getRepository(repoName);
        } catch (WebApplicationException ex) {
            // If we get Response Status = 404, then no repo exists. So we can create it
            if (ex.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                CreateRepositoryDTO createRepoDTO = new CreateRepositoryDTO();
                createRepoDTO.setDescription(description);
                createRepoDTO.setName(repoName);
                createRepoDTO.setCreate_empty_commit(Boolean.valueOf(empty_commit));

                RepositoryDTO repository = gitApi.createRepository(repoName, createRepoDTO);

                if (log.isDebugEnabled()) {
                    log.debug("Git Repo created : " + toJson(repository));
                }
                log.info("Created git repo for " + repoName + " for namespace: " + namespace + " on gogs URL: " + gerritAddress);

                return true;
            }
        }

        if ((project != null) && (project.getName().equals(repoName))) {
            throw new MojoExecutionException("Repository " + repoName + " already exists !");
        }
        
        return false;
    }

    @Priority(value = 1000)
    protected static class RemovePrefix implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext interceptorContext)
                throws IOException, WebApplicationException {
            InputStream in = interceptorContext.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder received = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                received.append(line);
            }
            
            String s = received.toString();
            s = s.replace(JSON_MAGIC,"");

            System.out.println("Reader Interceptor removing the prefix invoked.");
            System.out.println("Content cleaned : " + s);
            
            String responseContent = new String(s);
            interceptorContext.setInputStream(new ByteArrayInputStream(
                    responseContent.getBytes()));

            return interceptorContext.proceed();
        }
    }

}
