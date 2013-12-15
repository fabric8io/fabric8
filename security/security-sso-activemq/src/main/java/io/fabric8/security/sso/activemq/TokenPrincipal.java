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

package io.fabric8.security.sso.activemq;

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
