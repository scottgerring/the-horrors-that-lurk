package org.acme;

import java.util.List;
import java.util.stream.Collectors;

import org.acme.db.Country;
import org.acme.db.Pass;
import org.acme.dto.PassDTO;
import org.acme.events.model.PassEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;

import com.horror.TagLibrary;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/passes")
public class PassResource {

  @Channel("passes")
  MutinyEmitter<PassEvent> passEventEmitter;

  LongCounter apiCallCounter;

  public PassResource(Meter meter) {
    apiCallCounter = meter.counterBuilder("get-pass-calls")
        .setDescription("Calls made to fetch from the pass APIs")
        .setUnit("invocations")
        .build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<List<PassDTO>> getAll() {

    // Get tags 
    TagLibrary t = new TagLibrary();
    System.out.println(t.getTags());
    
    apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "getPasses"));
    
    return Pass.<Pass>findAll().list()
        .map(passes -> passes.stream()
            .map(pass -> new PassDTO(pass.id, pass.name, pass.country.getName(), pass.ascent, pass.latitude,
                pass.longitude))
            .collect(Collectors.toList()));
  }

  @GET
  @Path("/{id}")
  public Uni<PassDTO> getPass(@PathParam("id") Long id) {

    apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "getPass"));
    
    return Pass.<Pass>findById(id)
        .onItem().ifNotNull()
        .transform(
            pass -> new PassDTO(pass.id, pass.name, pass.country.getName(), pass.ascent, pass.latitude, pass.longitude))
        .onItem().ifNull().failWith(() -> new WebApplicationException("Pass not found", Response.Status.NOT_FOUND));
  }

  @POST
  public Uni<Response> createPass(PassDTO passDTO) {
    
    apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "postPass"));
    
    return Country.find("name", passDTO.country).firstResult()
        .onItem().ifNotNull().transformToUni(country -> {
          Pass pass = new Pass();
          pass.name = passDTO.name;
          pass.ascent = passDTO.ascent;
          pass.country = (Country) country;

          return Panache.withTransaction(() -> pass.persistAndFlush()
              .onItem()
              .transformToUni(saved -> emitEvent(PassEvent.EventType.Created, pass.id, pass.name,
                  pass.country.getName(), pass.ascent)
                  .replaceWith(Response.status(Response.Status.CREATED)
                      .entity(new PassDTO(pass.id, pass.name, pass.country.getName(), pass.ascent, pass.latitude,
                          pass.longitude))
                      .build())));
        })
        .onItem().ifNull()
        .failWith(() -> new WebApplicationException("Country not found", Response.Status.BAD_REQUEST));
  }

  @PUT
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> updatePass(@PathParam("id") Long id, PassDTO passDTO) {

    apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "pustPass"));

    return Pass.<Pass>findById(id)
        .onItem().ifNotNull().transformToUni(pass -> Country.find("name", passDTO.country).firstResult()
            .onItem().ifNotNull().transformToUni(country -> {
              pass.name = passDTO.name;
              pass.ascent = passDTO.ascent;
              pass.country = (Country) country;

              return Panache.withTransaction(() -> pass.persist()
                  .onItem()
                  .transformToUni(updated -> emitEvent(PassEvent.EventType.Updated, pass.id, pass.name,
                      pass.country.getName(), pass.ascent)
                      .replaceWith(Response.ok(
                          new PassDTO(pass.id, pass.name, pass.country.getName(), pass.ascent, pass.latitude,
                              pass.longitude))
                          .build())));
            })
            .onItem().ifNull()
            .failWith(() -> new WebApplicationException("Country not found", Response.Status.BAD_REQUEST)))
        .onItem().ifNull().failWith(() -> new WebApplicationException("Pass not found", Response.Status.NOT_FOUND));
  }

  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/{id}")
  public Uni<Response> deletePass(@PathParam("id") Long id) {
    
    apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "deletePass"));
    
    return Panache.withTransaction(() -> Pass.deleteById(id)
        .onItem().ifNotNull().transformToUni(deleted -> {
          if (deleted) {
            return emitEvent(PassEvent.EventType.Deleted, id, null, null, 0)
                .replaceWith(Response.ok("Deleted").build());
          } else {
            return Uni.createFrom().item(
                Response.status(Response.Status.NOT_FOUND)
                    .entity("Pass not found")
                    .build());
          }
        }));
  }

  private Uni<Void> emitEvent(PassEvent.EventType et, long id, String name, String countryName, int ascent) {
    PassEvent event = new PassEvent(id, name, countryName, ascent, et);
    return passEventEmitter.send(event);
  }
}
