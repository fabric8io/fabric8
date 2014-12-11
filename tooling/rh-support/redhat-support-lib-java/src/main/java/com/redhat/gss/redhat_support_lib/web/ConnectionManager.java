package com.redhat.gss.redhat_support_lib.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import com.redhat.gss.redhat_support_lib.errors.FTPException;
import com.redhat.gss.redhat_support_lib.filters.UserAgentFilter;
import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;

public class ConnectionManager {

	private final static Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
	ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder().connectionPoolSize(100);
	ConfigHelper config = null;

	public ConnectionManager(ConfigHelper config) {
		this.config = config;
		clientBuilder.connectionPoolSize(100);
		clientBuilder.connectionTTL(config.getTimeout(), TimeUnit.MILLISECONDS);
		clientBuilder.socketTimeout(config.getTimeout(), TimeUnit.MILLISECONDS);
		CustomHttpEngine httpEngine = new CustomHttpEngine(config);
		clientBuilder.httpEngine(httpEngine);
		if (config.isDevel()) {
			clientBuilder.disableTrustManager();
		} 
		if (config.getProxyUrl() != null) {
			clientBuilder.defaultProxy("10.13.49.98", config.getProxyPort());
		}
	}

	public ResteasyClient getConnection() throws MalformedURLException {
		ResteasyClient client =  clientBuilder.build();
		client.register(new BasicAuthentication(config.getUsername(), config
				.getPassword()));
		client.register(new UserAgentFilter(config.getUserAgent()));
		return client;
	}

	public ConfigHelper getConfig() {
		return config;
	}

	public FTPClient getFTP() throws IOException, FTPException {
		FTPClient ftp = null;
		if (config.getProxyUrl() == null) {
			ftp = new FTPClient();
		} else {
			ftp = new FTPHTTPClient(config.getProxyUrl().getHost(),
					config.getProxyPort(), config.getProxyUser(),
					config.getProxyPassword());
		}
		ftp.connect(config.getFtpHost(), config.getFtpPort());
		if (!ftp.login(config.getFtpUsername(), config.getFtpPassword())) {
			throw new FTPException("Error during FTP login");
		}
		return ftp;
	}
}
