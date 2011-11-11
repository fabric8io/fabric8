package org.fusesource.fabric.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.Group;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FabricLoadBalanceStrategySupport implements LoadBalanceStrategy {
    private static final transient Log LOG = LogFactory.getLog(FabricLoadBalanceStrategySupport.class);
    protected Group group;
    protected List<String> alternateAddressList = new CopyOnWriteArrayList<String>();

    public void setGroup(final Group group) {
        this.group = group;
        group.add(new ChangeListener(){
            @Override
            public void changed() {

                alternateAddressList.clear();
                for (byte[] uri : group.members().values()) {
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Added the CXF endpoint address " + new String(uri, "UTF-8"));
                        }
                        alternateAddressList.add(new String(uri, "UTF-8"));
                    } catch (UnsupportedEncodingException ignore) {
                    }
                }
            }

            public void connected() {
                changed();
            }

            @Override
            public void disconnected() {
                changed();
            }
        });
    }

    public List<String> getAlternateAddressList() {
        return new ArrayList(alternateAddressList);
    }

}
