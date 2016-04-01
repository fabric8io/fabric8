/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.letschat;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

/**
 * REST API for working with <a href="http://sdelements.github.io/lets-chat/">Letschat</a> or <a href="http://github.com/">github</a>
 */
@Path("/")
@Produces("application/json")
@Consumes("application/json")
public interface LetsChatApi {

    // Rooms
    //-------------------------------------------------------------------------

    @GET
    @Path("rooms")
    public List<RoomDTO> getRooms();

    @GET
    @Path("rooms/{id}")
    public RoomDTO getRoom(@PathParam("id") String idOrSlug);

    @DELETE
    @Path("rooms/{id}")
    public void deleteRoom(@PathParam("id") String idOrSlug);

    @PUT
    @Path("rooms/{id}")
    public void updateRoom(@PathParam("id") String idOrSlug, RoomDTO room);

    @POST
    @Path("rooms")
    public RoomDTO createRoom(RoomDTO dto);

    // Users
    //-------------------------------------------------------------------------

    @GET
    @Path("rooms/{id}/users")
    public List<UserDTO> getRoomUsers(@PathParam("id") String idOrSlug);


    @GET
    @Path("account")
    public UserDTO getAccount();

    @GET
    @Path("users/{userId}")
    public UserDTO getUser(@PathParam("userId") String id);


    // Authenticate
    //-------------------------------------------------------------------------
    @POST
    @Path("account/token/generate")
    public TokenDTO generateToken();

    @POST
    @Path("account/token/revoke")
    public void revokeToken();
}
