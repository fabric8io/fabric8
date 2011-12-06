/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge;

import javax.jms.JMSException;
import javax.jms.Message;
import java.io.Serializable;

/**
 * 
 * Used to transform messages flowing across the bridge. Must be serializable.
 * 
 * @author Dhiraj Bokde
 *
 */
public interface MessageConverter extends Serializable {
	
	Message convert (final Message message) throws JMSException;

}
