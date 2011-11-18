/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log.elastic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonCreator;

public class LogRequest {
	private long from;
	private long size;
	private List<Map<String,String>> sort;
	private Map query;

	public static LogRequest newInstance(Long maxLogSeq) {
		List<Map<String,String>> s = new ArrayList();
		s.add(keyValueMap("timestamp", "desc"));
		s.add(keyValueMap("seq", "desc"));

		Map q = new HashMap();
		Map constant_score = new HashMap();
		Map filter = new HashMap();

		q.put("constant_score", constant_score);
		constant_score.put("filter", filter);

		List<Map> listOfTerms = new ArrayList();
		//listOfTerms.add(createSearchTerm("host", "root"));
		listOfTerms.add(createSearchTerms("level", "error", "warn", "info"));

		if (maxLogSeq != null) {
			listOfTerms.add(createSearchRangeGt("seq", maxLogSeq));
		}

		filter.put("and", listOfTerms);

		return new LogRequest(0, 50, s, q);
	}

	private static Map<String, String> keyValueMap(String key, String value) {
		Map<String, String> answer = new HashMap<String, String>();
		answer.put(key, value);
		return answer;
	}

	protected static Map createSearchRangeGt(String name, Object value) {
		return createSearchRange(name, "gt", value);
	}

	protected static Map createSearchRange(String name, String compareOperation, Object value) {
		Map answer = new HashMap();
		Map range = new HashMap();
		Map compare = new HashMap();
		answer.put("range", range);
		range.put(compareOperation,  compare);
		compare.put(name, value);
		return answer;
	}

	protected static Map createSearchTerm(String name, String value) {
		Map answer = new HashMap();
		Map term = new HashMap();
		answer.put("term", term);
		term.put(name, value);
		return answer;
	}

	protected static Map createSearchTerms(String name, String... values) {
		Map answer = new HashMap();
		Map term = new HashMap();
		answer.put("terms", term);
		term.put(name, new ArrayList(Arrays.asList(values)));
		return answer;
	}

	@JsonCreator
	public LogRequest(long from, long size, List<Map<String,String>> sort, Map query) {
		this.from = from;
		this.size = size;
		this.sort = sort;
		this.query = query;
	}
	public long getFrom() {
		return from;
	}
	public void setFrom(long from) {
		this.from = from;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public List<Map<String,String>> getSort() {
		return sort;
	}
	public void setSort(List<Map<String,String>> sort) {
		this.sort = sort;
	}

	public Map getQuery() {
		return query;
	}

	public void setQuery(Map query) {
		this.query = query;
	}



}
