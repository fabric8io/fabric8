/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.codec;

import org.fusesource.fusemq.amqp.codec.types.*;
import org.fusesource.hawtbuf.Buffer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;

import static org.fusesource.fusemq.amqp.codec.CodecUtils.getSize;
import static org.fusesource.fusemq.amqp.codec.CodecUtils.unmarshal;
import static org.fusesource.fusemq.amqp.codec.marshaller.v1_0_0.AmqpMarshaller.getMarshaller;
import static org.fusesource.fusemq.amqp.codec.types.TypeFactory.*;

/**
 * Representation of an amqp message,
 */
public class AmqpMessage {

    public static final int HEADER = 0;
    public static final int PROPERTIES = 1;
    public static final int FOOTER = 2;
    public static final int DATA = 3;
    public static final int AMQP_DATA = 4;
    public static final int AMQP_MAP = 5;
    public static final int AMQP_LIST = 6;

    private AmqpHeader header = null;
    private AmqpProperties properties = null;
    private AmqpFooter footer = null;

    private LinkedList<AmqpFragment> fragments = new LinkedList<AmqpFragment>();

    public AmqpMessage() {

    }

    public AmqpMessage(IAmqpList<AmqpFragment> list) {
        setFragments(list);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append("AmqpMessage{");
        if (header != null) {
            ret.append("header=");
            ret.append(header);
            ret.append(" ");
        }
        if (properties != null) {
            ret.append("properties=");
            ret.append(properties);
            ret.append(" ");
        }
        ret.append("fragments=");
        ret.append(fragments);
        if (footer != null) {
            ret.append(" footer=");
            ret.append(footer);
        }

        ret.append("}");

        return ret.toString();
    }

    public int getCount() {
        return fragments.size();
    }

    public Object getPayload(int index) throws IOException {
        AmqpFragment fragment = fragments.get(index);
        switch (fragment.getSectionCode().intValue()) {
            case DATA:
                return fragment.getPayload();
            case AMQP_DATA:
                return unmarshal(fragment.getPayload().data);
            case AMQP_MAP:
                return AmqpMap.AmqpMapBuffer.create(fragment.getPayload(), 0, getMarshaller());
            case AMQP_LIST:
                return AmqpList.AmqpListBuffer.create(fragment.getPayload(), 0, getMarshaller());
            default:
                throw new RuntimeException("Format code " + fragment.getSectionCode() + " is unknown or does not belong in message body");
        }
    }

    /**
     * Get the encoded size of the message
     * @return
     */
    public long getEncodedSize() throws IOException {
        long rc = 0;
        LinkedList<AmqpFragment> fragments = constructMessage();
        for ( AmqpFragment fragment : fragments ) {
            rc += getSize(fragment);
        }
        return rc;
    }

    /**
     * Get the total size of the fragment payloads
     * @return
     * @throws IOException
     */
    public long getPayloadSize() throws IOException {
        long rc = 0;
        LinkedList<AmqpFragment> fragments = constructMessage();
        for ( AmqpFragment fragment : fragments ) {
            rc += fragment.getPayload().getLength();
        }
        return rc;
    }

    /**
     * Get the size of the message body, includes everything exception header/footer
     * @return
     * @throws IOException
     */
    public long getBodySize() throws IOException {
        long rc = 0;
        if (properties != null) {
            rc += getPropertiesFragment().getPayload().getLength();
        }
        for (AmqpFragment fragment : fragments) {
            rc += fragment.getPayload().getLength();
        }
        return rc;
    }

    private LinkedList<AmqpFragment> constructMessage() throws IOException {
        LinkedList<AmqpFragment> rc = new LinkedList<AmqpFragment>(fragments);
        if (properties != null) {
            rc.addFirst(getPropertiesFragment());
        }
        long offset = 0;
        for( AmqpFragment fragment : rc ) {
            fragment.setSectionNumber(1);
            fragment.setSectionOffset(BigInteger.valueOf(offset++));
        }
        if (header != null) {
            rc.addFirst(getHeaderFragment());
        }
        if (footer != null) {
            rc.addLast(getFooterFragment());
        }

        return rc;
    }

