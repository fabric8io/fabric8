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

package org.fusesource.mq.monitor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Main {

    String jmx = "service:jmx:rmi:///jndi/rmi://127.0.0.1:11099/jmxrmi";
    String user;
    String password;
    long interval = 5*1000;

    public static void main(String[] args) throws Exception {
        Main main = new Main();

        // Process the arguments
        LinkedList<String> argl = new LinkedList<String>(Arrays.asList(args));
        while(!argl.isEmpty()) {
            try {
                String arg = argl.removeFirst();
                if( "--help".equals(arg) ) {
                    displayHelpAndExit(0);
                } else if( "--jmx".equals(arg) ) {
                    main.jmx = shift(argl);
                } else if( "--user".equals(arg) ) {
                    main.user = shift(argl);
                } else if( "--password".equals(arg) ) {
                    main.password = shift(argl);
                } else if( "--interval".equals(arg) ) {
                    main.interval = Long.parseLong(shift(argl));
                } else {
                    System.err.println("Invalid usage: unknown option: "+arg);
                    displayHelpAndExit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid usage: argument not a number");
                displayHelpAndExit(1);
            }
        }

        main.execute();
        System.exit(0);
    }

    private static String shift(LinkedList<String> argl) {
        if(argl.isEmpty()) {
            System.err.println("Invalid usage: Missing argument");
            displayHelpAndExit(1);
        }
        return argl.removeFirst();
    }

    private static void displayHelpAndExit(int exitCode) {
        System.exit(exitCode);
    }

    static class Stats {
        private final HashMap<ObjectName, HashMap<String, Object>> queues;
        private final HashMap<ObjectName, HashMap<String, Object>> subs;

        Stats(HashMap<ObjectName, HashMap<String, Object>> queues, HashMap<ObjectName, HashMap<String, Object>> subs) {
            this.queues = queues;
            this.subs = subs;
        }
    }

    HashSet<String> fetchedQueueAttributes = new HashSet<String>(Arrays.asList("Name", "QueueSize", "ExpiredCount"));
    HashSet<String> fetchedSubscriptionAttributes = new HashSet<String>(Arrays.asList("ConnectionId", "MessageCountAwaitingAcknowledge", "DispatchedCounter"));

    MBeanServerConnection connection;
    private void execute() throws Exception {
        HashMap<String, Object> env = new HashMap<String, Object>();
        if( user!=null ) {
            String[] creds = {user, password};
            env.put(JMXConnector.CREDENTIALS, creds);
        }
        JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(jmx), env);
        try {
            connection = connector.getMBeanServerConnection();
            Stats oldStats = fetchStats();
            while(true) {
                Thread.sleep(interval);
                Stats newStats = fetchStats();
                analyze(oldStats, newStats);
            }

        } finally {
            connector.close();
        }
    }

    private long diff(String attribute, HashMap<String, Object> next, HashMap<String, Object> prev) {
        Number prevNumber = (Number) prev.get(attribute);
        Number nextNumber = (Number) next.get(attribute);
        if(prevNumber==null || nextNumber==null)
            return 0;
        return nextNumber.longValue() - prevNumber.longValue();
    }

    private long l(HashMap<String, Object> next, String attribute) {
        Number n = (Number) next.get(attribute);
        if(n==null)
            return 0;
        return n.longValue();
    }

    private void analyze(Stats prevStats, Stats currentStats) {

        // We can now compare the old stats with the new stats..
        for (Map.Entry<ObjectName, HashMap<String, Object>> entry : currentStats.queues.entrySet()) {
            HashMap<String, Object> current = entry.getValue();
            HashMap<String, Object> previous = prevStats.queues.get(entry.getKey());
            if(previous==null) continue;

            long diff = diff("ExpiredCount", current, previous);
            if(diff!=0) {
                System.out.println(String.format("Queue %s expired count changed: %d", current.get("Name"), diff));
            }
        }

        for (Map.Entry<ObjectName, HashMap<String, Object>> entry : currentStats.subs.entrySet()) {
            HashMap<String, Object> current = entry.getValue();
            HashMap<String, Object> previous = prevStats.subs.get(entry.getKey());
            if(previous==null) continue;

            if( l(current, "MessageCountAwaitingAcknowledge") > 0 && diff("DispatchedCounter", current, previous) ==0 )  {
                System.out.println(String.format("Subscription %s looks hung, on connection: %s it has not acknowleged any messages since the last poll interval.", entry.getKey(), current.get("ConnectionId")));
            }
        }


    }

    public Stats fetchStats() throws Exception {

        HashMap<ObjectName, HashMap<String, Object>> queues = new HashMap<ObjectName, HashMap<String, Object>>();
        {
            Set<ObjectName> mbeans = connection.queryNames(new ObjectName("org.apache.activemq:Type=Queue,*"), null);
            for (ObjectName mbean : mbeans) {
                HashMap<String, Object> attributes = new HashMap<String, Object>();
                for( String attr: fetchedQueueAttributes) {
                    try {
                        Object value = connection.getAttribute(mbean, attr);
                        attributes.put(attr, value);
                    } catch (Exception e) {
                    }
                }
                if( !attributes.isEmpty() ) {
                    queues.put(mbean, attributes);
                }
            }
        }

        HashMap<ObjectName, HashMap<String, Object>> subscriptions = new HashMap<ObjectName, HashMap<String, Object>>();
        {
            Set<ObjectName> mbeans = connection.queryNames(new ObjectName("org.apache.activemq:Type=Subscription,destinationType=Queue,*"), null);
            for (ObjectName mbean : mbeans) {
                HashMap<String, Object> attributes = new HashMap<String, Object>();
                for( String attr: fetchedSubscriptionAttributes) {
                    try {
                        Object value = connection.getAttribute(mbean, attr);
                        attributes.put(attr, value);
                    } catch (Exception e) {
                    }
                }
                if( !attributes.isEmpty() ) {
                    subscriptions.put(mbean, attributes);
                }
            }
        }
        return new Stats(queues, subscriptions);
    }


}
