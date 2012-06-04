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

public class ConsumerThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerThread.class);

    int messageCount = 1000;
    int receiveTimeOut = 3000;
    int received = 0;
    int transactions = 0;
    String dest;
    JMSService service;
    boolean breakOnNull = false;
    boolean running = false;
    int sleep;
    int transactionBatchSize;

    public ConsumerThread(JMSService service, String dest) {
        this.dest = dest;
        this.service = service;
    }

    @Override
    public void run() {
      running = true;
      MessageConsumer consumer = null;

        try {
            consumer = service.createConsumer(dest);
            while (running && received < messageCount) {
                Message msg = consumer.receive(receiveTimeOut);
                if (msg != null) {
                    LOG.info("Received " + (msg instanceof TextMessage ? ((TextMessage)msg).getText() : msg.getJMSMessageID()));
                    received++;
                } else {
                    if (breakOnNull) {
                        break;
                    }
                }

                if (transactionBatchSize > 0 && received > 0 && received % transactionBatchSize == 0) {
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
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
        
        LOG.info("Consumer thread finished");
    }

    public int getReceived() {
        return received;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public void setBreakOnNull(boolean breakOnNull) {
        this.breakOnNull = breakOnNull;
    }


    public void setReceiveTimeOut(int receiveTimeOut) {
        this.receiveTimeOut = receiveTimeOut;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
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
