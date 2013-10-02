package org.fusesource.insight.activemq.base;

import java.util.Map;

import org.apache.activemq.broker.BrokerPlugin;

public interface InsightBrokerPlugin extends BrokerPlugin {

    void update(Map<String, String> properties);

}
