package vercoi.parser;

import vercoi.model.Effect;
import vercoi.model.PolicyRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Parser for VerCoI's controlled textual/tabular input. */
public final class ControlledTextParser {
    private ControlledTextParser() {}

    public static List<PolicyRule> parse(String text) {
        List<PolicyRule> out = new ArrayList<>();
        int lineNo = 0;
        for (String raw : text.split("\\R")) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            try { out.add(parseLine(line)); }
            catch (RuntimeException ex) { throw new IllegalArgumentException("Line " + lineNo + ": " + ex.getMessage(), ex); }
        }
        return out;
    }

    public static PolicyRule parseLine(String line) {
        if (line.contains("|")) {
            String[] p = Arrays.stream(line.split("\\|", -1)).map(String::trim).toArray(String[]::new);
            // Article-level canonical format: RuleId | Subject | Resource | Actions | Effect
            if (p.length == 5) return new PolicyRule(p[0], "role", p[1], p[2], splitActions(p[3]), Effect.parse(p[4]));
            // Extended format preserves the subject attribute identifier when needed.
            if (p.length == 6) return new PolicyRule(p[0], p[1], p[2], p[3], splitActions(p[4]), Effect.parse(p[5]));
            throw new IllegalArgumentException("Expected 5 fields (RuleId|Subject|Resource|Actions|Effect) or 6 extended fields");
        }
        // Recovered legacy prototype format: fixed token positions.
        String[] t = line.split("\\s+");
        if (t.length < 10) throw new IllegalArgumentException("Legacy format requires at least 10 whitespace-separated tokens");
        String subject = String.join("_", Arrays.copyOfRange(t, 1, 4));
        String resource = String.join("_", Arrays.copyOfRange(t, 4, 7));
        return new PolicyRule(t[0], "role", subject, resource, Arrays.asList(t[7], t[8]), Effect.parse(t[9]));
    }

    private static List<String> splitActions(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
