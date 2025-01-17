package org.acme;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.acme.services.osm.OsmImageGenerator;
import org.acme.db.Pass;

/**
 * Provides images of passes using OpenStreetMaps tile data.
 */
@Path("/api/v1/passes")
public class PassImageResource {

  @Inject
  OsmImageGenerator osmImageGenerator;
  private LongCounter getImageCalls;

  public PassImageResource(Meter meter) {
    getImageCalls = meter.counterBuilder("get-pass-image-calls")
        .setDescription("Calls made to fetch a pass image")
        .setUnit("1")
        .build();
  }

  @GET
  @Path("/{pass_id}/image")
  @Produces("image/png")
  public Uni<Response> getPassImage(
      @PathParam("pass_id") Long passId,
      @QueryParam("radius_km") @DefaultValue("0.5") double radiusKm,
      @QueryParam("size_pixels") @DefaultValue("1000") int sizePixels) {

    // Bump our metric
    getImageCalls.add(1);
    
    // Fetch the pass by ID
    return Pass.<Pass>findById(passId)
        .onItem().ifNotNull().transformToUni(pass -> {
          // Generate the image using PassImageGenerator
          return osmImageGenerator.generateImage(pass.latitude, pass.longitude, radiusKm, sizePixels)
              .onItem().transform(imageBytes -> Response.ok(imageBytes)
                  .type("image/png")
                  .build())
              // Handle any failure during image generation and return a 500 response
              .onFailure().recoverWithItem(throwable -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                  .entity("Failed to generate the image. Please try again later.")
                  .build());
        })
        // If the pass is not found, return a 404 response
        .onItem().ifNull().failWith(() -> new WebApplicationException("Pass not found", Response.Status.NOT_FOUND));
  }
}
