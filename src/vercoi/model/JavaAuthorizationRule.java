package vercoi.model;

public record JavaAuthorizationRule(String subject, String resource, String action, Effect effect) {
    public JavaAuthorizationRule {
        subject = subject.trim(); resource = resource.trim(); action = action.trim();
    }
    public String tupleKey() { return subject + "|" + resource + "|" + action; }
}
