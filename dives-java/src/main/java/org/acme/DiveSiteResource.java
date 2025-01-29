package org.acme;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.acme.db.Country;
import org.acme.db.DiveSite;
import org.acme.dto.DiveSiteDTO;

import com.horror.TagLibrary;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
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

@Path("/api/v1/divesites")
public class DiveSiteResource {

    LongCounter apiCallCounter;

    public DiveSiteResource(Meter meter) {
        apiCallCounter = meter.counterBuilder("get-pass-calls")
                .setDescription("Calls made to fetch from the pass APIs")
                .setUnit("invocations")
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getAll() {
        apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "getPasses"));

        return DiveSite.<DiveSite>findAll().list()
                .map(diveSites -> diveSites.stream()
                        .map(diveSite -> new DiveSiteDTO(diveSite.id, diveSite.name, diveSite.country.getName(), diveSite.descent, diveSite.latitude,
                                diveSite.longitude))
                        .collect(Collectors.toList()))
                .map(diveSiteDTOs -> withTagHeaders(Response.ok(diveSiteDTOs)).build());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getDiveSite(@PathParam("id") Long id) {
        apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "getPass"));

        return DiveSite.<DiveSite>findById(id)
                .onItem().ifNotNull()
                .transform(diveSite -> withTagHeaders(Response.ok(
                        new DiveSiteDTO(diveSite.id, diveSite.name, diveSite.country.getName(), diveSite.descent, diveSite.latitude, diveSite.longitude)
                )).build())
                .onItem().ifNull()
                .failWith(() -> new WebApplicationException("Pass not found", Response.Status.NOT_FOUND));
    }

    @POST
    public Uni<Response> createDiveSite(DiveSiteDTO diveSiteDTO) {
        apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "postPass"));

        return Country.find("name", diveSiteDTO.country).firstResult()
                .onItem().ifNotNull().transformToUni(country -> {
                    DiveSite diveSite = new DiveSite();
                    diveSite.name = diveSiteDTO.name;
                    diveSite.descent = diveSiteDTO.descent;
                    diveSite.country = (Country) country;

                    return Panache.withTransaction(() -> diveSite.persistAndFlush()
                                    .replaceWith(diveSite)) // Ensure transaction completes
                            .onItem().transform(savedDiveSite -> {
                                if (savedDiveSite.id == null) {
                                    throw new WebApplicationException("ID was not generated", Response.Status.INTERNAL_SERVER_ERROR);
                                }
                                return withTagHeaders(Response.status(Response.Status.CREATED)
                                        .entity(new DiveSiteDTO(savedDiveSite.id, savedDiveSite.name, savedDiveSite.country.getName(), savedDiveSite.descent, savedDiveSite.latitude, savedDiveSite.longitude)))
                                        .build();
                            });
                })
                .onItem().ifNull()
                .failWith(() -> new WebApplicationException("Country not found", Response.Status.BAD_REQUEST));
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> updateDiveSite(@PathParam("id") Long id, DiveSiteDTO diveSiteDTO) {
        apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "pustPass"));

        return DiveSite.<DiveSite>findById(id)
                .onItem().ifNotNull().transformToUni(diveSite ->
                        Country.find("name", diveSiteDTO.country).firstResult()
                                .onItem().ifNotNull().transformToUni(country -> {
                                    diveSite.name = diveSiteDTO.name;
                                    diveSite.descent = diveSiteDTO.descent;
                                    diveSite.country = (Country) country;

                                    return Panache.withTransaction(() -> diveSite.persist()
                                            .replaceWith(withTagHeaders(Response.ok(
                                                    new DiveSiteDTO(diveSite.id, diveSite.name, diveSite.country.getName(), diveSite.descent, diveSite.latitude, diveSite.longitude)
                                            )).build()));
                                })
                                .onItem().ifNull()
                                .failWith(() -> new WebApplicationException("Country not found", Response.Status.BAD_REQUEST)))
                .onItem().ifNull()
                .failWith(() -> new WebApplicationException("Pass not found", Response.Status.NOT_FOUND));
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{id}")
    public Uni<Response> deletePass(@PathParam("id") Long id) {
        apiCallCounter.add(1, Attributes.of(AttributeKey.stringKey("api"), "deletePass"));

        return Panache.withTransaction(() ->
                DiveSite.deleteById(id)
                        .onItem().transform(deleted ->
                                deleted
                                        ? withTagHeaders(Response.ok("Deleted")).build()
                                        : withTagHeaders(Response.status(Response.Status.NOT_FOUND).entity("Pass not found")).build()
                        )
        );
    }

    private Response.ResponseBuilder withTagHeaders(Response.ResponseBuilder responseBuilder) {
        TagLibrary t = new TagLibrary();
        Map<String, String> tags = t.getTags();

        if (tags != null && !tags.isEmpty()) {
            String headerValue = tags.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            responseBuilder.header("X-Dive-Tags", headerValue);
        }

        return responseBuilder;
    }
}
