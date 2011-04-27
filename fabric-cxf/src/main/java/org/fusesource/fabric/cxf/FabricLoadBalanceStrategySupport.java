package org.fusesource.fabric.cxf;

import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.Group;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FabricLoadBalanceStrategySupport implements LoadBalanceStrategy {
    protected Group group;
    protected List<String> alternateAddressList = new CopyOnWriteArrayList<String>();

    public void setGroup(Group group) {
        this.group = group;
        group.add(new ChangeListener(){
            public void changed(byte[][] members) {
                alternateAddressList.clear();
                for (byte[] uri : members) {
                    try {
                        System.out.println("new address");
                        alternateAddressList.add(new String(uri, "UTF-8"));
                    } catch (UnsupportedEncodingException ignore) {
                    }
                }
            }
        });
    }

}
