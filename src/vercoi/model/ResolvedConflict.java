package vercoi.model;

public record ResolvedConflict(Conflict conflict, CombiningAlgorithm algorithm, Effect resolvedEffect) { }
