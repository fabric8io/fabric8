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
package org.fusesource.gateway.handlers.http.rule;

/**
 * The proxy command to perform
 */
public class ProxyCommand {
    private final ProxyOperation operation;
    private final String url;

    public ProxyCommand(ProxyOperation operation, String url) {
        this.operation = operation;
        this.url = url;
    }

    @Override
    public String toString() {
        return "ProxyCommand{" +
                "operation=" + operation +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProxyCommand that = (ProxyCommand) o;

        if (operation != that.operation) return false;
        if (!url.equals(that.url)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = operation.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    public ProxyOperation getOperation() {
        return operation;
    }

    public String getUrl() {
        return url;
    }
}
