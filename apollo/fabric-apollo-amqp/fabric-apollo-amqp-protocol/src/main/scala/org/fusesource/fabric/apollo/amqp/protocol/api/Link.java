/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol.api;

import org.fusesource.fabric.apollo.amqp.codec.interfaces.Source;
import org.fusesource.fabric.apollo.amqp.codec.interfaces.Target;
import org.fusesource.fabric.apollo.amqp.codec.types.Role;

/**
 *
 */
public interface Link {

    public String getName();

    public void setName(String name);

    public Role getRole();

    public void setMaxMessageSize(long size);

    public long getMaxMessageSize();

    public void setSource(Source source);

    public void setTarget(Target target);

    public Source getSource();

    public Target getTarget();

    public void onAttach(Runnable task);

    public void onDetach(Runnable task);

    public boolean established();

    public Session getSession();

}
