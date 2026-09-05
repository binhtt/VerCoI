package vercoi.model;

public record ComplianceViolation(Type type, String subject, String resource, String action,
                                  Effect expected, Effect actual, String detail) {
    public enum Type { MISSING_RULE, EFFECT_MISMATCH, UNEXPECTED_RULE, IMPLEMENTATION_CONFLICT }
}
