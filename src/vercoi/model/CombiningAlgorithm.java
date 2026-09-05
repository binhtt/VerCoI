package vercoi.model;

public enum CombiningAlgorithm {
    DENY_OVERRIDES, PERMIT_OVERRIDES, FIRST_APPLICABLE;

    public static CombiningAlgorithm parse(String value) {
        String v = value == null ? "" : value.toLowerCase();
        if (v.contains("deny-overrides")) return DENY_OVERRIDES;
        if (v.contains("permit-overrides")) return PERMIT_OVERRIDES;
        return FIRST_APPLICABLE;
    }
}
