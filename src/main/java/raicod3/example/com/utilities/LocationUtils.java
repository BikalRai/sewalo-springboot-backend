package raicod3.example.com.utilities;

public class LocationUtils {

    private static final int EARTH_RADIUS_KM = 6371;

    public static Double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if(lat1 == null || lng1 == null || lat2 == null || lng2 == null){
            return null;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;
        return Math.round(distance * 100.0) / 100.0;
    }
}
