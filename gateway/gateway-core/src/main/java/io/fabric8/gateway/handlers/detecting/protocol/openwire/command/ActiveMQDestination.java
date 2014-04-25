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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.command;

import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * @openwire:marshaller
 * @version $Revision: 1.10 $
 */
abstract public class ActiveMQDestination implements DataStructure, Comparable {

  public static final String PATH_SEPERATOR = ".";
  public static final char COMPOSITE_SEPERATOR = ',';

  public static final byte QUEUE_TYPE = 0x01;
  public static final byte TOPIC_TYPE = 0x02;
  public static final byte TEMP_MASK = 0x04;
  public static final byte TEMP_TOPIC_TYPE = TOPIC_TYPE | TEMP_MASK;
  public static final byte TEMP_QUEUE_TYPE = QUEUE_TYPE | TEMP_MASK;

  public static final String QUEUE_QUALIFIED_PREFIX = "queue://";
  public static final String TOPIC_QUALIFIED_PREFIX = "topic://";
  public static final String TEMP_QUEUE_QUALIFED_PREFIX = "temp-queue://";
  public static final String TEMP_TOPIC_QUALIFED_PREFIX = "temp-topic://";

  public static final String TEMP_DESTINATION_NAME_PREFIX = "ID:";

  private static final long serialVersionUID = -3885260014960795889L;

  protected UTF8Buffer physicalName;

  public ActiveMQDestination() {
  }

  protected ActiveMQDestination(String name) {
      setPhysicalName(new UTF8Buffer(name));
  }

  // static helper methods for working with destinations
  // -------------------------------------------------------------------------
  public static ActiveMQDestination createDestination(String name) {
      if (name.startsWith(QUEUE_QUALIFIED_PREFIX)) {
          return new ActiveMQQueue(name.substring(QUEUE_QUALIFIED_PREFIX.length()));
      } else if (name.startsWith(TOPIC_QUALIFIED_PREFIX)) {
          return new ActiveMQTopic(name.substring(TOPIC_QUALIFIED_PREFIX.length()));
      } else if (name.startsWith(TEMP_QUEUE_QUALIFED_PREFIX)) {
          return new ActiveMQTempQueue(name.substring(TEMP_QUEUE_QUALIFED_PREFIX.length()));
      } else if (name.startsWith(TEMP_TOPIC_QUALIFED_PREFIX)) {
          return new ActiveMQTempTopic(name.substring(TEMP_TOPIC_QUALIFED_PREFIX.length()));
      } else {
          return null;
      }
  }

  public static int compare(ActiveMQDestination destination, ActiveMQDestination destination2) {
      if (destination == destination2) {
          return 0;
      }
      if (destination == null) {
          return -1;
      } else if (destination2 == null) {
          return 1;
      } else {
          if (destination.isQueue() == destination2.isQueue()) {
              return destination.getPhysicalName().compareTo(destination2.getPhysicalName());
          } else {
              return destination.isQueue() ? -1 : 1;
          }
      }
  }

  public int compareTo(Object that) {
      if (that instanceof ActiveMQDestination) {
          return compare(this, (ActiveMQDestination)that);
      }
      if (that == null) {
          return 1;
      } else {
          return getClass().getName().compareTo(that.getClass().getName());
      }
  }
    
  public abstract String getQualifiedPrefix();

  /**
   * @openwire:property version=1
   */
  public UTF8Buffer getPhysicalName() {
      return physicalName;
  }

  public void setPhysicalName(UTF8Buffer physicalName) {
      this.physicalName = physicalName;
  }

  public abstract byte getDestinationType();

  public boolean isQueue() {
      return false;
  }

  public boolean isTopic() {
      return false;
  }

  public boolean isTemporary() {
      return false;
  }

  public boolean equals(Object o) {
      if (this == o) {
          return true;
      }
      if (o == null || getClass() != o.getClass()) {
          return false;
      }
      ActiveMQDestination d = (ActiveMQDestination)o;
      return physicalName.equals(d.physicalName);
  }

  public int hashCode() {
      return physicalName.hashCode();
  }

  public String toString() {
      return physicalName.toString();
  }

  public String getDestinationTypeAsString() {
      switch (getDestinationType()) {
      case QUEUE_TYPE:
          return "Queue";
      case TOPIC_TYPE:
          return "Topic";
      case TEMP_QUEUE_TYPE:
          return "TempQueue";
      case TEMP_TOPIC_TYPE:
          return "TempTopic";
      default:
          throw new IllegalArgumentException("Invalid destination type: " + getDestinationType());
      }
  }

  public boolean isMarshallAware() {
      return false;
  }

}
