package org.jboss.fuse.rhaccess.filter;

import io.hawt.system.Authenticator;
import io.hawt.system.ConfigManager;
import io.hawt.system.Helpers;
import io.hawt.system.PrivilegedCallback;
import io.hawt.web.AuthenticationConfiguration;
import io.hawt.web.AuthenticationContainerDiscovery;
import io.hawt.web.AuthenticationFilter;
import io.hawt.web.tomcat.TomcatAuthenticationContainerDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Filter for authentication. If the filter is enabled, then the login screen is shown.
 */
public class SupportAuthenticationFilter implements Filter {

    private static final transient Logger LOG = LoggerFactory.getLogger(SupportAuthenticationFilter.class);

    // JVM system properties
    public static final String HAWTIO_NO_CREDENTIALS_401 = "hawtio.noCredentials401";
    public static final String HAWTIO_AUTHENTICATION_ENABLED = "hawtio.authenticationEnabled";
    public static final String HAWTIO_REALM = "hawtio.realm";
    public static final String HAWTIO_ROLE = "hawtio.role";
    public static final String HAWTIO_ROLES = "hawtio.roles";
    public static final String HAWTIO_ROLE_PRINCIPAL_CLASSES = "hawtio.rolePrincipalClasses";

    private final AuthenticationConfiguration configuration = new AuthenticationConfiguration();

    // add known SPI authentication container discovery
    private final AuthenticationContainerDiscovery[] discoveries = new AuthenticationContainerDiscovery[]{
            new TomcatAuthenticationContainerDiscovery()
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ConfigManager config = (ConfigManager) filterConfig.getServletContext().getAttribute("ConfigManager");

        String defaultRolePrincipalClasses = "";

        if (System.getProperty("karaf.name") != null) {
            defaultRolePrincipalClasses = "org.apache.karaf.jaas.boot.principal.RolePrincipal,org.apache.karaf.jaas.modules.RolePrincipal,org.apache.karaf.jaas.boot.principal.GroupPrincipal";
        }

        if (config != null) {
            configuration.setRealm(config.get("realm", readFromFilterConfig("realm", "karaf", filterConfig)));
            configuration.setRole(config.get("role", readFromFilterConfig("role", "admin", filterConfig)));
            configuration.setRole(config.get("roles", readFromFilterConfig("roles", "admin", filterConfig)));
            configuration.setRolePrincipalClasses(config.get("rolePrincipalClasses", readFromFilterConfig("rolePrincipalClasses", defaultRolePrincipalClasses, filterConfig)));
            configuration.setEnabled(Boolean.parseBoolean(config.get("authenticationEnabled", readFromFilterConfig("authenticationEnabled", "true", filterConfig))));
            configuration.setNoCredentials401(Boolean.parseBoolean(config.get("noCredentials401", readFromFilterConfig("noCredentials401", "false", filterConfig))));
        }

        // JVM system properties can override always
        if (System.getProperty(HAWTIO_AUTHENTICATION_ENABLED) != null) {
            configuration.setEnabled(Boolean.getBoolean(HAWTIO_AUTHENTICATION_ENABLED));
        }
        if (System.getProperty(HAWTIO_NO_CREDENTIALS_401) != null) {
            configuration.setNoCredentials401(Boolean.getBoolean(HAWTIO_NO_CREDENTIALS_401));
        }
        if (System.getProperty(HAWTIO_REALM) != null) {
            configuration.setRealm(System.getProperty(HAWTIO_REALM));
        }
        if (System.getProperty(HAWTIO_ROLE) != null) {
            configuration.setRole(System.getProperty(HAWTIO_ROLE));
        }
        if (System.getProperty(HAWTIO_ROLES) != null) {
            configuration.setRole(System.getProperty(HAWTIO_ROLES));
        }
        if (System.getProperty(HAWTIO_ROLE_PRINCIPAL_CLASSES) != null) {
            configuration.setRolePrincipalClasses(System.getProperty(HAWTIO_ROLE_PRINCIPAL_CLASSES));
        }

        if (configuration.isEnabled()) {
            for (AuthenticationContainerDiscovery discovery : discoveries) {
                if (discovery.canAuthenticate(configuration)) {
                    LOG.info("Discovered container {} to use with hawtio authentication filter", discovery.getContainerName());
                    break;
                }
            }
        }

        if (configuration.isEnabled()) {
            LOG.info("Starting hawtio authentication filter, JAAS realm: \"{}\" authorized role(s): \"{}\" role principal classes: \"{}\"",
                    new Object[]{configuration.getRealm(), configuration.getRole(), configuration.getRolePrincipalClasses()});
        } else {
            LOG.info("Starting hawtio authentication filter, JAAS authentication disabled");
        }
    }

    String readFromFilterConfig(String param, String defaultVal, FilterConfig config) {
        String result = defaultVal;
        if (config != null) {
            String val = config.getInitParameter(param);
            if (val != null && !"".equals(val.trim())) {
                result = val;
            }
        }
        return result;
    }


    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        LOG.debug("Handling request for path {}", path);

        if (configuration.getRealm() == null || configuration.getRealm().equals("") || !configuration.isEnabled()) {
            LOG.debug("No authentication needed for path {}", path);
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Subject subject = (Subject) session.getAttribute("subject");
            if (subject != null) {
                LOG.debug("Session subject {}", subject);
                executeAs(request, response, chain, subject);
                return;
            }
        }

        LOG.debug("Doing authentication and authorization for path {}", path);
        switch (Authenticator.authenticate(configuration.getRealm(), configuration.getRole(), configuration.getRolePrincipalClasses(),
                configuration.getConfiguration(), httpRequest, new PrivilegedCallback() {
                    public void execute(Subject subject) throws Exception {
                        executeAs(request, response, chain, subject);
                    }
                })) {
            case AUTHORIZED:
                // request was executed using the authenticated subject, nothing more to do
                break;
            case NOT_AUTHORIZED:
                Helpers.doForbidden((HttpServletResponse) response);
                break;
            case NO_CREDENTIALS:
                if (configuration.isNoCredentials401()) {
                    // return auth prompt 401
                    Helpers.doAuthPrompt(configuration.getRealm(), (HttpServletResponse)response);
                } else {
                    // return forbidden 403 so the browser login does not popup
                    Helpers.doForbidden((HttpServletResponse) response);
                }
                break;
        }
    }

    private static void executeAs(final ServletRequest request, final ServletResponse response, final FilterChain chain, Subject subject) {
        try {
            Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    chain.doFilter(request, response);
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            LOG.info("Failed to invoke action " + ((HttpServletRequest) request).getPathInfo() + " due to:", e);
        }
    }

    @Override
    public void destroy() {
        LOG.info("Destroying hawtio authentication filter");
    }
}
