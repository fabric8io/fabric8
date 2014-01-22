package io.fabric8.insight.activemq.base;

public interface SwichtableBrokerPluginMBean {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    String getConfiguration();

    void setConfiguration(String configuration);

    void enableForDest(String dest);

    void disableForDest(String dest);

    void clearForDest(String dest);

}
