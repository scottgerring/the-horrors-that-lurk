package org.acme.services.osm;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.wildfly.common.Assert.assertTrue;

public class OsmImageGeneratorTest extends OsmImageGenerator {

    private static final double DELTA = 0.001; // Approximation margin

    public OsmImageGeneratorTest()  {
        super(null);
    }

    @Test
    void testLatLongToTileCoords_ZeroZoom() {
        OsmImageGenerator.LatLong latLong = new LatLong(-31.0, 115.0);
        int zoom = 0;

        OsmImageGenerator.TileCoordinate tileCoordinate = latLongToTileCoords(latLong, zoom);

        System.out.printf("x: %f, y: %f%n", tileCoordinate.x, tileCoordinate.y);

        assertEquals(0.8194444, tileCoordinate.x, DELTA);
        assertEquals(0.5906487, tileCoordinate.y, DELTA);
    }

    @Test
    void testLatLongToTileCoords_Perth() {
        OsmImageGenerator.LatLong latLong = new LatLong(-31.9514, 115.8617);
        int zoom = 12;

        OsmImageGenerator.TileCoordinate tileCoordinate = new OsmImageGenerator(null)
                .latLongToTileCoords(latLong, zoom);

        assertEquals(3366.2486, tileCoordinate.x, DELTA);
        assertEquals(2431.9897, tileCoordinate.y, DELTA);
        assertEquals(zoom, tileCoordinate.z);
    }

    @Test
    void testLatLongToTileCoords_Thun() {
        OsmImageGenerator.LatLong latLong = new LatLong(46.7580, 7.6280);
        int zoom = 14;

        OsmImageGenerator.TileCoordinate tileCoordinate = latLongToTileCoords(latLong, zoom);

        assertEquals(8539.1587, tileCoordinate.x, DELTA);
        assertEquals(5778.7951, tileCoordinate.y, DELTA);
        assertEquals(zoom, tileCoordinate.z);
    }

    @Test
    void testComputeBoundingBox_Perth() {
        LatLong latLong = new OsmImageGenerator.LatLong(-31.9514, 115.8617);
        double radiusKm = 10.0;
        int imageSizePx = 1000;

        ConstrainedTileBox tileBox = computeBoundingBox(latLong, radiusKm, imageSizePx);

        TileCoordinate topLeft = tileBox.getTileBox().topLeft;
        TileCoordinate bottomRight = tileBox.getTileBox().bottomRight;

        assertEquals(13460.902, topLeft.x, DELTA);
        assertEquals(9723.866, topLeft.y, DELTA);
        assertEquals(13469.088, bottomRight.x, DELTA);
        assertEquals(9732.052, bottomRight.y, DELTA);

    }

    @Test
    void testGenerateTileCoordinates() {
        TileCoordinate topLeft = new TileCoordinate(3366.5, 2431.5, 12);
        TileCoordinate bottomRight = new TileCoordinate(3367.1, 2432.0, 12);

        TileBox tileBox = new TileBox(topLeft, bottomRight);
        Set<TileCoordinate> coordinates = generateTileCoordinates(tileBox);

        assertTrue(coordinates.contains(new TileCoordinate(3366.0, 2431.0, 12)));
        assertTrue(coordinates.contains(new TileCoordinate(3367.0, 2431.0, 12)));
        assertTrue(coordinates.contains(new TileCoordinate(3366.0, 2432.0, 12)));
        assertTrue(coordinates.contains(new TileCoordinate(3367.0, 2432.0, 12)));
        assertTrue(coordinates.contains(new TileCoordinate(3368.0, 2431.0, 12)));
        assertTrue(coordinates.contains(new TileCoordinate(3368.0, 2432.0, 12)));

        assertEquals(6, coordinates.size());
    }


}
