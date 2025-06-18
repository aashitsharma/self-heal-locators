package onemg.analytics.dump.enums;

public enum VerticalService {
    UNIFIED_CART("unifiedcart"),
    INVENTORY_VENDOR_MANAGEMENT("ivm"),
    PHARMACY("pharmacy"),
    LABS("labs");

    private final String value;

    VerticalService(String valueEnum){
        this.value=valueEnum;
    }

    public String getValue(){
        return this.value;
    }

    // Static method to fetch enum from string
    public static VerticalService fromValue(String value) {
        for (VerticalService vs : VerticalService.values()) {
            if (vs.getValue().equalsIgnoreCase(value)) {
                return vs;
            }
        }
        throw new IllegalArgumentException("No enum constant with value: " + value);
    }

    // Static method to check wheter enum is valid
    public static boolean isValidVertical(String value){
        boolean isValid = false;
         for (VerticalService vs : VerticalService.values()) {
            if (vs.getValue().equalsIgnoreCase(value)) {
                isValid =true;
            }
        }
        return isValid;
    }
}
