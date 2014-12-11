package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.Case;
import com.redhat.gss.redhat_support_lib.parsers.Group;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Groups extends BaseQuery {
	private final static Logger LOGGER = Logger.getLogger(Groups.class
			.getName());
	ConnectionManager connectionManager = null;
	static String url = "/rs/groups/";

	public Groups(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	/**
	 * Queries the API for the given case number. RESTful method:
	 * https://api.access.redhat.com/rs/groups/
	 * 
	 * 
	 * @return A case object that represents the given case number.
	 * @throws RequestException
	 *             An exception if there was a connection related issue.
	 * @throws MalformedURLException
	 */
	public List<Group> list() throws RequestException, MalformedURLException {
		String fullUrl = connectionManager.getConfig().getUrl() + url;
		com.redhat.gss.redhat_support_lib.parsers.Groups groups = get(
				connectionManager.getConnection(), fullUrl,
				com.redhat.gss.redhat_support_lib.parsers.Groups.class);
		return (List<Group>) FilterHelper
				.filterResults(groups.getGroup(), null);
	}
}