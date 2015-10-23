package io.fabric8.maven;

import io.fabric8.gerrit.CreateRepositoryDTO;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Strings;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static boolean createGerritRepo(KubernetesClient kubernetes, String namespace, Log log, String gerritUser, String gerritPwd, String repoName, String description, String empty_commit) throws MojoExecutionException, IOException {

        // lets add defaults if not env vars
        if (Strings.isNullOrBlank(gerritUser)) {
            gerritUser = "admin";
        }
        if (Strings.isNullOrBlank(gerritPwd)) {
            gerritPwd = "secret";
        }
        
        // TODO
        // Check boolean & use Log4J

        String gerritAddress = KubernetesHelper.getServiceURL(kubernetes,ServiceNames.GERRIT, namespace, "http", true);
        log.info("Found gerrit address: " + gerritAddress + " for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        if (Strings.isNullOrBlank(gerritAddress)) {
            throw new MojoExecutionException("No address for service " + ServiceNames.GERRIT + " in namespace: "
                    + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        }
        log.info("Querying Gerrit for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());

        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        CloseableHttpClient httpclientPost = HttpClientBuilder.create().build();
        String GERRIT_URL= gerritAddress + "/a/projects/" + repoName;
        HttpGet httpget = new HttpGet(GERRIT_URL);
        System.out.println("Requesting : " + httpget.getURI());

        try {
            //Initial request without credentials returns "HTTP/1.1 401 Unauthorized"
            HttpResponse response = httpclient.execute(httpget);
            System.out.println(response.getStatusLine());

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                // Get current current "WWW-Authenticate" header from response
                // WWW-Authenticate:Digest realm="My Test Realm", qop="auth",
                // nonce="cdcf6cbe6ee17ae0790ed399935997e8", opaque="ae40d7c8ca6a35af15460d352be5e71c"
                Header authHeader = response.getFirstHeader(AUTH.WWW_AUTH);
                System.out.println("authHeader = " + authHeader);

                DigestScheme digestScheme = new DigestScheme();

                //Parse realm, nonce sent by server.
                digestScheme.processChallenge(authHeader);

                UsernamePasswordCredentials creds = new UsernamePasswordCredentials("admin", "secret");
                httpget.addHeader(digestScheme.authenticate(creds, httpget, null));

                HttpPost httpPost = new HttpPost(GERRIT_URL);
                httpPost.addHeader(digestScheme.authenticate(creds, httpPost, null));
                httpPost.addHeader("Content-Type", "application/json");

                CreateRepositoryDTO createRepoDTO = new CreateRepositoryDTO();
                createRepoDTO.setDescription("my cool git repo");
                createRepoDTO.setName(repoName);
                createRepoDTO.setCreate_empty_commit(Boolean.valueOf(empty_commit));

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(createRepoDTO);

                HttpEntity entity = new StringEntity(json);
                httpPost.setEntity(entity);
                
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclientPost.execute(httpPost, responseHandler);
                System.out.println("responseBody : " + responseBody);
            }

        } catch (MalformedChallengeException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            System.out.println("Gerrit Server is not responding");
        } catch (HttpResponseException e) {
            System.out.println("Response from Gerrit Server : " + e.getMessage());
            throw new MojoExecutionException("Repository " + repoName + " already exists !");
        } finally {
            httpclient.close();
            httpclientPost.close();
        }
        return false;
    }
}
