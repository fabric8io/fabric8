/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.dosgi.util;

import java.util.Arrays;

/**
 * Helper class to hold common text/string manipulation methods.
 *  
 * @author chirino
 */
public class StringSupport {

    public static String indent(String value, int spaces) {
        if( value == null ) {
            return null;
        }
        String indent = fillString(spaces, ' ');
        return value.replaceAll("(\\r?\\n)", "$1"+indent);
    }

    public static String fillString(int count, char character) {
        char t[] = new char[count];
        Arrays.fill(t, character);
        return new String(t);
    }
    
}
