package vercoi.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class PolicyRule {
    private final String ruleId;
    private final String subjectAttributeId;
    private final String subject;
    private final String resource;
    private final LinkedHashSet<String> actions;
    private final Effect effect;

    public PolicyRule(String ruleId, String subjectAttributeId, String subject,
                      String resource, Collection<String> actions, Effect effect) {
        this.ruleId = require(ruleId, "ruleId");
        this.subjectAttributeId = require(subjectAttributeId, "subjectAttributeId");
        this.subject = require(subject, "subject");
        this.resource = require(resource, "resource");
        this.actions = new LinkedHashSet<>();
        if (actions != null) for (String a : actions) if (a != null && !a.trim().isEmpty()) this.actions.add(a.trim());
        if (this.actions.isEmpty()) throw new IllegalArgumentException("A rule must contain at least one action");
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    private static String require(String s, String name) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return s.trim();
    }

    public String ruleId() { return ruleId; }
    public String subjectAttributeId() { return subjectAttributeId; }
    public String subject() { return subject; }
    public String resource() { return resource; }
    public Set<String> actions() { return Set.copyOf(actions); }
    public Effect effect() { return effect; }

    public boolean actionsOverlap(PolicyRule other) {
        for (String a : actions) if (other.actions.contains(a)) return true;
        return false;
    }

    public boolean conflictsWith(PolicyRule other) {
        return subjectAttributeId.equals(other.subjectAttributeId)
            && subject.equals(other.subject)
            && resource.equals(other.resource)
            && actionsOverlap(other)
            && effect != other.effect;
    }

    public boolean structurallyEquals(PolicyRule other) {
        return subjectAttributeId.equals(other.subjectAttributeId)
            && subject.equals(other.subject)
            && resource.equals(other.resource)
            && actions.equals(other.actions)
            && effect == other.effect;
    }

    public String normalizedKey() {
        return subjectAttributeId + "|" + subject + "|" + resource + "|"
                + actions.stream().sorted().reduce((a,b)->a+","+b).orElse("") + "|" + effect;
    }

    @Override public String toString() {
        return ruleId + " [" + subjectAttributeId + "=" + subject + ", resource=" + resource
                + ", actions=" + actions + ", effect=" + effect + "]";
    }
}
