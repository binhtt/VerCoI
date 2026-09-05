package vercoi.model;

import java.util.Set;

public record Conflict(PolicyRule left, PolicyRule right, Set<String> overlappingActions) { }
