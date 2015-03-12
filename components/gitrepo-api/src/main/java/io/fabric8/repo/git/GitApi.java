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
package io.fabric8.repo.git;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

/**
 * REST API for working with git hosted repositories using back ends like
 * <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>
 */
@Path("api/v1")
@Produces("application/json")
@Consumes("application/json")
public interface GitApi {

    @GET
    @Path("user/repos")
    public List<RepositoryDTO> listRepositories();


    @POST
    @Path("user/repos")
    public RepositoryDTO createRepository(CreateRepositoryDTO dto);

}
