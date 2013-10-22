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
package org.fusesource.gateway.chooser;

import org.fusesource.gateway.ServiceDetails;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.List;

/**
 * A default implementation of {@link org.fusesource.gateway.chooser.HttpChooser}
 * which delegates to a {@link Chooser}
 */
public class DefaultHttpChooser implements HttpChooser {
    private final Chooser<ServiceDetails> chooser;

    public DefaultHttpChooser() {
        this(new RandomChooser<ServiceDetails>());
    }

    public DefaultHttpChooser(Chooser<ServiceDetails> chooser) {
        this.chooser = chooser;
    }

    @Override
    public ServiceDetails chooseService(HttpServerRequest request, List<ServiceDetails> services) {
        return chooser.choose(services);
    }
}
