/*
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
package org.fusesource.process.manager;

import org.fusesource.process.manager.support.command.CommandFailedException;

import java.io.IOException;
import java.io.Serializable;

/**
 * Controls a process
 */
public interface ProcessController extends Serializable {
    int install() throws InterruptedException, IOException, CommandFailedException;
    int uninstall();

    int start() throws Exception;
    int stop() throws Exception;
    int kill() throws Exception;
    int restart() throws Exception;
    int status() throws Exception;
    int configure() throws Exception;

    Integer getPid() throws IOException;
}
