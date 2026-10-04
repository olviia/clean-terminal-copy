package io.github.olviia.cleanterminalcopy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalTextCleanerTest {

    /** A Claude Code reply copied from the Rider terminal, CRLF and all. */
    @Test
    void claudeReply_matchesGolden() throws IOException {
        String cleaned = TerminalTextCleaner.clean(resource("claude-reply.raw.txt"));
        assertEquals(resource("claude-reply.clean.txt").replace("\r\n", "\n"), cleaned);
    }

    @Test
    void claudeReply_wrappedParagraphBecomesOneLine() throws IOException {
        List<String> lines = TerminalTextCleaner.clean(resource("claude-reply.raw.txt")).lines().toList();
        assertEquals("Good news: your Rider is 2026.2.2. It's installed in the folder still named "
                + "\"Rider 2025.2\", because it updated in place. Its terminal has a copy action with "
                + "the ID Terminal.CopySelectedText, and that's what the design below builds on.", lines.get(0));
        assertTrue(lines.contains("- Collaborators: IndentStripper, and Rider's clipboard (CopyPasteManager)."));
        assertFalse(lines.stream().anyMatch(l -> l.startsWith(" ")));
    }

    @Test
    void headingFollowedByList_staysSeparate() {
        String raw = "  IndentStripper (core)\n  - Responsibility: knows the rules\n  - Collaborators: none.\n";
        assertEquals("IndentStripper (core)\n- Responsibility: knows the rules\n- Collaborators: none.\n",
                TerminalTextCleaner.clean(raw));
    }

    @Test
    void shortLines_areNeverJoined() {
        assertEquals("foo()\nbar()", TerminalTextCleaner.clean("  foo()\n  bar()"));
    }

    @Test
    void relativeIndent_isKept() {
        String raw = "  - parent item\n    - child item\n      continued text";
        assertEquals("- parent item\n  - child item\n    continued text", TerminalTextCleaner.clean(raw));
    }

    @Test
    void wrappedNestedListItem_isJoined() {
        String raw = "  - parent\n"
                + "    - child item whose text is long enough to reach the width of the\n"
                + "      terminal and wrap";
        assertEquals("- parent\n  - child item whose text is long enough to reach the width of the terminal and wrap",
                TerminalTextCleaner.clean(raw));
    }

    @Test
    void selectionStartingMidLine_joinsFirstRow() {
        String raw = "the middle of a sentence\n  and its next row, which was wrapped by the terminal width\n";
        assertEquals("the middle of a sentence and its next row, which was wrapped by the terminal width\n",
                TerminalTextCleaner.clean(raw));
    }

    @Test
    void trailingPadding_andBullet_areRemoved() {
        assertEquals("Hello", TerminalTextCleaner.clean("● Hello      "));
    }

    @Test
    void tableRows_areNeverJoined() {
        String row = "  │ a very long table row that fills the whole width of the terminal  │";
        assertEquals(2, TerminalTextCleaner.clean(row + "\n" + row).lines().count());
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = TerminalTextCleanerTest.class.getResourceAsStream("/" + name)) {
            if (in == null) throw new IOException("missing test resource " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
