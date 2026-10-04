package io.github.olviia.cleanterminalcopy;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Notices when the user copies from the terminal and tells {@link ClipboardCleaner} to expect it.
 * <p>
 * Observes actions instead of replacing them: Ctrl+C keeps the terminal's own behaviour
 * (copy with a selection, interrupt without), and this plugin only reacts to a copy that ran.
 */
public final class TerminalCopyListener implements AnActionListener {

    /** Terminal actions that put selected terminal text on the clipboard. */
    private static final Set<String> TERMINAL_COPY_ACTIONS = Set.of("Terminal.CopySelectedText");

    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event) {
        String id = ActionManager.getInstance().getId(action);
        if (id != null && TERMINAL_COPY_ACTIONS.contains(id)) ClipboardCleaner.getInstance().expectCopy();
    }
}