    /**
     * Create a fragment list that can fit into one transfer frame
     * @return
     */
    public AmqpList construct() throws IOException {
        return createList(constructMessage());
    }

    private static AmqpList createList(LinkedList<AmqpFragment> fragments) {
        AmqpType<?, ?> fragmentsArray[] = new AmqpType<?, ?>[fragments.size()];
        fragmentsArray = fragments.toArray(fragmentsArray);
        return createAmqpList(new IAmqpList.ArrayBackedList(fragmentsArray));
    }
    /**
     * Create a list of fragment lists that will be sent across a number of transfer frames
     * @param maxSize
     * @return
     */
    // TODO could actually break up long fragments here
    public LinkedList<AmqpList> construct(long maxSize) throws IOException {
        LinkedList<AmqpFragment> fragments = constructMessage();
        LinkedList<AmqpList> rc = new LinkedList<AmqpList>();
        LinkedList<AmqpFragment> currentFragments = new LinkedList<AmqpFragment>();
        long currentSize = 0;
        for ( AmqpFragment fragment : fragments ) {
            if ( currentSize > maxSize ) {
                currentSize = 0;
                rc.add(createList(currentFragments));
                currentFragments = new LinkedList<AmqpFragment>();
            }
            currentSize += getSize(fragment);
            currentFragments.add(fragment);
        }
        rc.add(createList(currentFragments));
        return rc;
    }


    public static AmqpFragment createFragment(int sectionCode) {
        AmqpFragment fragment = createAmqpFragment();
        fragment.setSectionCode(sectionCode);
        return fragment;
    }

    private AmqpFragment getFooterFragment() throws IOException {
        AmqpFragment fragment = createFragment(FOOTER);
        setPayload(fragment, footer);
        fragment.setFirst(true);
        fragment.setLast(true);
        fragment.setSectionNumber(3);
        fragment.setSectionOffset(BigInteger.ZERO);
        return fragment;
    }

    private AmqpFragment getPropertiesFragment() throws IOException {
        AmqpFragment fragment = createFragment(PROPERTIES);
        setPayload(fragment, properties);
        fragment.setFirst(true);
        fragment.setLast(true);
        return fragment;
    }

    private AmqpFragment getHeaderFragment() throws IOException {
        AmqpFragment fragment = createFragment(HEADER);
        setPayload(fragment, header);
        fragment.setFirst(true);
        fragment.setLast(true);
        fragment.setSectionNumber(0);
        fragment.setSectionOffset(BigInteger.ZERO);
        return fragment;
    }

    public void setFragments(IAmqpList<AmqpFragment> list) {
        for ( AmqpFragment fragment : list ) {
            int sectionCode = fragment.getSectionCode().intValue();
            switch (sectionCode) {
                case HEADER:
                    setHeader(fragment);
                    break;
                case PROPERTIES:
                    setProperties(fragment);
                    break;
                case FOOTER:
                    setFooter(fragment);
                    break;
                default:
                    fragments.add(fragment);
            }
        }
    }

    public void setProperties(AmqpFragment fragment) {
        properties = null;
        if ( fragment.getSectionCode() != PROPERTIES ) {
            throw new IllegalArgumentException ("Expecting fragment with PROPERTIES format code, instead got " + fragment.getSectionCode());
        }
        if ( fragment.getPayload().getLength() > 0 ) {
            properties = AmqpProperties.AmqpPropertiesBuffer.create(fragment.getPayload(), 0, getMarshaller());
        }
    }

    public void setFooter(AmqpFragment fragment) {
        footer = null;
        if ( fragment.getSectionCode() != FOOTER ) {
            throw new IllegalArgumentException ("Expecting fragment with FOOTER format code, instead got " + fragment.getSectionCode());
        }
        if ( fragment.getPayload().getLength() > 0 ) {
            footer = AmqpFooter.AmqpFooterBuffer.create(fragment.getPayload(), 0, getMarshaller());
        }
    }

