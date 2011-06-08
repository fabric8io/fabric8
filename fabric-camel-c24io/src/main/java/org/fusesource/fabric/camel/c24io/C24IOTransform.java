/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.io.IOException;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.data.Element;
import biz.c24.io.api.data.ValidationException;
import biz.c24.io.api.transform.Transform;
import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.ObjectHelper;

/**
 * Transforms an <a href="http://fabric.fusesource.org/documentation/camel/c24io.html">C24 IO</a>
 * object into some output format
 *
 * @version $Revision$
 */
public class C24IOTransform implements Processor {
    private Transform transform;
    
    public C24IOTransform(Transform transform) {
        this.transform = transform;
    }

    public static C24IOTransform transform(Class<?> transformType) {
        Transform transformer = (Transform) ObjectHelper.newInstance(transformType);
        return transform(transformer);
    }

    public static C24IOTransform transform(Transform transformer) {
        return new C24IOTransform(transformer);
    }   

    public void process(Exchange exchange) throws Exception {
        ComplexDataObject[][] objects = null;
        ComplexDataObject dataObject = null;
        try {
            dataObject = exchange.getIn().getMandatoryBody(ComplexDataObject.class);
        } catch (InvalidPayloadException e1) {
            try {
                objects = exchange.getIn().getMandatoryBody(ComplexDataObject[][].class);
            } catch (InvalidPayloadException e2) {
                objects = getInBodyAsArray(exchange, objects);
            }
        }
            
        if (objects == null) {
            if (dataObject == null) {
                dataObject = unmarshalDataObject(exchange);
            }
            objects = new ComplexDataObject[][]{{dataObject}};
        }
        Object result = transform(objects);

        Message out = exchange.getOut();
        out.setBody(result);
    }

    private ComplexDataObject[][] getInBodyAsArray(Exchange exchange, ComplexDataObject[][] objects) {
        try {
            ComplexDataObject[] array = exchange.getIn().getMandatoryBody(ComplexDataObject[].class);
            if (array != null) {
                objects = new ComplexDataObject[][]{array};
            }
        } catch (InvalidPayloadException e) {
            // Do nothing here
        }
        return objects;
    }

    // Properties
    //-------------------------------------------------------------------------
    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    protected Object transform(Object[][] objects) throws ValidationException {
        Transform transformer = getTransform();
        Object[][] answer = transformer.transform(objects);
        return answer[0][0];
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected ComplexDataObject unmarshalDataObject(Exchange exchange) throws InvalidPayloadException, IOException {
        // lets try use the Sink to unmarshall it
        Transform transformer = getTransform();
        Element input = transformer.getInput(0);
        C24IOSource source = new C24IOSource(input);
        return source.parseDataObject(exchange);
    }
}