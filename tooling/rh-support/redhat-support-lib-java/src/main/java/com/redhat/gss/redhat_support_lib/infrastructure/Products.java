package com.redhat.gss.redhat_support_lib.infrastructure;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.List;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.parsers.Product;
import com.redhat.gss.redhat_support_lib.parsers.Versions;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Products extends BaseQuery {
	ConnectionManager connectionManager = null;

	public Products(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	/**
	 * Queries the products RESTful interface with a given set of keywords.
	 * RESTful method: https://api.access.redhat.com/rs/products
	 * 
	 * @param kwargs
	 *            Properties to filter on. The RESTful interface provides no
	 *            keyword search for products; however, you can use this method
	 *            to post-filter the results returned. Simply supply a list of
	 *            valid properties and their associated values.
	 * @return A list of products
	 * @throws RequestException
	 *             An exception if there was a connection related issue.
	 * @throws MalformedURLException
	 */
	public List<Product> list(String[] kwargs) throws RequestException,
			MalformedURLException {
		String url = "/rs/products/";
		String fullUrl = connectionManager.getConfig().getUrl() + url;
		com.redhat.gss.redhat_support_lib.parsers.Products prods = get(
				connectionManager.getConnection(), fullUrl,
				com.redhat.gss.redhat_support_lib.parsers.Products.class);
		return (List<Product>) FilterHelper.filterResults(prods.getProduct(),
				kwargs);
	}

	/**
	 * @param prodName
	 *            Name of product you would like versions
	 * @return A list of versions
	 * @throws RequestException
	 *             An exception if there was a connection related issue.s
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	public List<String> getVersions(String prodName) throws RequestException,
			MalformedURLException, UnsupportedEncodingException {
		String url = "/rs/products/{prodName}/versions";
		prodName = URLEncoder.encode(prodName, "UTF-8").replace("+", "%20");
		url = url.replace("{prodName}", prodName);
		String fullUrl = connectionManager.getConfig().getUrl() + url;
		Versions vers = get(connectionManager.getConnection(), fullUrl,
				Versions.class);
		return vers.getVersion();
	}
}
