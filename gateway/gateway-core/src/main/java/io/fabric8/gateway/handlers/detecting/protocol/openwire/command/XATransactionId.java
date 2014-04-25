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


import javax.transaction.xa.Xid;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * @openwire:marshaller code="112"
 * @version $Revision: 1.6 $
 */
public class XATransactionId extends TransactionId implements Xid, Comparable {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.ACTIVEMQ_XA_TRANSACTION_ID;

    private int formatId;
    private byte[] branchQualifier;
    private byte[] globalTransactionId;

    private transient int hash;
    private transient String transactionKey;
    private transient ArrayList<MessageAck> preparedAcks;

    public XATransactionId() {
    }

    public XATransactionId(Xid xid) {
        this.formatId = xid.getFormatId();
        this.globalTransactionId = xid.getGlobalTransactionId();
        this.branchQualifier = xid.getBranchQualifier();
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public synchronized String getTransactionKey() {
        if (transactionKey == null) {
            StringBuffer s = new StringBuffer();
            s.append("XID:[globalId=");
            for (int i = 0; i < globalTransactionId.length; i++) {
                s.append(Integer.toHexString(globalTransactionId[i]));
            }
            s.append(",branchId=");
            for (int i = 0; i < branchQualifier.length; i++) {
                s.append(Integer.toHexString(branchQualifier[i]));
            }
            s.append("]");
            transactionKey = s.toString();
        }
        return transactionKey;
    }

    public String toString() {
        return getTransactionKey();
    }

    public boolean isXATransaction() {
        return true;
    }

    public boolean isLocalTransaction() {
        return false;
    }

    /**
     * @openwire:property version=1
     */
    public int getFormatId() {
        return formatId;
    }

    /**
     * @openwire:property version=1
     */
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    /**
     * @openwire:property version=1
     */
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    public void setBranchQualifier(byte[] branchQualifier) {
        this.branchQualifier = branchQualifier;
        this.hash = 0;
    }

    public void setFormatId(int formatId) {
        this.formatId = formatId;
        this.hash = 0;
    }

    public void setGlobalTransactionId(byte[] globalTransactionId) {
        this.globalTransactionId = globalTransactionId;
        this.hash = 0;
    }

    public int hashCode() {
        if (hash == 0) {
            hash = formatId;
            hash = hash(globalTransactionId, hash);
            hash = hash(branchQualifier, hash);
            if (hash == 0) {
                hash = 0xaceace;
            }
        }
        return hash;
    }

    private static int hash(byte[] bytes, int hash) {
        int size = bytes.length;
        for (int i = 0; i < size; i++) {
            hash ^= bytes[i] << ((i % 4) * 8);
        }
        return hash;
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != XATransactionId.class) {
            return false;
        }
        XATransactionId xid = (XATransactionId)o;
        return xid.formatId == formatId && Arrays.equals(xid.globalTransactionId, globalTransactionId)
               && Arrays.equals(xid.branchQualifier, branchQualifier);
    }

    public int compareTo(Object o) {
        if (o == null || o.getClass() != XATransactionId.class) {
            return -1;
        }
        XATransactionId xid = (XATransactionId)o;
        return getTransactionKey().compareTo(xid.getTransactionKey());
    }

    public void setPreparedAcks(ArrayList<MessageAck> preparedAcks) {
        this.preparedAcks = preparedAcks;
    }

    public ArrayList<MessageAck> getPreparedAcks() {
        return preparedAcks;
    }
}
