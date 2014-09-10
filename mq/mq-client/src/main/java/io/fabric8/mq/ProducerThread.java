/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ProducerThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(ProducerThread.class);

    int messageCount = 1000;
    String dest;
    protected JMSService service;
    int sleep = 0;
    int sentCount = 0;
    int transactions = 0;
    boolean persistent = true;
    int messageSize = 0;
    String textMessageSize;
    byte[] payload = null;
    int transactionBatchSize;
    boolean running = false;
    long msgTTL = 0L;
    String msgGroupID=null;
    Set<String> sizeTypes;

    public ProducerThread(JMSService service, String dest) {
        this.dest = dest;
        this.service = service;
        this.sizeTypes = getSizeTypes();
    }

    public void run() {
        MessageProducer producer = null;
        try {
            producer = service.createProducer(dest);
            producer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            producer.setTimeToLive(msgTTL);
            initPayLoad();
            running = true;

            LOG.info("\nStarted to calculate elapsed time ...\n");
            long tStart = System.currentTimeMillis();

            for (sentCount = 0; sentCount < messageCount; sentCount++) {
                if (!running)
                    break;
                Message message = createMessage(sentCount);
                if ((msgGroupID!=null)&&(!msgGroupID.isEmpty())) message.setStringProperty("JMSXGroupID", msgGroupID);
                producer.send(message);
                LOG.info("Sent: " + (message instanceof TextMessage ? ((TextMessage) message).getText() : message.getJMSMessageID()));

                if (transactionBatchSize > 0 && sentCount > 0 && sentCount % transactionBatchSize == 0) {
                    LOG.info("Committing transaction: " + transactions++);
                    service.getDefaultSession().commit();
                }

                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
            }

            long tEnd = System.currentTimeMillis();
            long elapsed = (tEnd - tStart) / 1000;
            LOG.info("\nElapsed time in second : " + elapsed + "s\n");
            LOG.info("\nElapsed time in milli second : " + (tEnd - tStart) + "milli seconds\n");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
        LOG.info("Producer thread finished");
    }

    private void initPayLoad() {
        if (messageSize > 0) {
            payload = new byte[messageSize];
            for (int i = 0; i < payload.length; i++) {
                payload[i] = '.';
            }
        }
    }

    private HashSet<String> getSizeTypes() {
        String[] types = {"100b", "1K", "10K"};
        return new HashSet<String>(Arrays.asList(types));
    }

    protected Message createMessage(int i) throws Exception {
        Message message = null;
        if (payload != null) {
            message = service.createBytesMessage(payload);
        } else {
            if (textMessageSize == null) {
                message = service.createTextMessage("test message: " + i);
            } else {
                if (sizeTypes.contains(textMessageSize)) {
                    if (textMessageSize.equals("100b")) {
                        message =  service.createTextMessage(i + "::" + dummy100bMessage());
                    } else if (textMessageSize.equals("1K")) {
                        message =  service.createTextMessage(i + "::" + dummy1KMessage());
                    } else if (textMessageSize.equals("10K")) {
                        message =  service.createTextMessage(i + "::" + dummy10KMessage());
                    }
                } else {
                    LOG.info("Type size unknown : " + textMessageSize + ", we will use a text message of 100b");
                    message =  service.createTextMessage(i + "::" + dummy100bMessage());
                }
            }
        }
        return message;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public void setMessageSize(int size) {
        this.messageSize = size;
    }

    public String getTextMessageSize() {
        return textMessageSize;
    }

    public void setTextMessageSize(String textMessageSize) {
        this.textMessageSize = textMessageSize;
    }

    public void setTransactionBatchSize(int transactionBatchSize) {
        this.transactionBatchSize = transactionBatchSize;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setTTL(long ttl) { this.msgTTL = ttl;}

    public void setMsgGroupID(String msgGroupID) { this.msgGroupID = msgGroupID;}

    public static String dummy100bMessage() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque id ex ut dolor iaculis turpis duis.";
    }

    public static String dummy1KMessage() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean blandit ipsum at varius ornare. Cras commodo massa et mauris feugiat, a convallis enim imperdiet. Integer sit amet lorem egestas, semper sapien vel, fermentum eros. Praesent eu iaculis mauris, eget viverra felis. Suspendisse ut neque convallis, pulvinar erat pulvinar, tristique risus. Vivamus ut vehicula massa, a fermentum nisi. Nullam ut dapibus mi.\n" +
                "\n" +
                "In nulla leo, semper vel turpis sollicitudin, malesuada ornare metus. Integer dolor leo, accumsan a elit et, cursus congue libero. Curabitur quis ultricies eros. Vivamus bibendum purus sit amet erat lacinia blandit non at orci. Aliquam varius faucibus mauris, at ultrices arcu viverra ac. In hac habitasse platea dictumst. Curabitur ornare neque in eros vehicula, ac tempor purus ultrices.\n" +
                "\n" +
                "Nam eu convallis tortor. In elementum laoreet augue eget placerat. Quisque et ipsum lorem. Cras consectetur accumsan eleifend. Nunc ante libero, vulputate a vulputate et, venenatis non nibh amet.";
    }

    public static String dummy10KMessage() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur molestie, est sed scelerisque pellentesque, tortor enim dictum sem, a tincidunt purus massa id nisl. Mauris interdum velit et lobortis finibus. Nullam at iaculis sapien. Nunc nunc eros, iaculis nec iaculis vel, pellentesque et neque. Duis quis nisl a nulla dictum blandit id eu lectus. Aliquam auctor rhoncus tellus nec dignissim. Mauris ut convallis enim. Vivamus ut quam eu purus tempus venenatis. Phasellus ac tristique massa.\n" +
                "\n" +
                "Proin aliquam congue orci et fringilla. Praesent fermentum sem non quam consectetur pulvinar. Nullam et neque ultrices, dapibus nibh in, ullamcorper ipsum. Sed condimentum augue at sem maximus pulvinar. Suspendisse ornare lacus nisi, ac porttitor velit hendrerit a. Proin vestibulum laoreet nibh lacinia condimentum. Aliquam erat volutpat. Etiam at libero imperdiet, congue erat quis, tristique mauris. Fusce ultrices ex felis, ut euismod velit placerat vel. Praesent consequat convallis ligula, eget iaculis eros fringilla ac. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Nullam fermentum ac felis a ornare. Donec in justo rhoncus, auctor nunc vitae, fringilla risus. Morbi eget ullamcorper eros. Fusce lorem diam, dignissim et urna quis, tempus laoreet tortor. Sed lectus nulla, suscipit non rutrum et, porta sed tellus.\n" +
                "\n" +
                "Nullam accumsan placerat urna. Nulla porttitor pharetra sagittis. Fusce arcu mauris, sagittis eu dignissim eu, auctor vel dui. Phasellus sit amet quam faucibus, scelerisque ex nec, tempus orci. Pellentesque sodales tincidunt egestas. Etiam tempor erat a fermentum pharetra. Nam eu arcu quis sapien cursus auctor. Phasellus in lectus hendrerit, pellentesque velit et, consectetur elit. In eu purus non felis sollicitudin scelerisque. Quisque sed facilisis erat, in gravida turpis. Praesent cursus ac nunc in convallis. Ut sollicitudin porta cursus. Nullam sollicitudin ac eros nec rutrum. Nunc auctor cursus nulla, eget vehicula dolor laoreet eu.\n" +
                "\n" +
                "Nam eget tortor sed diam viverra fringilla. Sed fringilla tincidunt eleifend. Sed mattis tristique maximus. Sed ac dolor risus. Morbi ullamcorper gravida tortor nec viverra. Sed sed laoreet lacus. Vivamus non magna scelerisque, commodo felis fermentum, sollicitudin libero. Nunc ut nisi sed tellus consequat tristique accumsan in purus. In dictum consequat sem. Integer ut scelerisque eros. Sed tortor nisi, feugiat eget faucibus vel, tincidunt quis elit. In gravida accumsan lobortis.\n" +
                "\n" +
                "Aenean venenatis nibh non euismod hendrerit. Mauris ultricies placerat pretium. In enim neque, placerat at lorem quis, semper venenatis ipsum. Maecenas egestas blandit est id dapibus. Aenean eu convallis nisl. Sed quis mi fringilla, aliquam mi vitae, sagittis dui. Sed non massa viverra, posuere velit mattis, porttitor odio. Mauris auctor augue id interdum consectetur.\n" +
                "\n" +
                "Mauris eget arcu sapien. Aenean vel condimentum sapien. Pellentesque facilisis venenatis ex, id faucibus mi suscipit eget. Nunc gravida augue ut velit vulputate, in dapibus ex posuere. Cras tempus purus eget mauris pharetra, id varius lorem sagittis. Fusce ligula tortor, laoreet non bibendum at, elementum nec lorem. Cras at lacus sem. Proin tempus magna eu elit placerat tristique.\n" +
                "\n" +
                "Integer egestas leo vitae justo dignissim, non pellentesque tortor pharetra. Aenean a dictum turpis. Phasellus luctus volutpat elit, a vulputate massa. Suspendisse quis libero erat. Nunc justo ante, laoreet nec nibh quis, pulvinar fringilla est. Mauris iaculis in arcu non rutrum. Pellentesque ut mauris quis turpis vulputate bibendum in at sapien. Sed sit amet semper tellus. Aliquam lobortis urna odio, ac placerat est tincidunt ullamcorper. Phasellus pulvinar eros magna. Aliquam molestie eget ipsum a iaculis. Sed hendrerit laoreet massa et egestas. Sed sollicitudin lorem sed ante tristique, ut dignissim lorem volutpat. Vestibulum et rhoncus urna.\n" +
                "\n" +
                "Nulla tempus, felis quis tincidunt lacinia, massa massa bibendum purus, nec rhoncus risus ligula et nisi. Nunc eget ligula rhoncus, finibus mi a, eleifend odio. Nullam volutpat libero nec quam porta, ut dignissim sapien dapibus. Donec finibus orci in orci malesuada, eu tempus arcu fringilla. Integer nec diam vitae arcu sollicitudin scelerisque ac eu elit. Pellentesque sodales volutpat lectus, in vulputate massa rhoncus a. Aliquam erat volutpat. Vivamus et ultricies mauris, vitae tincidunt massa. Suspendisse congue tempor leo at eleifend. Proin aliquam nec ligula sed elementum. Integer eu mauris ex. Nulla libero felis, dictum a aliquet a, blandit euismod nulla. Vivamus eu mi posuere, tincidunt magna eu, congue dolor. Etiam gravida nec felis et vulputate. Vestibulum eu urna sit amet ante viverra iaculis mollis non purus.\n" +
                "\n" +
                "Maecenas id turpis pulvinar, pellentesque tellus sit amet, rutrum lorem. Mauris nec placerat purus, vitae varius massa. Ut pulvinar, tellus sit amet accumsan tempus, nulla sapien blandit tellus, non tristique ante dolor et sapien. Nunc at felis non tortor faucibus tincidunt ornare id velit. Cras sit amet iaculis felis, id elementum augue. Suspendisse scelerisque felis orci, quis bibendum nisi commodo et. Ut blandit tempor molestie. Aenean convallis sollicitudin tempus. Vestibulum blandit, ante vel tempus cursus, dolor neque pulvinar ante, vel malesuada eros mi at libero. Sed tristique tellus lectus, quis imperdiet eros convallis id. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Vestibulum mollis, risus quis sollicitudin sollicitudin, libero augue venenatis urna, id hendrerit turpis urna non est. Etiam commodo commodo massa nec tempor. Nullam non est congue, sagittis quam ac, finibus libero. Cras ullamcorper tortor eget maximus hendrerit.\n" +
                "\n" +
                "Quisque ut volutpat diam. Curabitur quis ligula aliquam, pharetra arcu sed, varius tellus. Vivamus turpis libero, euismod id nunc at, lacinia consequat purus. Proin eu posuere ex. Nulla sodales turpis sed magna ultricies egestas. Pellentesque quis tortor non lorem vehicula luctus sed ac dui. Fusce eleifend, diam eu fringilla tincidunt, est felis aliquet massa, vulputate ultrices eros lacus eu lacus. Nullam consequat, tortor at lobortis porttitor, odio nibh feugiat velit, a iaculis lacus nisi ac massa. Quisque faucibus lorem quis fermentum feugiat. Quisque dapibus semper nisi, eu molestie felis varius at.\n" +
                "\n" +
                "Donec non posuere lectus. Nunc condimentum dolor magna, sed condimentum ante maximus ut. Nulla et vestibulum magna, sed cursus nisl. Ut scelerisque feugiat risus, at ornare purus semper eget. In volutpat hendrerit lacus, non dictum est sagittis scelerisque. Nullam non euismod nunc. Phasellus fermentum magna id est dignissim, id dapibus purus eleifend. Etiam rutrum id tortor id ullamcorper. Aliquam nibh enim, iaculis et velit nec, dignissim hendrerit lacus. Donec molestie nulla est, nec facilisis ante varius sit amet. Sed leo arcu, mattis sed nulla vitae, aliquet luctus sem.\n" +
                "\n" +
                "Praesent pretium lectus blandit enim ultrices, eu lobortis mauris elementum. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Nam elementum pellentesque quam ac tristique. Mauris quis libero ac erat posuere fermentum non quis mauris. Sed id laoreet enim, ultrices mollis odio. Vivamus non vehicula lectus, a tristique lorem. Aliquam viverra nibh nunc, ut hendrerit justo blandit efficitur. Duis mattis nisi ut bibendum dignissim. Praesent a odio in nulla tincidunt commodo.\n" +
                "\n" +
                "Ut sodales augue at feugiat finibus. Donec vel feugiat mauris. Vestibulum metus leo, venenatis congue orci consectetur, congue accumsan tellus. Sed sed placerat odio. Ut ac nulla vel sem interdum placerat quis vel lorem. Donec sagittis neque tortor, at finibus lectus malesuada cursus. In hac habitasse platea dictumst. Etiam ac efficitur magna, eu venenatis urna.\n" +
                "\n" +
                "In egestas tincidunt nisi non ultricies. Ut sodales nunc ut leo tempus sodales sed in mi. Aenean aliquet posuere felis viverra consectetur. Curabitur ac erat volutpat, malesuada ligula eget, finibus neque. Vivamus enim tortor, dignissim eu ornare ut, molestie a risus. Sed sed dolor leo. Ut vel posuere neque, vel faucibus sem. In tempus interdum velit, quis pellentesque mauris. Fusce sit amet eleifend augue. Morbi quis lorem id diam auctor elementum sit amet eget justo. Ut suscipit in lorem eget placerat. Fusce finibus dolor a egestas pellentesque. Nullam id bibendum erat, non dignissim nulla.\n" +
                "\n" +
                "Aliquam ante massa, facilisis a cursus nec, eleifend sit amet ex. Fusce consectetur libero vitae turpis molestie, in pretium leo tempor. Proin sit amet aliquet metus, quis placerat tortor. Suspendisse potenti. Donec placerat odio ut nisl tincidunt, eget egestas turpis tempor. Vivamus eu tortor porta, dignissim nunc a, lobortis ipsum. Praesent efficitur mi malesuada porttitor sollicitudin. Nulla turpis sem, aliquet a accumsan in, mattis ac sem. Vestibulum vulputate a lectus et eleifend. Cras imperdiet massa a hendrerit gravida. Integer vel viverra magna, ut pellentesque lectus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus volutpat arcu fringilla nisl condimentum ornare. Cras eros diam, iaculis ut eleifend vel, condimentum eget enim. Quisque tristique, neque ut dignissim luctus, elit neque congue magna, in posuere turpis magna ut justo. Nunc iaculis tortor eu nulla feugiat, a sollicitudin enim volutpat.\n" +
                "\n" +
                "Vestibulum vitae ex mauris. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Aenean mattis sapien sem, id posuere justo egestas ac. Nam a consequat nisl, vel pharetra nibh. Sed tempus sagittis metus, eu volutpat mi pellentesque sit amet. Quisque vitae sollicitudin tellus. Sed et ante a ex vestibulum rhoncus. Cras aliquam tincidunt congue. Nulla luctus tincidunt massa, vitae scelerisque mauris volutpat nec. Donec vestibulum eget elit eget efficitur. In hendrerit sed sapien et pretium. Pellentesque et mi nec mauris consequat condimentum non nec metus. Aliquam viverra dapibus ipsum, non tempor est tempus a. Donec amet.";
    }
}
