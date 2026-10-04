package io.github.olviia.cleanterminalcopy;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Notices copy keystrokes (Ctrl+C, Ctrl+Shift+C, Ctrl+Insert) while a terminal has focus and
 * tells {@link ClipboardCleaner} to expect the copy.
 * <p>
 * Watches keys instead of IDE actions because some terminals handle Ctrl+C inside their own
 * component and never raise an action. The key itself is never consumed: the terminal still
 * decides between copying and interrupting.
 */
public final class TerminalCopyKeyWatcher implements AppLifecycleListener {

    private static final Logger LOG = Logger.getInstance(TerminalCopyKeyWatcher.class);

    /** Fragments of class names that mark a component as part of a terminal. */
    private static final List<String> TERMINAL_MARKERS = List.of("terminal", "jediterm");

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        IdeEventQueue.getInstance().addDispatcher(TerminalCopyKeyWatcher::onEvent, ClipboardCleaner.getInstance());
    }

    private static boolean onEvent(@NotNull AWTEvent event) {
        if (event instanceof KeyEvent key && key.getID() == KeyEvent.KEY_PRESSED && isCopyKey(key)) {
            Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            boolean terminal = isInTerminal(focus);
            LOG.debug("copy key, focus=" + (focus == null ? null : focus.getClass().getName()) + " terminal=" + terminal);
            if (terminal) ClipboardCleaner.getInstance().expectCopy();
        }
        return false;
    }

    private static boolean isCopyKey(KeyEvent key) {
        int mods = key.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
                | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK);
        int code = key.getKeyCode();
        return (code == KeyEvent.VK_C && (mods == InputEvent.CTRL_DOWN_MASK
                        || mods == (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)))
                || (code == KeyEvent.VK_INSERT && mods == InputEvent.CTRL_DOWN_MASK);
    }

    private static boolean isInTerminal(Component component) {
        for (Component c = component; c != null; c = c.getParent()) {
            String name = c.getClass().getName().toLowerCase();
            for (String marker : TERMINAL_MARKERS) if (name.contains(marker)) return true;
        }
        return false;
    }
}
