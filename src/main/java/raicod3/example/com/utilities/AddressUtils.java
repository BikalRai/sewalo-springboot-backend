package raicod3.example.com.utilities;

public class AddressUtils {
    public static String generateMaskedAddress(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return "Unknown Area";
        }

        String[] parts = fullAddress.split(",");

        // If the address is incredibly short, just return it
        if (parts.length <= 2) {
            return fullAddress.trim();
        }

        // OpenStreetMap usually puts the specific street/house at index 0,
        // and the Neighborhood/Suburb at index 1.
        String neighborhood = parts[1].trim();

        return neighborhood;
    }
}
