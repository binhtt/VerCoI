package vercoi.parser;

import vercoi.model.Effect;
import vercoi.model.JavaAuthorizationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight static recognizer for the explicitly supported Java authorization idiom:
 * allow("SUBJECT","RESOURCE","ACTION") / deny(...).
 * It is deliberately not presented as a general Java semantic analyzer.
 */
public final class JavaPolicyParser {
    private JavaPolicyParser() {}
    private static final Pattern CALL = Pattern.compile(
        "\\b(allow|deny)\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"\\s*\\)\\s*;",
        Pattern.CASE_INSENSITIVE);

    public static List<JavaAuthorizationRule> parse(String javaSource) {
        String source = stripComments(javaSource == null ? "" : javaSource);
        List<JavaAuthorizationRule> out = new ArrayList<>();
        Matcher m = CALL.matcher(source);
        while (m.find()) {
            out.add(new JavaAuthorizationRule(m.group(2), m.group(3), m.group(4),
                    m.group(1).equalsIgnoreCase("allow") ? Effect.Permit : Effect.Deny));
        }
        return List.copyOf(out);
    }

    /** Remove // and /* ... *\/ comments while preserving string/character literals. */
    static String stripComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inString = false, inChar = false, line = false, block = false, escape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i), n = i + 1 < s.length() ? s.charAt(i + 1) : '\0';
            if (line) {
                if (c == '\n') { line = false; out.append(c); } else out.append(' ');
                continue;
            }
            if (block) {
                if (c == '*' && n == '/') { out.append("  "); i++; block = false; }
                else out.append(c == '\n' ? '\n' : ' ');
                continue;
            }
            if (!inString && !inChar && c == '/' && n == '/') { out.append("  "); i++; line = true; continue; }
            if (!inString && !inChar && c == '/' && n == '*') { out.append("  "); i++; block = true; continue; }
            out.append(c);
            if (escape) { escape = false; continue; }
            if ((inString || inChar) && c == '\\') { escape = true; continue; }
            if (!inChar && c == '"') inString = !inString;
            else if (!inString && c == '\'') inChar = !inChar;
        }
        return out.toString();
    }
}
