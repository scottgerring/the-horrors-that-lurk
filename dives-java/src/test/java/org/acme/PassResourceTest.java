package org.acme;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PassResourceTest {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @BeforeEach
    public void cleanDatabase() {
        sessionFactory.withTransaction(session -> Uni.combine().all()
                .unis(
                        // Clear tables
                        session.createNativeQuery("TRUNCATE TABLE passes CASCADE").executeUpdate(),
                        session.createNativeQuery("TRUNCATE TABLE country CASCADE").executeUpdate(),

                        // Reset the sequence
                        session.createNativeQuery("ALTER SEQUENCE passes_SEQ RESTART WITH 1").executeUpdate(),

                        // Reinsert test data for Country
                        session.createNativeQuery("INSERT INTO country (shortCode, name) VALUES ('US', 'United States')").executeUpdate(),
                        session.createNativeQuery("INSERT INTO country (shortCode, name) VALUES ('CH', 'Switzerland')").executeUpdate(),

                        // Reinsert test data for Pass using the sequence for IDs
                        session.createNativeQuery("INSERT INTO passes (id, name, ascent, country_id, latitude, longitude) VALUES (nextval('passes_SEQ'), 'Mountain Pass', 1200, 'US', 1.2, 3.4)").executeUpdate(),
                        session.createNativeQuery("INSERT INTO passes (id, name, ascent, country_id, latitude, longitude) VALUES (nextval('passes_SEQ'), 'Alpine Pass', 1800, 'CH', -5.6, -7.8)").executeUpdate()
                ).discardItems()
        ).await().indefinitely();
    }

    @Test
    public void testGetAll() {
        given()
                .when().get("/api/v1/passes")
                .then()
                .statusCode(200)
                .body("$.size()", is(2))
                .body("[0].name", is("Mountain Pass"))
                .body("[0].id", is(1))
                .body("[1].country", is("Switzerland"))
                .body("[1].id", is(2));
    }

    @Test
    public void testGetById() {
        given()
                .when().get("/api/v1/passes/1")
                .then()
                .statusCode(200)
                .body("name", is("Mountain Pass"))
                .body("country", is("United States"));
    }

    @Test
    public void testCreatePass() {
        // Step 1: Create the Pass
        Integer passId = given()
                .contentType("application/json")
                .body("{\"name\":\"New Pass\",\"ascent\":1500,\"country\":\"United States\"}")
                .when().post("/api/v1/passes")
                .then()
                .statusCode(201)
                .body("name", is("New Pass"))
                .body("country", is("United States"))
                .body("ascent", is(1500))
                .extract().path("id"); // Extract the ID of the created pass

        // Step 2: Retrieve the Pass by ID and verify it
        given()
                .when().get("/api/v1/passes/" + passId)
                .then()
                .statusCode(200)
                .body("id", is(passId))
                .body("name", is("New Pass"))
                .body("country", is("United States"))
                .body("ascent", is(1500));
    }

    @Test
    public void testUpdatePass() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"Updated Pass\",\"ascent\":1600,\"country\":\"Switzerland\"}")
                .when().put("/api/v1/passes/2")
                .then()
                .statusCode(200)
                .body("name", is("Updated Pass"))
                .body("country", is("Switzerland"))
                .body("ascent", is(1600));
    }

    @Test
    public void testDeletePass() {
        given()
                .when().delete("/api/v1/passes/1")
                .then()
                .statusCode(200)
                .body(is("Deleted"));

        // Verify deletion
        given()
                .when().get("/api/v1/passes/1")
                .then()
                .statusCode(404);
    }
}
