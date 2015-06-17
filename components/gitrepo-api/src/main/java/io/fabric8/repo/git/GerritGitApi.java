package io.fabric8.repo.git;

import javax.ws.rs.*;

@Path("a")
@Produces("application/json")
@Consumes("application/json")
public interface GerritGitApi {

    @POST
    @Path("projects/{repo}")
    public GerritRepositoryDTO createRepository(@PathParam("repo") String repo, CreateRepositoryDTO dto);
}
