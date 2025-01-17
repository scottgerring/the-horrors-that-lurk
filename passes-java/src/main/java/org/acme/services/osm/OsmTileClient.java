package org.acme.services.osm;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

@Path("/")
@RegisterRestClient
interface OsmTileClient {

    @GET
    @Path("{z}/{x}/{y}.png")
    @ClientHeaderParam(name = "User-Agent", value = "passes-java-demo")
    Uni<byte[]> fetchTile(@PathParam("z") int zoom,
                          @PathParam("x") int x,
                          @PathParam("y") int y);
}
