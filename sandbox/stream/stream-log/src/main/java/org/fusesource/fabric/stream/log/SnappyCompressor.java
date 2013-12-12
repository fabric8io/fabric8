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
package io.fabric8.stream.log;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import static io.fabric8.stream.log.Support.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class SnappyCompressor implements Processor {

    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        if (in.getBody() != null) {
            byte[] data = in.getMandatoryBody(byte[].class);
            byte[] compressed = compress(data);

            if (exchange.getPattern().isOutCapable()) {
                Message out = exchange.getOut();
                out.copyFrom(in);
                out.setBody(compressed);
            } else {
                in.setBody(compressed);
            }
        }
    }


}