    public void setHeader(AmqpFragment fragment) {
        header = null;
        if ( fragment.getSectionCode() != HEADER ) {
            throw new IllegalArgumentException ("Expecting fragment with HEADER format code, instead got " + fragment.getSectionCode());
        }
        if ( fragment.getPayload().getLength() > 0 ) {
            header = AmqpHeader.AmqpHeaderBuffer.create(fragment.getPayload(), 0, getMarshaller());
        }
    }

    // TODO - Check size of passed in object against max frame size, a fragment can't be larger than a little less than the maximum frame size
    // TODO - Or better still, break it up into multiple fragments
    private void check(Object obj) {
        if ( obj == null ) {
            throw new IllegalArgumentException("Argument can't be null");
        }
    }

    public <T extends AmqpType<?, ?>> void setPayload(AmqpFragment fragment, T type) throws IOException {
        byte payload[] = new byte[0];
        if ( type != null ) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            type.marshal(out, getMarshaller());
            payload = bos.toByteArray();
        }
        fragment.setPayload(new Buffer(payload));
    }

    // TODO - These operations should create multiple fragments as necessary if there's a limit on the message size
    /**
     * Add a hawtbuff Buffer to the message
     * @param data
     */
    public void add(Buffer data) {
        check(data);
        AmqpFragment fragment = createFragment(DATA);
        fragment.setPayload(data);
        fragment.setFirst(true);
        fragment.setLast(true);
        add(fragment);
    }

    /**
     * Add plain byte array to the message
     * @param data
     */
    public void add(byte[] data) {
        check(data);
        AmqpFragment fragment = createFragment(DATA);
        fragment.setPayload(new Buffer(data));
        fragment.setFirst(true);
        fragment.setLast(true);
        add(fragment);
    }

    /**
     * Add an existing AmqpFragment to the message
     * @param fragment
     */
    public void add(AmqpFragment fragment) {
        check(fragment);
        fragments.add(fragment);
    }

    /**
     * Add some AmqpType to the message
     * @param type
     * @param <T>
     * @throws IOException
     */
    public <T extends AmqpType<?, ?>> void add(T type) throws IOException {
        check(type);
        AmqpFragment fragment = createFragment(AMQP_DATA);
        fragment.setFirst(true);
        fragment.setLast(true);
        add(fragment, type);
    }

    /**
     * Add an AmqpMap to the message
     * @param map
     * @throws IOException
     */
    public void add(AmqpMap map) throws IOException {
        check(map);
        AmqpFragment fragment = createFragment(AMQP_MAP);
        fragment.setFirst(true);
        fragment.setLast(true);
        add(fragment, map);
    }

    /**
     * Add an AmqpList to the message
     * @param list
     * @throws IOException
     */
    public void add(AmqpList list) throws IOException {
        check(list);
        AmqpFragment fragment = createFragment(AMQP_LIST);
        fragment.setFirst(true);
        fragment.setLast(true);
        add(fragment, list);
    }

    private <T extends AmqpType<?, ?>> void add(AmqpFragment fragment, T type) throws IOException {
        setPayload(fragment, type);
        add(fragment);
    }

    public LinkedList<AmqpFragment> getBody() {
        return fragments;
    }

    public AmqpHeader getHeader() {
        if (header == null) {
            header = createAmqpHeader();
        }
        return header;
    }

    public void setHeader(AmqpHeader header) {
        this.header = header;
    }

    public AmqpProperties getProperties() {
        if (properties == null) {
            properties = createAmqpProperties();
        }
        return properties;
    }

    public void setProperties(AmqpProperties properties) {
        this.properties = properties;
    }

    public AmqpFooter getFooter() {
        if (footer == null) {
            footer = createAmqpFooter();
        }
        return footer;
    }

    public void setFooter(AmqpFooter footer) {
        this.footer = footer;
    }
}
