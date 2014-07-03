package io.fabric8.insight.elasticsearch.command;

import java.io.IOException;
import java.net.URI;

import io.fabric8.insight.elasticsearch.ElasticRest;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.elasticsearch.node.Node;

@Command(scope = "elasticsearch", name = "esrest")
public class EsRest extends OsgiCommandSupport {

    @Option(name = "--no-pretty")
    boolean noPretty = false;

    @Option(name = "--get")
    boolean get;

    @Option(name = "--put")
    boolean put;

    @Option(name = "--delete")
    boolean delete;

    @Option(name = "--head")
    boolean head;

    @Option(name = "--post")
    boolean post;

    @Argument(required = true)
    String uri;

    @Argument(index = 1)
    String body;

    @Override
    protected Object doExecute() throws Exception {
        ElasticRest rest = getService(ElasticRest.class);
        if (rest == null) {
            throw new IllegalStateException("Unable to find ElasticSearch");
        }
        doExecute(rest);
        return null;
    }

    protected void doExecute(ElasticRest rest) throws IOException {
        int nb = (get ? 1 : 0) + (put ? 1 : 0) + (delete ? 1 : 0) + (head ? 1 : 0) + (post ? 1 : 0);
        if (nb > 1) {
            throw new IllegalArgumentException("Only one of get / put / delete / head / post can be used");
        }
        String ret;
        if (!noPretty) {
            int idx = uri.indexOf("?");
            if (idx > 0) {
                boolean hasPretty = false;
                for (String p : uri.substring(idx + 1).split("&")) {
                    hasPretty |= (p.equals("pretty") || p.startsWith("pretty="));
                }
                uri = uri + "&pretty";
            } else {
                uri = uri + "?pretty";
            }
        }
        if (post) {
            ret = rest.post(uri, body != null ? body : "");
        } else if (put) {
            ret = rest.put(uri, body != null ? body : "");
        } else if (head) {
            ret = rest.head(uri);
        } else if (delete) {
            ret = rest.delete(uri);
        } else {
            ret = rest.get(uri);
        }
        System.out.println(ret);
    }
}
