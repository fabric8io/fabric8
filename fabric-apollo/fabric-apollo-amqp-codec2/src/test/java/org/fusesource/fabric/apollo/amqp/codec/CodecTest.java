
package org.fusesource.fabric.apollo.amqp.codec;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType;
import org.fusesource.fabric.apollo.amqp.codec.types.Header;
import org.junit.Test;

import static org.junit.Assert.*;

public class CodecTest {

    @Test
    public void instantiateMarshaller() throws Exception {

        AmqpMarshaller marshaller = AmqpMarshaller.instance();

        for (Long key : marshaller.getFormatCodeMap().keySet()) {
            Class clazz = marshaller.getFormatCodeMap().get(key);
            System.out.println(String.format("%s = %s", key, clazz.getName()));
        }

        for (String key : marshaller.getSymbolicCodeMap().keySet()) {
            Class clazz = marshaller.getSymbolicCodeMap().get(key);
            System.out.println(String.format("%s = %s", key, clazz.getName()));
        }

        assertTrue(marshaller.getFormatCodeMap().size() > 0);
        assertTrue(marshaller.getSymbolicCodeMap().size() > 0);

        AmqpType type = (AmqpType) marshaller.getSymbolicCodeMap().get("amqp:header:list").newInstance();

        System.out.println(String.format("Got type : %s", type.getClass().getName()));

        assertSame("Received type does not match expected type!", type.getClass(), Header.class);

    }

}
