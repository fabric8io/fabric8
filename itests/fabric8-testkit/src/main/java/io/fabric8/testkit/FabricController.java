/**
 *
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
package io.fabric8.testkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Represents a Strategy for creating a fabric; typically using some kind of remote process.
 */
public interface FabricController {


    /**
     * Creates a fabric and asserts that it gets created within an allowable time then returns the REST API to interact with it
     */
    FabricRestApi createFabric() throws Exception;

    /**
     * Destroys the fabric
     */
    void destroy() throws Exception;

    File getWorkDirectory();

    void setWorkDirectory(File workDir);
}
