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

import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static io.fabric8.utils.Lists.notNullList;
import static io.fabric8.utils.jaxrs.JAXRSClients.handle404ByReturningNull;

/**
 * Default base class for a LetschatClient implementation
 */
public abstract class LetsChatClientSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(LetsChatClientSupport.class);

    protected final String address;
    protected final String username;
    protected final String password;
    protected String authToken;
    private LetsChatApi api;
    private boolean createToken = false;

    public LetsChatClientSupport(String address, String username, String password, String authToken) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.authToken = authToken;
    }

    /**
     * Tries to find the given room and if not it will auto-create a new one
     */
    public RoomDTO getOrCreateRoom(final String idOrSlug) {
        RoomDTO room = getRoom(idOrSlug);
        if (room == null) {
            room = new RoomDTO();
            room.setSlug(idOrSlug);
            room.setName(idOrSlug);
            room.setDescription("Description of room " + idOrSlug);
            Rooms.setOwner(room, getAccount());
            return createRoom(room);
        } else {
            return room;
        }
    }


    // Delegate of LetsChatApi
    //-------------------------------------------------------------------------

    @POST
    @Path("rooms")
    public RoomDTO createRoom(RoomDTO dto) {
        return getApi().createRoom(dto);
    }

    @GET
    @Path("rooms/{id}")
    public RoomDTO getRoom(final String idOrSlug) {
        return handle404ByReturningNull(new Callable<RoomDTO>() {
            @Override
            public RoomDTO call() throws Exception {
                return getApi().getRoom(idOrSlug);
            }
        });
    }

    @GET
    @Path("users/{userId}")
    public UserDTO getUser(final String id) {
        return handle404ByReturningNull(new Callable<UserDTO>() {
            @Override
            public UserDTO call() throws Exception {
                return getApi().getUser(id);
            }
        });
    }

    @GET
    @Path("account")
    public UserDTO getAccount() {
        return handle404ByReturningNull(new Callable<UserDTO>() {
            @Override
            public UserDTO call() throws Exception {
                return getApi().getAccount();
            }
        });
    }

    @GET
    @Path("rooms")
    public List<RoomDTO> getRooms() {
        List<RoomDTO> answer = handle404ByReturningNull(new Callable<List<RoomDTO>>() {
            @Override
            public List<RoomDTO> call() throws Exception {
                return getApi().getRooms();
            }
        });
        return notNullList(answer);
    }

    @GET
    @Path("rooms/{id}/users")
    public List<UserDTO> getRoomUsers(final String idOrSlug) {
        List<UserDTO> answer = handle404ByReturningNull(new Callable<List<UserDTO>>() {
            @Override
            public List<UserDTO> call() throws Exception {
                return getApi().getRoomUsers(idOrSlug);
            }
        });
        return notNullList(answer);
    }

    @PUT
    @Path("rooms/{id}")
    public void updateRoom(String idOrSlug, RoomDTO room) {
        getApi().updateRoom(idOrSlug, room);
    }

    @DELETE
    @Path("rooms/{id}")
    public void deleteRoom(String idOrSlug) {
        getApi().deleteRoom(idOrSlug);
    }


    // Properties
    //-------------------------------------------------------------------------

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    /**
     * Returns true if this client has an address and an auth token
     */
    public boolean isValid() {
        if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
            return true;
        }
        return Strings.isNotBlank(authToken);
    }

    // Implementation
    //-------------------------------------------------------------------------
    protected abstract <T> T createWebClient(Class<T> clientType);

    protected LetsChatApi getApi() {
        if (api == null) {
            api = createWebClient(LetsChatApi.class);
            if (createToken) {
                if (Strings.isNullOrBlank(authToken)) {
                    generateToken(api);
                }
            } else {
                if (Strings.isNotBlank(username) && Strings.isNotBlank(password)) {
                    // this is fine
                } else if (Strings.isNullOrBlank(authToken)) {
                    LOG.info("username: " + username + " password: " + password);
                    throw new IllegalArgumentException("No token available for letschat so cannot login. Try setting the $" + LetsChatKubernetes.LETSCHAT_HUBOT_TOKEN + " environment variable?");
                }
            }
        }
        return api;
    }

    protected void generateToken(LetsChatApi api) {
        // lets try revoke first
        try {
            api.revokeToken();
        } catch (Exception e) {
            LOG.warn("Ignored error revoking: " + e, e);
        }
        TokenDTO tokenDTO = api.generateToken();
        if (tokenDTO != null) {
            this.authToken = tokenDTO.getToken();
        }
    }
}
