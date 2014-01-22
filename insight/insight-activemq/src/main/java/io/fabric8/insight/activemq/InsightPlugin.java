package io.fabric8.insight.activemq;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import io.fabric8.insight.activemq.base.Activator;

public class InsightPlugin implements BrokerPlugin {

    @Override
    public Broker installPlugin(Broker broker) throws Exception {
        return Activator.installPlugins(broker);
    }

}
