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
package io.fabric8.agent.mvn;

import java.util.Dictionary;

/**
 * Resolves properties from a Dictionary.
 *
 * @author Alin Dreghiciu
 * @since 0.5.0, January 16, 2008
 */
public class DictionaryPropertyResolver
    extends FallbackPropertyResolver
{

    /**
     * Dictionary to resolve properties from. Can be null.
     */
    private Dictionary m_properties;

    /**
     * Creates a property resolver without a fallback resolver.
     *
     * @param properties dictionary; optional
     */
    public DictionaryPropertyResolver( final Dictionary properties )
    {
        this( properties, null );
    }

    /**
     * Creates a property resolver with a fallback resolver.
     *
     * @param properties       dictionary; optional
     * @param fallbackResolver fallback resolver
     */
    public DictionaryPropertyResolver( final Dictionary properties,
                                       final PropertyResolver fallbackResolver )
    {
        super( fallbackResolver );
        m_properties = properties;
    }

    /**
     * Sets the properties in use.
     *
     * @param properties dictionary of properties
     */
    public void setProperties( final Dictionary properties )
    {
        m_properties = properties;
    }

    /**
     * Resolves a property based on it's name .
     *
     * @param propertyName property name to be resolved
     *
     * @return value of property or null if property is not set or is empty.
     */
    public String findProperty( final String propertyName )
    {
        String value = null;
        if( m_properties != null )
        {
            value = (String) m_properties.get( propertyName );
        }
        if( value != null && value.trim().length() == 0 )
        {
            value = null;
        }
        return value;
    }

}
