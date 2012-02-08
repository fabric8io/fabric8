/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.insight.log.rest;

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
