package com.mediflow.service;

public final class GeoUtils {

    private GeoUtils() {}

    /** Haversine distance between two lat/lng points, in kilometers. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // earth radius km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static String googleMapsDirectionsUrl(double destLat, double destLon) {
        return "https://www.google.com/maps/dir/?api=1&destination=" + destLat + "," + destLon;
    }
}
