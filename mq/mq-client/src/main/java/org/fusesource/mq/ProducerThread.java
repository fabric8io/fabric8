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
package org.fusesource.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

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
    byte[] payload = null;
    int transactionBatchSize;
    boolean running = false;

    public ProducerThread(JMSService service, String dest) {
        this.dest = dest;
        this.service = service;
    }

    public void run() {
        MessageProducer producer = null;
        try {
            producer = service.createProducer(dest);
            producer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            initPayLoad();
            running = true;
            for (sentCount = 0; sentCount < messageCount; sentCount++) {
                if (!running)
                    break;
                Message message = createMessage(sentCount);
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
            for (int i=0; i<payload.length; i++) {
                payload[i] = '.';
            }
        }
    }

    protected Message createMessage(int i) throws Exception {
        if (payload != null) {
            return service.createBytesMessage(payload);
        } else {
            return service.createTextMessage("test message: " + i);
        }
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

    public void setTransactionBatchSize(int transactionBatchSize) {
        this.transactionBatchSize = transactionBatchSize;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
