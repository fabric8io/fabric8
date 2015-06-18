package io.fabric8.gerrit;

import javax.ws.rs.*;

@Path("a")
@Produces("application/json")
@Consumes("application/json")
public interface GitApi {

    @GET
    @Path("projects/{repo}")
    public ProjectInfoDTO getRepository(@PathParam("repo") String repo);

    @POST
    @Path("projects/{repo}")
    public RepositoryDTO createRepository(@PathParam("repo") String repo, CreateRepositoryDTO dto);
}