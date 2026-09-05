package vercoi.model;

import java.util.List;

public record ConflictAnalysis(List<Conflict> conflicts, List<ResolvedConflict> resolutions) {
    public ConflictAnalysis {
        conflicts = List.copyOf(conflicts);
        resolutions = List.copyOf(resolutions);
    }
}
