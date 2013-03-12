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

package org.fusesource.insight.kibana;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.fusesource.insight.elasticsearch.ElasticRest;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KibanaServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(KibanaServlet.class);

    private static ServiceTracker<ElasticRest, ElasticRest> tracker;

    private static final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public static class Config {

        /**
         * Read/open timeouts for the connection to the ES backend
         */
        public static long elasticSearchTimeout = 500;

        /**
         * Use this interval as fallback.
         */
        public static long fallbackInterval = TimeUnit.SECONDS.toMillis(900);

        public static String[] defaultFields  = new String[] { "level", "message" };

        public static String filter = "";

        public static String defaultOperator = "OR";

        public static String primaryField = "_all";

        public static String highlightedField = "message";

        public static int perPage = 50;

        public static boolean highlightResults = true;

        public static boolean smartIndex = true;

        /**
         * You can define your custom pattern here for index names if you
         * use something other than daily indexing. Pattern needs to have
         * date formatting like '%Y.%m.%d'.  Will accept a comma separated
         * list of smart indexes.
         */
        public static String smartIndexPattern = "'insight-'yyyy.MM.dd";

        /**
         * Number of seconds between each index. 86400 = 1 day.
         */
        public static long smartIndexStep = TimeUnit.DAYS.toMillis(1);

        public static int smartIndexLimit = 150;

        public static String defaultIndex = "_all";

        public static String type = "log";

        public static boolean clickableUrls = true;

        public static String timeFormat = "mm/dd HH:MM:ss";

        public static String timeZone = "user";
        public static int analyzeLimit = 2000;
        public static int analyzeShow = 25;
        public static int facetIndexLimit = 0;

        public static String timestamp = "timestamp";
    }

    @Override
    public void init() throws ServletException {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        this.tracker = new ServiceTracker<ElasticRest, ElasticRest>(context, ElasticRest.class, null);
        this.tracker.open();
    }

    @Override
    public void destroy() {
        try {
            this.tracker.close();
        } catch (IllegalStateException e) {
            // Context is certainly already destroyed as we don' use any activator
            // The destroy() method is usually called from the web extender when destroying the
            // servlet, reacting to the http service being unget.
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String path = req.getServletPath();
        String info = req.getPathInfo();
        String request = path + (info != null ? info : "");

        Pattern search;
        Matcher matcher;

        search = Pattern.compile("/js/timezone\\.js");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            LOGGER.debug("Timezone Javascript");
            LOGGER.debug("\trequest: {}", request);

            ArrayNode fields = Json.arrayNode();
            for (String f : Config.defaultFields) {
                fields.add(f);
            }
            String str = "var tmp_offset = \"" +
                    ("user".equals(Config.timeZone) ? "user" : TimeZone.getTimeZone(Config.timeZone).getRawOffset() / 1000)  + "\"\n" +
                    "\n" +
                    "window.time_format = \"" + (Config.timeFormat != null ? Config.timeFormat : "mm/dd HH:MM:ss") + "\"\n" +
                    "\n" +
                    "if (tmp_offset == 'user') {\n" +
                    "\tvar d = new Date()\n" +
                    "\twindow.tOffset = -d.getTimezoneOffset() * 60 * 1000;\n" +
                    "} else {\n" +
                    "\twindow.tOffset = parseFloat(tmp_offset) * 3600 * 1000;\n" +
                    "}\n" +
                    "\n" +
                    "window.default_fields = " + fields.toString() + ";\n" +
                    "window.timestamp = \"" + Config.timestamp + "\";\n";

            resp.setHeader("Content-Type", "application/json");
            resp.getWriter().write(str);
            return;
        }

        search = Pattern.compile("/stream");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            LOGGER.debug("Stream");
            LOGGER.debug("\trequest: {}", request);

            req.getRequestDispatcher("/stream.html").forward(req, resp);
            return;
        }

        search = Pattern.compile("/api/search/([^/?]+)(?:/([^/?]+))?");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            LOGGER.debug("Search");
            LOGGER.debug("\trequest: {}", request);

            String hash = matcher.group(1);
            String segment = matcher.group(2);

            String result = search(hash, segment);
            resp.getWriter().write(result);
            return;
        }

        search = Pattern.compile("/api/graph/([^/?]+)/([^/?]+)/([^/?]+)/([^/?]*)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            LOGGER.debug("Graph");
            LOGGER.debug("\trequest: {}", request);

            String mode = matcher.group(1);
            String interval = matcher.group(2);
            String hash = matcher.group(3);
            String segment = matcher.group(4);

            String result = graph(mode, interval, hash, segment);
            resp.getWriter().write(result);
            return;
        }

        search = Pattern.compile("/api/analyze/([^/?]+)/([^/?]+)/([^/?]+)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String field = matcher.group(1);
            String mode = matcher.group(2);
            String hash = matcher.group(3);

            LOGGER.debug("Analyze {}", mode);
            LOGGER.debug("\trequest: {}", request);

            String result;
            if ("score".equals(mode)) {
                result = analyzeScore(field, hash);
            } else if ("mean".equals(mode)) {
                result = analyzeMean(field, hash);
            } else if ("terms".equals(mode)) {
                result = analyzeTerms(field, hash);
            } else if ("trend".equals(mode)) {
                result = analyzeTrend(field, hash);
            } else {
                resp.sendError(404);
                return;
            }
            resp.getWriter().write(result);
            return;
        }

        search = Pattern.compile("/api/id/([^/?]+)/([^/?]+)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String id = matcher.group(1);
            String index = matcher.group(2);

            LOGGER.debug("Id");
            LOGGER.debug("\trequest: {}", request);

            ObjectNode result = Kelastic.kelastic(new IDQuery(id), index);
            resp.getWriter().write(result.toString());
            return;
        }

        search = Pattern.compile("/api/stream/([^/?]+)(?:/([^/?]+))?");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String hash = matcher.group(1);
            String from = matcher.group(2);

            LOGGER.debug("Stream");
            LOGGER.debug("\trequest: {}", request);

            ObjectNode result = stream(hash, from);
            resp.getWriter().write(result.toString());
            return;
        }

        search = Pattern.compile("/rss/([^/?]+)/([^/?]+)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String hash = matcher.group(1);
            String count = matcher.group(2);

            LOGGER.debug("Rss");
            LOGGER.debug("\trequest: {}", request);

            // TODO: create rss
            resp.sendError(404);
            return;
        }

        search = Pattern.compile("/export/([^/?]+)/([^/?]+)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String hash = matcher.group(1);
            String count = matcher.group(2);

            LOGGER.debug("Export");
            LOGGER.debug("\trequest: {}", request);

            // TODO: implement
            resp.sendError(404);
            return;
        }

        search = Pattern.compile("/turl/([^/?]+)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String id = matcher.group(1);

            LOGGER.debug("Turl id");
            LOGGER.debug("\trequest: {}", request);

            // TODO: implement
            resp.sendError(404);
            return;
        }

        search = Pattern.compile("/turl/save/([^/?]+)");
        matcher = search.matcher(request);
        if (matcher.matches()) {
            String hash = matcher.group(1);

            LOGGER.debug("Turl save");
            LOGGER.debug("\trequest: {}", request);

            // TODO: implement
            resp.sendError(404);
            return;
        }

        LOGGER.debug("Unknown request {}", request);
        resp.sendError(404);
    }

    private ObjectNode stream(String hash, String from) throws IOException {
        // This is delayed by 10 seconds to account for indexing time and a small time
        // difference between us and the ES server.
        int delay = 10;

        // Calculate 'from'  and 'to' based on last event in stream.
        long fromDate;
        if (from == null) {
            fromDate = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10 + delay);
        } else {
            fromDate = parseDate(from);
        }

        // ES's range filter is inclusive. delay-1 should give us the correct window.
        // Maybe?
        long toDate = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(delay);

        // Build and execute
        ClientRequest reqc     = new ClientRequest(hash);
        Query query   = new SortedQuery(reqc.getSearch(), fromDate, toDate, 0, 30);
        List<String> indices = Kelastic.getIndices(fromDate, toDate);
        return kelasticMulti(query, indices);
    }

    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[4096];
            int l;
            while ((l = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, l);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    private String analyzeTrend(String field, String hash) throws IOException {
        int limit  = Config.analyzeLimit;
        int show  = Config.analyzeShow;
        ClientRequest req = new ClientRequest(hash);

        LOGGER.debug("\tclient: {}", req);

        Query queryEnd = new SortedQuery(req.getSearch(), req.getFrom(), req.getTo(), 0, limit, Config.timestamp, "desc");
        List<String> indicesEnd = Kelastic.getIndices(req.getFrom(), req.getTo());
        ObjectNode resultEnd = kelasticMulti(queryEnd, indicesEnd);
        if (resultEnd.get("hits").get("hits").size() < limit) {
            limit = resultEnd.get("hits").get("hits").size() / 2;
            queryEnd = new SortedQuery(req.getSearch(), req.getFrom(), req.getTo(), 0, limit, Config.timestamp, "desc");
            resultEnd = kelasticMulti(queryEnd, indicesEnd);
        }
        Map<String, Integer> countEnd = KelasticResponse.countFields(resultEnd, field);

        Query queryBegin = new SortedQuery(req.getSearch(), req.getFrom(), req.getTo(), 0, limit, Config.timestamp, "asc");
        List<String> indicesBegin = Kelastic.getIndices(req.getFrom(), req.getTo());
        Collections.reverse(indicesBegin);
        ObjectNode resultBegin = kelasticMulti(queryBegin, indicesBegin);
        Map<String, Integer> countBegin = KelasticResponse.countFields(resultBegin, field);

        ObjectNode time = ((ObjectNode) resultEnd.get("kibana")).putObject("time");
        time.put("from", formatDate(new Date(req.getFrom())));
        time.put("to", formatDate(new Date(req.getTo())));

        List<ObjectNode> trends = new ArrayList<ObjectNode>();
        int count = resultEnd.get("hits").get("hits").size();
        for (Map.Entry<String, Integer> entry : countEnd.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            int first = countBegin.containsKey(key) ? countBegin.get(key) : 0;
            ObjectNode n = resultEnd.objectNode();
            n.put("id", key);
            n.put("count", value);
            n.put("first", first);
            n.put("trend", (((float) value) / count - ((float) first) / count) * 100.0f);
            trends.add(n);
        }
        Collections.sort(trends, new Comparator<ObjectNode>() {
            @Override
            public int compare(ObjectNode o1, ObjectNode o2) {
                Double t1 = Math.abs(o1.get("trend").asDouble());
                Double t2 = Math.abs(o2.get("trend").asDouble());
                return t2.compareTo(t1);
            }
        });
        ArrayNode hits = resultEnd.arrayNode();
        for (ObjectNode n : trends) {
            hits.add(n);
        }

        resultEnd.with("hits").put("count", resultEnd.get("hits").get("hits").size());
        resultEnd.with("hits").put("hits", hits);

        return Json.serialize(resultEnd);
    }

    private String analyzeTerms(String field, String hash) throws IOException {
        int limit  = Config.analyzeShow;
        ClientRequest req = new ClientRequest(hash);
        Query query = new TermsFacet(req.getSearch(), req.getFrom(), req.getTo(), field.split(","), limit);

        LOGGER.debug("\tclient: {}", req);
        LOGGER.debug("\tquery: {}", query);

        List<String> indices = Kelastic.getIndices(req.getFrom(), req.getTo(), Config.facetIndexLimit);

        ObjectNode result = kelasticMultiFlat(query, indices);

        ObjectNode time = ((ObjectNode) result.get("kibana")).putObject("time");
        time.put("from", formatDate(new Date(req.getFrom())));
        time.put("to", formatDate(new Date(req.getTo())));


        return Json.serialize(result);
    }

    private String analyzeMean(String field, String hash) throws IOException {
        ClientRequest req = new ClientRequest(hash);
        Query query = new StatsFacet(req.getSearch(), req.getFrom(), req.getTo(), field);

        LOGGER.debug("\tclient: {}", req);
        LOGGER.debug("\tquery: {}", query);

        List<String> indices = Kelastic.getIndices(req.getFrom(), req.getTo(), Config.facetIndexLimit);

        String type = Kelastic.getFieldType(indices.get(0), field);
        if ("long".equals(type) || "integer".equals(type) || "double".equals(type) || "float".equals(type)) {
            ObjectNode result = kelasticMultiFlat(query, indices);
            ObjectNode time = result.with("kibana").putObject("time");
            time.put("from", formatDate(new Date(req.getFrom())));
            time.put("to", formatDate(new Date(req.getTo())));
            String str = Json.serialize(result);
            return str;
        } else {
            ObjectNode node = Json.objectNode();
            node.put("error", "Statistics not supported for type: " + type);
            return Json.serialize(node);
        }
    }

    private String analyzeScore(String field, String hash) throws IOException {
        int limit = Config.analyzeLimit;
        int show = Config.analyzeShow;
        ClientRequest req = new ClientRequest(hash);
        Query query = new SortedQuery(req.getSearch(), req.getFrom(), req.getTo(), 0, limit);

        LOGGER.debug("\tclient: {}", req);
        LOGGER.debug("\tquery: {}", query);

        List<String> indices = Kelastic.getIndices(req.getFrom(), req.getTo());

        ObjectNode result = kelasticMulti(query, indices);

        ObjectNode time = result.with("kibana").putObject("time");
        time.put("from", formatDate(new Date(req.getFrom())));
        time.put("to", formatDate(new Date(req.getTo())));

        Map<String, Integer> count = KelasticResponse.countFields(result, field, show);
        ArrayNode hits = result.arrayNode();
        for (Map.Entry<String, Integer> entry : count.entrySet()) {
            ObjectNode n = hits.objectNode();
            n.put("id", entry.getKey());
            n.put("count", entry.getValue().intValue());
            hits.add(n);
        }
        result.with("hits").put("count", result.get("hits").get("hits").size());
        result.with("hits").put("hits", hits);

        return Json.serialize(result);
    }

    private String graph(String mode, String interval, String hash, String segment) throws IOException {
        LOGGER.debug("\tmode: {}", mode);
        LOGGER.debug("\tinterval: {}", interval);
        LOGGER.debug("\thash: {}", hash);
        LOGGER.debug("\tsegment: {}", segment);

        // TODO: segment
        int seg = segment.isEmpty() ? 0 : Integer.parseInt(segment);
        ClientRequest req = new ClientRequest(hash);
        Query query;
        if ("count".equals(mode)) {
            query = new DateHistogram(req.getSearch(), req.getFrom(), req.getTo(), interval);
        } else if ("mean".equals(mode)) {
            query = new StatsHistogram(req.getSearch(), req.getFrom(), req.getTo(), req.getAnalyze(), interval);
        } else {
            throw new IllegalArgumentException("unsupported mode " + mode);
        }

        LOGGER.debug("\tclient: {}", req);
        LOGGER.debug("\tquery: {}", query);

        List<String> indices = Kelastic.getIndices(req.getFrom(), req.getTo());
        ObjectNode result = kelasticSegment(query, indices, seg);

        return Json.serialize(result);
    }

    private String search(String hash, String segment) throws IOException {
        LOGGER.debug("\thash: {}", hash);
        LOGGER.debug("\tsegment: {}", segment);

        // TODO: segment
        ClientRequest req = new ClientRequest(hash);
        Query query = Config.highlightResults
                ? new HighlightedQuery(req.getSearch(), req.getFrom(), req.getTo(), req.getOffset())
                : new SortedQuery(req.getSearch(), req.getFrom(), req.getTo(), req.getOffset());

        LOGGER.debug("\tclient: {}", req);
        LOGGER.debug("\tquery: {}", query);


        Collection<String> indices = Kelastic.getIndices(req.getFrom(), req.getTo());
        LOGGER.debug("\tindices: {}", indices);

        ObjectNode result = kelasticMulti(query, indices);
        ObjectNode time = result.with("kibana").putObject("time");
        time.put("from", formatDate(new Date(req.getFrom())));
        time.put("to", formatDate(new Date(req.getTo())));
        ArrayNode fields = result.with("kibana").putArray("default_fields");
        for (String s : Config.defaultFields) {
            fields.add(s);
        }
        result.with("kibana").put("clickable_urls", Config.clickableUrls);

        return Json.serialize(result);
    }

    private static String formatDate(Date date) {
        return isoFormat.format(date);
    }

    public static class Json {

        public static String serialize(JsonNode node) {
            try {
                ObjectMapper om = new ObjectMapper();
                StringWriter sw = new StringWriter();
                om.writeTree(om.getJsonFactory().createJsonGenerator(sw), node);
                return sw.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static ObjectNode objectNode() {
            return JsonNodeFactory.instance.objectNode();
        }

        public static ArrayNode arrayNode() {
            return JsonNodeFactory.instance.arrayNode();
        }
    }

    public static class ElasticSearch {

        public static ObjectNode getJson(String request) {
            try {
                ElasticRest rest = tracker.getService();
                if (rest != null) {
                    String response = rest.get(request);
                    return (ObjectNode) new ObjectMapper().readTree(response);
                } else {
                    throw new RuntimeException("ElasticSearch not available");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            /*
            InputStream is = null;
            try {
                URL url = new URL("http://" + Config.elasticSearch + request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // TODO: timeout
                conn.setDoOutput(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "close");
                conn.setRequestProperty("Accept", "application/json");
                is = conn.getInputStream();
                JsonNode node = new ObjectMapper().readTree(is);
                return (ObjectNode) node;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
            */
        }

        public static ObjectNode postJson(String request, String query) {
            try {
                ElasticRest rest = tracker.getService();
                if (rest != null) {
                    String response = rest.post(request, query);
                    return (ObjectNode) new ObjectMapper().readTree(response);
                } else {
                    throw new RuntimeException("ElasticSearch not available");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            /*
            InputStream is = null;
            OutputStream os = null;
            try {
                URL url = new URL(request);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // TODO: timeout
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "close");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.connect();
                os = conn.getOutputStream();
                os.write(query.getBytes());
                os.close();
                is = conn.getInputStream();
                return (ObjectNode) new ObjectMapper().readTree(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
            */
        }

    }

    public static class Kelastic {

        public static ObjectNode kelastic(Query query, String index) {
            ObjectNode response =  run("/" + index + "/_search", query);
            response.with("kibana").put("index", index);
            return response;
        }

        public static ObjectNode run(String url, Query query) {
            ObjectNode response = ElasticSearch.postJson(url, query.toString());
            ObjectNode kibana = response.putObject("kibana");
            kibana.put("per_page", Config.perPage);
            // TODO: handle errors
            if (false) {
                kibana.put("error", "Invalid query");
            }
            return response;
        }

        public static ObjectNode error(String message) {
            ObjectNode node = Json.objectNode();
            node.putObject("kibana").put("error", message);
            return node;
        }

        public static Collection<String> getAllIndices() {
            JsonNode node = ElasticSearch.getJson("/_aliases");
            Set<String> indices = new TreeSet<String>();
            for (Iterator<Map.Entry<String, JsonNode>> entries = node.getFields(); entries.hasNext();) {
                Map.Entry<String, JsonNode> entry = entries.next();
                indices.add(entry.getKey());
                // TODO: Add aliases
            }
            return indices;
        }

        public static List<String> getIndices(long from, long to) {
            return getIndices(from, to, -1);
        }

        public static List<String> getIndices(long from, long to, int limit) {
            if (Config.smartIndex) {
                String pattern = Config.smartIndexPattern;
                if (pattern == null) {
                    pattern = "'logstash-'yyyy.MM.dd";
                }
                Set<String> requested = new TreeSet<String>();
                for (String p : pattern.split(",")) {
                    SimpleDateFormat fmt = new SimpleDateFormat(p.trim());
                    long stepTime = from;
                    do {
                        requested.add(fmt.format(new Date(stepTime)));
                    } while ((stepTime += Config.smartIndexStep) <= to);
                    requested.add(fmt.format(new Date(to)));
                }
                requested.retainAll(getAllIndices());
                if (requested.size() <= Config.smartIndexLimit) {
                    List<String> l = new ArrayList<String>(requested);
                    Collections.reverse(l);
                    if (limit > 0 && l.size() > limit) {
                        l = l.subList(0, limit);
                    }
                    return l;
                } else {
                    return Collections.singletonList(Config.defaultIndex);
                }
            } else {
                return Collections.singletonList(Config.defaultIndex);
            }
        }

        public static String getFieldType(String index, String field) {
            ObjectNode node = ElasticSearch.getJson("/" + index + "/_mapping");
            JsonNode in = node.get(index);
            for  (JsonNode tn : in) {
                JsonNode n = tn.path("properties").path(field).get("type");
                if (n != null) {
                    return n.asText();
                }
            }
            return "";
        }
    }

    public static class KelasticResponse {

        public static Map<String, Integer> countFields(ObjectNode result, String field) {
            return countFields(result, field, 0);
        }

        public static Map<String, Integer> countFields(ObjectNode result, String field, int limit) {
            Map<String, Integer> count = new HashMap<String, Integer>();
            for (String v : collectFieldValues(result, field)) {
                Integer c = count.get(v);
                c = (c != null ? c : 0) + 1;
                count.put(v, c);
            }
            return sortByValue(count, limit);
        }

        public static List<String> collectFieldValues(ObjectNode result, String field) {
            List<String> hits = new ArrayList<String>();
            for (JsonNode hit : result.get("hits").get("hits")) {
                JsonNode fv = hit.get("_source").findValue(field);
                if (fv.isArray()) {
                    for (JsonNode v : fv) {
                        hits.add(v.asText());
                    }
                } else {
                    hits.add(fv.asText());
                }
            }
            return hits;
        }

        public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map, int limit ) {
            List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>( map.entrySet() );
            Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
                public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 ) {
                    return (o1.getValue()).compareTo( o2.getValue() );
                }
            });
            Map<K, V> result = new LinkedHashMap<K, V>();
            for (Map.Entry<K, V> entry : list) {
                result.put( entry.getKey(), entry.getValue() );
                if (limit > 0 && result.size() >= limit) {
                    break;
                }
            }
            return result;
        }
    }

    public static ObjectNode kelasticMulti(Query query, Collection<String> indices) {
            if (indices.isEmpty()) {
                return Kelastic.error("no index");
            }

            Iterator<String> indexIterator = indices.iterator();

            String index = indexIterator.next();
            String url = "/" + index + (Config.type.isEmpty() ? "" : "/" + Config.type) + "/_search";
            ObjectNode response = Kelastic.run(url, query);

//            System.err.println("Query: " + Json.serialize(query.getQuery()));
//            System.err.println("Response: " + Json.serialize(response));

            if (response.get("kibana").get("error") != null) {
                return response;
            }

            // Store the original values for reference
            long target = query.getQuery().get("size").asLong();
            long offset = query.getQuery().get("from").asLong();

            // Didn't get enough hits, and still have indices left?
            while (response.get("hits").get("hits").size() < target && indexIterator.hasNext()) {
                // Subtract from size however many hits we already have
                query.getQuery().put("size", target - response.get("hits").get("hits").size());

                // Calculate an offset to account for anything that might have been shown
                // on the previous page, otherwise, set to 0
                query.getQuery().put("from", (offset - response.get("hits").get("total").asLong() < 0) ?
                        0 : (offset - response.get("hits").get("total").asLong()));

                index = indexIterator.next();
                url = "/" + index + (Config.type.isEmpty() ? "" : "/" + Config.type) + "/_search";
                ObjectNode segment = Kelastic.run(url, query);
                if (!segment.has("status") && segment.has("hits")) {
                    // Concatonate the hits array
                    ((ArrayNode) response.get("hits").get("hits")).addAll((ArrayNode) segment.get("hits").get("hits"));

                    // Add the total hits together
                    response.with("hits").put("total", response.get("hits").get("total").asLong() + segment.get("hits").get("total").asLong());
                } else if (!segment.has("status") || segment.get("status").asInt() != 404) {
                    throw new RuntimeException("Bad response for query to: " + url + ", query: " + query + " response" +
                            " data: " + response);
                }

            }
            ArrayNode indexNode = response.with("kibana").putArray("index");
            for (String i : indices) {
                indexNode.add(i);
            }

        return response;

    }

    public static ObjectNode kelasticSegment(Query query, List<String> indices, int segment) {
            if (indices.isEmpty()) {
                return Kelastic.error("no index");
            }

            String index = indices.get(segment);
            String url = "/" + index + (Config.type.isEmpty() ? "" : "/" + Config.type) + "/_search";
            ObjectNode response = Kelastic.run(url, query);

            ArrayNode indexNode = response.with("kibana").putArray("index");
            for (String i : indices) {
                indexNode.add(i);
            }
            if (segment < indices.size() - 1) {
                response.with("kibana").put("next", segment + 1);
            }
            return response;

    }

    public static ObjectNode kelasticMultiFlat(Query query, List<String> indices) {
            if (indices.isEmpty()) {
                return Kelastic.error("no index");
            }

            StringBuilder sb = new StringBuilder();
            for (String index : indices) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(index);
            }
            String index = sb.toString();
            String url = "/" + index + (Config.type.isEmpty() ? "" : "/" + Config.type) + "/_search";
            ObjectNode response = Kelastic.run(url, query);

            response.with("kibana").put("index", index);
        return response;
    }

    private static long parseDate(String date) {
        try {
            return isoFormat.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    public static class ClientRequest {
        String search;
        String index;
        int offset;
        String[] fields;
        String analyze;
        long from;
        long to;

        public ClientRequest(String hash) throws IOException {
            byte[] str = javax.xml.bind.DatatypeConverter.parseBase64Binary(hash);
            JsonNode request = new ObjectMapper().readTree(str);
            search = getField(request, "search");
            if (search != null && search.contains("|")) {
                search = search.trim().split("|")[0].trim();
            }
            index = getField(request, "index");
            offset = getInt(request, "offset", 0);

            JsonNode f = request.get("fields");
            if (f != null && f.size() > 0) {
                fields = new String[f.size()];
                int i = 0;
                for (Iterator<JsonNode> it = f.getElements(); it.hasNext();) {
                    fields[i++] = it.next().asText();
                }
            } else {
                fields = Config.defaultFields;
            }

            analyze = getField(request, "analyze_field");

            String timeframe = getField(request, "timeframe");
            if ("custom".equals(timeframe)) {
                from = parseDate(request.get("time").get("from").asText());
                to = parseDate(request.get("time").get("to").asText());
            } else if ("all".equals(timeframe)) {
                from = 0;
                to = System.currentTimeMillis();
            } else {
                long diff = timeframe != null ? Integer.parseInt(timeframe) * 1000 : 0;
                if (diff <= 0) {
                    diff = Config.fallbackInterval;
                }
                to = System.currentTimeMillis();
                from = to - diff;
            }
        }

        public String getSearch() {
            return search;
        }

        public String getIndex() {
            return index;
        }

        public int getOffset() {
            return offset;
        }

        public String[] getFields() {
            return fields;
        }

        public String getAnalyze() {
            return analyze;
        }

        public long getFrom() {
            return from;
        }

        public long getTo() {
            return to;
        }

        public String toString() {
            return "{ " +
                    "search: " + search + ", " +
                    "from: " + from + ", " +
                    "to: " + to + ", " +
                    "offset: " + offset + ", " +
                    "fields: " + fields + ", " +
                    "analyze: " + analyze + ", " +
                    "}";
        }

        private String getField(JsonNode request, String name) {
            JsonNode n = request.get(name);
            return n != null ? n.asText() : null;
        }

        private int getInt(JsonNode request, String name, int def) {
            JsonNode n = request.get(name);
            return n != null ? Integer.parseInt(n.asText()) : def;
        }
    }

    public static class Query {

        ObjectNode query;

        public Query(String question) {
            query = Json.objectNode();
            query.put("size", 0);
            ObjectNode filtered = query.putObject("query").putObject("filtered");
            // Query
            ObjectNode questionNode = filtered.putObject("query");
            question = (question == null || question.isEmpty()) ? "*" : question;
            question = Config.filter.isEmpty() ? question : "(#{question}) AND " + Config.filter;
            if ("*".equals(question)) {
                questionNode.putObject("match_all");
            } else {
                ObjectNode n = questionNode.putObject("query_string");
                n.put("default_operator", Config.defaultOperator);
                n.put("default_field", Config.primaryField);
                n.put("query", question);
            }
        }

        public Query(String question, long from, long to) {
            this(question);
            // Filter
            ObjectNode timestampNode = query.with("query").with("filtered").putObject("filter").putObject("range").putObject(Config.timestamp);
            timestampNode.put("from", formatDate(new Date(from)));
            timestampNode.put("to", formatDate(new Date(to)));
        }

        public ObjectNode getQuery() {
            return query;
        }

        public String toString() {
            return Json.serialize(query);
        }
    }

    public static class SortedQuery extends Query {

        public SortedQuery(String question, long from, long to, int offset) {
            this(question,from, to, offset, Config.perPage, Config.timestamp, "desc");
        }

        public SortedQuery(String question, long from, long to, int offset, int size) {
            this(question,from, to, offset, size, Config.timestamp, "desc");
        }

        public SortedQuery(String question, long from, long to, int offset, int size, String field, String order) {
            super(question, from, to);
            query.put("from", offset);
            query.put("size", size);
            query.putObject("sort").putObject(field).put("order", order);
        }

    }

    public static class HighlightedQuery extends SortedQuery {

        public HighlightedQuery(String question, long from, long to, int offset) {
            this(question,from, to, offset, Config.perPage, Config.timestamp, "desc");
        }

        public HighlightedQuery(String question, long from, long to, int offset, int size, String field, String order) {
            super(question, from, to, offset, size, field, order);
            ObjectNode highlight = query.putObject("highlight");
            highlight.putArray("pre_tags").add("@KIBANA_HIGHLIGHT_START@");
            highlight.putArray("post_tags").add("@KIBANA_HIGHLIGHT_END@");
            highlight.putObject("fields").putObject(Config.highlightedField).put("fragment_size", 9999);
        }

    }

    public static class DateHistogram extends Query {
        public DateHistogram(String question, long from, long to, String interval) {
            this(question, from, to, interval, Config.timestamp);
        }

        public DateHistogram(String question, long from, long to, String interval, String field) {
            super(question, from, to);
            ObjectNode histo = query.putObject("facets").putObject("count").putObject("date_histogram");
            histo.put("field", field);
            histo.put("interval", interval);
        }
    }

    public static class StatsHistogram extends Query {
        public StatsHistogram(String question, long from, long to, String field, String interval) {
            this(question, from, to, field, interval, Config.timestamp);
        }

        public StatsHistogram(String question, long from, long to, String field, String interval, String key_field) {
            super(question, from, to);
            ObjectNode histo = query.putObject("facets").putObject("mean").putObject("date_histogram");
            histo.put("value_field", field);
            histo.put("key_field", key_field);
            histo.put("interval", interval);
        }
    }

    public static class StatsFacet extends Query {
        public StatsFacet(String question, long from, long to, String field) {
            super(question, from, to);
            ObjectNode histo = query.putObject("facets").putObject("stats").putObject("statistical");
            histo.put("field", field);
        }
    }

    public static class TermsFacet extends Query {
        public TermsFacet(String question, long from, long to, String[] fields, int limit) {
            super(question, from, to);
            if (fields.length > 1) {
                StringBuilder script = new StringBuilder();
                for (String f : fields) {
                    if (script.length() > 0) {
                        script.append("+'||'+");
                    }
                    script.append("(doc['").append(f).append("'].value !=null ? doc['").append(f).append("'].value : '')");
                }
                ObjectNode terms = query.putObject("facets").putObject("terms").putObject("terms");
                terms.put("script", script.toString());
                terms.put("size", limit);
            } else {
                ObjectNode terms = query.putObject("facets").putObject("terms").putObject("terms");
                terms.put("field", fields[0]);
                terms.put("size", limit);
            }
        }
    }

    public static class IDQuery extends Query {
        public IDQuery(String id) {
            super("_id:\"" + id + "\"");
            query.put("size", 1);
        }
    }

}