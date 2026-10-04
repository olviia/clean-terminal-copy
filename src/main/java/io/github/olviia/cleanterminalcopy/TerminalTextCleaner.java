package io.github.olviia.cleanterminalcopy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns text copied from a terminal back into the text that was printed.
 * <p>
 * A terminal (and a CLI agent drawing into it) adds layout the reader never meant to copy:
 * a margin in front of every line, a hard line break wherever a sentence reached the
 * terminal width, padding spaces at line ends, and the agent's message bullet.
 * This class removes exactly that layout and nothing else. Pure text in, text out:
 * no IDE dependency, so it is unit-testable and reusable outside the plugin.
 */
public final class TerminalTextCleaner {

    /** Below this width a selection is treated as never wrapped (no terminal is that narrow). */
    static final int MIN_WRAP_WIDTH = 40;

    /** List item start: "- ", "* ", "+ ", "• ", "1. ", "2) ". */
    private static final Pattern LIST_MARKER = Pattern.compile("^([-*+•]|\\d{1,3}[.)])\\s+");

    /** Message bullets CLI agents print in the margin of a reply (Claude Code: ● / ⏺). */
    private static final String BULLETS = "●⏺";

    private TerminalTextCleaner() {
    }

    /** Returns {@code raw} without margin, wrap breaks, trailing padding and message bullet. */
    public static String clean(String raw) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        boolean endsWithNewline = text.endsWith("\n");
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        if (endsWithNewline) lines.remove(lines.size() - 1);
        if (lines.isEmpty()) return raw;

        for (int i = 0; i < lines.size(); i++) lines.set(i, stripTrailing(lines.get(i)));
        lines.set(0, bulletToMargin(lines.get(0)));

        int width = 0;
        for (String line : lines) width = Math.max(width, line.length());

        int margin = margin(lines);
        boolean firstLinePartial = lines.size() > 1 && indent(lines.get(0)) == 0 && margin > 0;

        List<String> out = new ArrayList<>();
        int hang = 0;
        int previousLength = 0;
        for (int i = 0; i < lines.size(); i++) {
            String physical = lines.get(i);
            String line = physical.substring(Math.min(margin, indent(physical)));
            boolean join = !out.isEmpty()
                    && isContinuation(out.get(out.size() - 1), line, hang)
                    && (i == 1 && firstLinePartial || wrappedBefore(previousLength, line, width));
            if (join) {
                out.set(out.size() - 1, out.get(out.size() - 1) + " " + line.strip());
            } else {
                out.add(line);
                hang = hangIndent(line);
            }
            previousLength = physical.length();
        }
        return String.join("\n", out) + (endsWithNewline ? "\n" : "");
    }

    /** The margin shared by all non-blank lines; the first line counts only if it is indented,
     *  since a selection usually starts mid-line, after the margin. */
    private static int margin(List<String> lines) {
        int margin = Integer.MAX_VALUE;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank()) margin = Math.min(margin, indent(line));
        }
        int first = indent(lines.get(0));
        if (first > 0 || margin == Integer.MAX_VALUE) margin = Math.min(margin, first);
        return margin == Integer.MAX_VALUE ? 0 : margin;
    }

    /** True when {@code line} has the shape of the next row of {@code logical}:
     *  both non-blank, aligned to its hanging indent, not a new list item, not a table row. */
    private static boolean isContinuation(String logical, String line, int hang) {
        if (logical.isBlank() || line.isBlank()) return false;
        if (indent(line) != hang) return false;
        String content = line.substring(hang);
        if (LIST_MARKER.matcher(content).find()) return false;
        return !isBoxDrawing(content.charAt(0)) && !isBoxDrawing(logical.charAt(logical.length() - 1));
    }

    /** True when the previous row broke only for lack of room: its first word would not have fit. */
    private static boolean wrappedBefore(int previousLength, String line, int width) {
        if (width < MIN_WRAP_WIDTH) return false;
        String word = line.strip().split("\\s+", 2)[0];
        return previousLength + 1 + word.length() > width;
    }

    /** Column where wrapped rows of this line start: after its indent and list marker. */
    private static int hangIndent(String line) {
        int indent = indent(line);
        Matcher marker = LIST_MARKER.matcher(line.substring(indent));
        return marker.find() ? indent + marker.end() : indent;
    }

    /** Replaces a leading message bullet and its space by spaces, so it reads as margin. */
    private static String bulletToMargin(String line) {
        int indent = indent(line);
        if (line.length() > indent + 1
                && BULLETS.indexOf(line.charAt(indent)) >= 0
                && line.charAt(indent + 1) == ' ') {
            return " ".repeat(indent + 2) + line.substring(indent + 2);
        }
        return line;
    }

    private static int indent(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static String stripTrailing(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) end--;
        return line.substring(0, end);
    }

    private static boolean isBoxDrawing(char c) {
        return c >= '─' && c <= '╿';
    }
}
