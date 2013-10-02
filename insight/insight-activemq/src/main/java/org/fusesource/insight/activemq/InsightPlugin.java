package org.fusesource.insight.activemq;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.fusesource.insight.activemq.base.Activator;

public class InsightPlugin implements BrokerPlugin {

    @Override
    public Broker installPlugin(Broker broker) throws Exception {
        return Activator.installPlugins(broker);
    }

}
