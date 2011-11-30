/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.security.sso.activemq;

import java.security.Principal;

/**
 *
 */
public class TokenPrincipal implements Principal {

    private final String token;
    private transient int hash;

    public TokenPrincipal(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token cannot be null");
        }
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TokenPrincipal that = (TokenPrincipal)o;

        if (!token.equals(that.token)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return token;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = token.hashCode();
        }
        return hash;
    }

    @Override
    public String getName() {
        return token;
    }
}
