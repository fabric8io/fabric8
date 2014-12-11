package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.Article;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Articles extends BaseQuery {
	private final static Logger LOGGER = Logger.getLogger(API.class.getName());
	ConnectionManager connectionManager = null;
	static String url = "/rs/articles/";

	public Articles(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	/**
	 * Queries the API for the given article ID. RESTful method:
	 * https://api.access.redhat.com/rs/articles/<articleID>
	 * 
	 * @param artID
	 *            The exact articleID you are interested in.
	 * @return A article object that represents the given article ID.
	 * @throws RequestException
	 *             An exception if there was a connection related issue.
	 * @throws MalformedURLException 
	 */
	public Article get(String artID) throws RequestException, MalformedURLException {
		String fullUrl = connectionManager.getConfig().getUrl() + url + artID;
		return get(connectionManager.getConnection(), fullUrl, Article.class);
	}

	/**
	 * Queries the articles RESTful interface with a given set of keywords.
	 * RESTful method: https://api.access.redhat.com/rs/articles?keyword=NFS
	 * 
	 * @param keywords
	 *            A string array of keywords to search on.
	 * @param kwargs
	 *            Additional properties to filter on. The RESTful interface can
	 *            only search on keywords; however, you can use this method to
	 *            post-filter the results returned. Simply supply a string array
	 *            of valid properties and their associated values.
	 * @return A list of article objects
	 * @throws RequestException
	 *             An exception if there was a connection related issue.
	 * @throws MalformedURLException 
	 */

	public List<Article> list(String[] keywords, String[] kwargs)
			throws RequestException, MalformedURLException {
		List<String> queryParams = new ArrayList<String>();
		for (String arg : keywords) {
			queryParams.add("keyword=" + arg);
		}
		String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig().getUrl()
						+ url, queryParams);
		com.redhat.gss.redhat_support_lib.parsers.Articles articles = get(connectionManager.getConnection(), fullUrl,
				com.redhat.gss.redhat_support_lib.parsers.Articles.class);
		return (List<Article>) FilterHelper.filterResults(
				articles.getArticle(), kwargs);
	}

	/**
	 * @param art
	 *            The article to be added
	 * @return The same solution with the ID and view_uri set if successful.
	 * @throws Exception
	 *             An exception if there was a connection related issue
	 */
	public Article add(Article art) throws Exception {
		String fullUrl = connectionManager.getConfig().getUrl() + url;
		Response resp = add(connectionManager.getConnection(), fullUrl, art);
		MultivaluedMap<String, String> headers = resp.getStringHeaders();
		URL url = null;
		try {
			url = new URL(headers.getFirst("view-uri"));
		} catch (MalformedURLException e) {
			LOGGER.debug("Failed : Adding article " + art.getTitle()
					+ " was unsuccessful.");
			throw new Exception();
		}
		String path = url.getPath();
		art.setId(path.substring(path.lastIndexOf('/') + 1, path.length()));
		art.setViewUri(url.toString());
		return art;
	}
}