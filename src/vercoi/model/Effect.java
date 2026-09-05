package vercoi.model;

public enum Effect {
    Permit, Deny;

    public static Effect parse(String value) {
        for (Effect e : values()) if (e.name().equalsIgnoreCase(value.trim())) return e;
        throw new IllegalArgumentException("Effect must be Permit or Deny: " + value);
    }
}
