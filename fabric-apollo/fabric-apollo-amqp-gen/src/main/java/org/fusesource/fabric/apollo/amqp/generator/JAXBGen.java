/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.generator;

public class JAXBGen {

	public static final void main(String [] args)
	{
		//TODO perhaps add make this a util for generating jaxb.schema?
	    //xjc -dtd amqp.dtd -verbose -d generated -p org.fusesource.fabric.apollo.amqp.jaxb.schema -target 2.0
	}
}
