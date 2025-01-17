package org.acme.services.osm;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;


@ApplicationScoped
public class OsmImageGenerator {

    private static final int TILE_SIZE = 256;
    private static final Logger LOG = Logger.getLogger(OsmImageGenerator.class);

    private final Tracer tracer;

    public OsmImageGenerator(Tracer tracer) {
        this.tracer = tracer;
    }

    @RestClient
    OsmTileClient osmTileClient;

    /**
     * Generates an aerial image of a given area based on latitude, longitude, radius, and image size.
     *
     * @param lat latitude of the center point.
     * @param lon longitude of the center point.
     * @param radiusKm radius of the area in kilometers.
     * @param sizePixels size of the output image in pixels.
     * @return A `Uni<byte[]>` containing the generated image as a PNG byte array.
     */
    @WithSpan
    public Uni<byte[]> generateImage(double lat, double lon, double radiusKm, int sizePixels) {
        LOG.info("Starting image generation...");

        LatLong center = new LatLong(lat, lon);

        return Uni.createFrom().item(() -> computeBoundingBox(center, radiusKm, sizePixels))
                .flatMap(tileBox -> fetchTilesAsync(tileBox)
                        .flatMap(tiles -> assembleImage(tileBox, tiles, sizePixels)));
    }

    protected ConstrainedTileBox computeBoundingBox(LatLong center, double radiusKm, int imageSizePx) {
        int zoom = computeZoomLevel(radiusKm, imageSizePx);
        TileCoordinate centerTile = latLongToTileCoords(center, zoom);

        double radiusTiles = radiusKm / tileSizeKm(zoom);

        TileCoordinate topLeft = new TileCoordinate(
                centerTile.x - radiusTiles,  // No need for Math.floor here
                centerTile.y - radiusTiles,
                zoom
        );
        TileCoordinate bottomRight = new TileCoordinate(
                centerTile.x + radiusTiles,  // No need for Math.ceil here
                centerTile.y + radiusTiles,
                zoom
        );

        return new ConstrainedTileBox(center, new TileBox(topLeft, bottomRight), new int[] { imageSizePx, imageSizePx });
    }

    private Uni<Map<TileCoordinate, BufferedImage>> fetchTilesAsync(ConstrainedTileBox tileBox) {
        Set<TileCoordinate> coordinates = generateTileCoordinates(tileBox.getTileBox());

        // Fetch tiles in parallel
        return Multi.createFrom().items(coordinates.stream())
                .onItem().transformToUniAndMerge(tile -> osmTileClient.fetchTile(tile.z, (int) tile.x, (int) tile.y)
                        .onItem().transform(tileBytes -> {
                            try {
                                return Map.entry(tile, ImageIO.read(new ByteArrayInputStream(tileBytes)));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to decode tile image", e);
                            }
                        }))
                .collect().asMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private Uni<byte[]> assembleImage(ConstrainedTileBox tileBox, Map<TileCoordinate, BufferedImage> tiles, int sizePixels) {
        return Uni.createFrom().emitter(emitter -> {
            Span span = null;
            try {
                span = tracer != null? tracer.spanBuilder("assembleImage").startSpan() : null;

                TileBox box = tileBox.getTileBox();
                int imgWidth = TILE_SIZE * (int) (box.bottomRight.x - box.topLeft.x);
                int imgHeight = TILE_SIZE * (int) (box.bottomRight.y - box.topLeft.y);

                BufferedImage fullImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = fullImage.createGraphics();

                for (Map.Entry<TileCoordinate, BufferedImage> entry : tiles.entrySet()) {
                    TileCoordinate coord = entry.getKey();
                    BufferedImage tile = entry.getValue();
                    int xOffset = (int) ((coord.x - box.topLeft.x) * TILE_SIZE);
                    int yOffset = (int) ((coord.y - box.topLeft.y) * TILE_SIZE);
                    graphics.drawImage(tile, xOffset, yOffset, null);
                }
                graphics.dispose();

                // Center and crop to requested size
                int cropX = imgWidth / 2 - sizePixels / 2;
                int cropY = imgHeight / 2 - sizePixels / 2;
                BufferedImage cropped = fullImage.getSubimage(cropX, cropY, sizePixels, sizePixels);

                // Convert to PNG byte array
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(cropped, "png", outputStream);

                // Complete the Uni with the generated byte array
                emitter.complete(outputStream.toByteArray());

                if (span != null) span.end();

            } catch (IOException e) {
                // Fail the Uni if an exception occurs
                emitter.fail(new RuntimeException("Failed to assemble image", e));

                // Make sure we stop the span even if things go bad
                if (span != null && span.isRecording()) {
                    span.end();
                }
            }
        });
    }

    private int computeZoomLevel(double radiusKm, int imageSizePx) {
        for (int zoom = 0; zoom <= 21; zoom++) {
            double tileSizeKm = tileSizeKm(zoom);
            double tileCount = radiusKm / tileSizeKm;
            if (tileCount * TILE_SIZE > imageSizePx) {
                return zoom;
            }
        }
        throw new IllegalStateException("Unable to find suitable zoom level");
    }

    private double tileSizeKm(int zoom) {
        double earthCircumferenceKm = 2 * Math.PI * 6371;
        return earthCircumferenceKm / Math.pow(2, zoom);
    }

    protected Set<TileCoordinate> generateTileCoordinates(TileBox box) {
        Set<TileCoordinate> coords = new HashSet<>();
        for (int x = (int) Math.floor(box.topLeft.x); x <= (int) Math.ceil(box.bottomRight.x); x++) {
            for (int y = (int) Math.floor(box.topLeft.y); y <= (int) Math.ceil(box.bottomRight.y); y++) {
                coords.add(new TileCoordinate(x, y, box.topLeft.z));
            }
        }
        return coords;
    }

    protected TileCoordinate latLongToTileCoords(LatLong point, int zoom) {
        double latRad = Math.toRadians(point.lat);
        double n = Math.pow(2, zoom);
        double xTile = (point.lon + 180) / 360 * n;
        double yTile = (1 - (Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI)) / 2 * n;

        // Keep the fractional values
        return new TileCoordinate(xTile, yTile, zoom);
    }

    static class LatLong {
        public final double lat;
        public final double lon;

        public LatLong(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    protected static class TileCoordinate {
        public final double x;
        public final double y;
        public final int z;

        public TileCoordinate(double x, double y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TileCoordinate that = (TileCoordinate) o;
            return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    static class TileBox {
        public final TileCoordinate topLeft;
        public final TileCoordinate bottomRight;

        public TileBox(TileCoordinate topLeft, TileCoordinate bottomRight) {
            this.topLeft = topLeft;
            this.bottomRight = bottomRight;
        }
    }

    static class ConstrainedTileBox {
        private final LatLong center;
        private final TileBox tileBox;
        private final int[] innerSizePx;

        public ConstrainedTileBox(LatLong center, TileBox tileBox, int[] innerSizePx) {
            this.center = center;
            this.tileBox = tileBox;
            this.innerSizePx = innerSizePx;
        }

        public LatLong getCenter() {
            return center;
        }

        public TileBox getTileBox() {
            return tileBox;
        }

        public int[] getInnerSizePx() {
            return innerSizePx;
        }
    }
}
