package io.github.olviia.cleanterminalcopy;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;

import javax.swing.Timer;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/**
 * Cleans the clipboard text a terminal copy has just produced.
 * <p>
 * Terminals write the system clipboard in their own ways (some bypass the IDE entirely), so
 * this class does not wait to be told what was copied: when a copy starts it remembers the
 * clipboard text, then looks again a few times shortly after. The first new text it sees is
 * the copy; it is cleaned once and written back. Nothing else in the IDE is touched.
 */
@Service(Service.Level.APP)
public final class ClipboardCleaner implements Disposable {

    private static final Logger LOG = Logger.getInstance(ClipboardCleaner.class);

    /** When to look for the copied text, in ms after the copy started. */
    private static final int[] CHECK_DELAYS_MS = {30, 150, 400, 1000};

    /** Until when a copy is already being watched for (EDT only). */
    private long watchingUntil;

    public static ClipboardCleaner getInstance() {
        return ApplicationManager.getApplication().getService(ClipboardCleaner.class);
    }

    /** A terminal copy is starting: clean whatever text it puts on the clipboard. */
    void expectCopy() {
        long time = System.currentTimeMillis();
        if (time < watchingUntil) return; // key and action of the same copy both report it
        watchingUntil = time + CHECK_DELAYS_MS[CHECK_DELAYS_MS.length - 1];
        String before = clipboardText();
        boolean[] done = {false};
        for (int delay : CHECK_DELAYS_MS) {
            Timer timer = new Timer(delay, e -> {
                if (done[0]) return;
                String now = clipboardText();
                if (now == null || Objects.equals(now, before)) return;
                done[0] = true;
                String cleaned = TerminalTextCleaner.clean(now);
                LOG.debug("copy seen after " + delay + " ms, cleaned=" + !cleaned.equals(now));
                if (!cleaned.equals(now)) CopyPasteManager.getInstance().setContents(new StringSelection(cleaned));
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    private static String clipboardText() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void dispose() {
    }
}
