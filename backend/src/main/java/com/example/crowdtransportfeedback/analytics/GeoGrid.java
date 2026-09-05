package com.example.crowdtransportfeedback.analytics;

/** Deterministic 250 m Web-Mercator grid (EPSG:3857). Distances become distorted near the poles. */
public final class GeoGrid {
    public static final double CELL_METERS = 250.0;
    private static final double R = 6378137.0;
    private GeoGrid() {}

    public record Cell(String id, long x, long y, double centerLatitude, double centerLongitude) {}

    public static Cell cell(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -85.05112878 || latitude > 85.05112878 || longitude < -180 || longitude > 180) return null;
        double mx = R * Math.toRadians(longitude);
        double my = R * Math.log(Math.tan(Math.PI / 4 + Math.toRadians(latitude) / 2));
        long x = (long) Math.floor(mx / CELL_METERS), y = (long) Math.floor(my / CELL_METERS);
        double cx = (x + .5) * CELL_METERS, cy = (y + .5) * CELL_METERS;
        return new Cell(x + ":" + y, x, y, Math.toDegrees(2 * Math.atan(Math.exp(cy / R)) - Math.PI / 2), Math.toDegrees(cx / R));
    }

    public static Cell fromId(String id) {
        try {
            String[] p = id.split(":", -1); if (p.length != 2) return null;
            long x = Long.parseLong(p[0]), y = Long.parseLong(p[1]);
            double cx = (x + .5) * CELL_METERS, cy = (y + .5) * CELL_METERS;
            return new Cell(id, x, y, Math.toDegrees(2 * Math.atan(Math.exp(cy / R)) - Math.PI / 2), Math.toDegrees(cx / R));
        } catch (RuntimeException ignored) { return null; }
    }
}
