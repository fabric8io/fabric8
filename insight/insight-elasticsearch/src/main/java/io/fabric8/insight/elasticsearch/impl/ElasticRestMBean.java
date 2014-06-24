package io.fabric8.insight.elasticsearch.impl;

import java.io.IOException;

/**
 * Created by gnodet on 26/06/14.
 */
public interface ElasticRestMBean {

    String exec(String method, String resource, String content) throws IOException;

}
