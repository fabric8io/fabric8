package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.Entitlement;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Entitlements extends BaseQuery {
	ConnectionManager connectionManager = null;
	static String url = "/rs/entitlements";

	public Entitlements(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	/**
	 * 
	 * Queries the entitlements RESTful interface with a given set of keywords.
	 * RESTful method: https://api.access.redhat.com/rs/entitlements?keyword=NFS
	 * 
	 * @param activeOnly
	 *            Only return active entitlements
	 * @param product
	 *            Limit results to entitlements for the specified product
	 * @param kwargs
	 *            Additional properties to filter on. The RESTful interface can
	 *            only search on keywords; however, you can use this method to
	 *            post-filter the results returned. Simply supply a string array
	 *            of valid properties and their associated values.
	 * @return A list of entitlements
	 * @throws RequestException
	 *             An exception if there was a connection related issue.
	 * @throws MalformedURLException
	 */
	public List<Entitlement> list(boolean activeOnly, String product,
			String[] kwargs) throws RequestException, MalformedURLException {

		// Strata is inverted in several respects, the option for filtering
		// inactive entitlements being one.
		String showAll = new Boolean(!activeOnly).toString();
		List<String> queryParams = new ArrayList<String>();
		queryParams.add("showAll=" + showAll);
		if (product != null) {
			queryParams.add("product=" + product);
		}
		String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig()
				.getUrl() + url, queryParams);
		com.redhat.gss.redhat_support_lib.parsers.Entitlements entitlements = get(
				connectionManager.getConnection(), fullUrl,
				com.redhat.gss.redhat_support_lib.parsers.Entitlements.class);
		return (List<Entitlement>) FilterHelper.filterResults(
				entitlements.getEntitlement(), kwargs);
	}
}
