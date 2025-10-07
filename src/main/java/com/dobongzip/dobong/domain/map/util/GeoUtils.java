package com.dobongzip.dobong.domain.map.util;

public class GeoUtils {
    private static final double EARTH_RADIUS_M = 6371000.0;

    public static long haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(EARTH_RADIUS_M * c);
    }

    public static String formatDistance(long meters) {
        double km = meters / 1000.0;
        return String.format("%.1f km", km);
    }

}
