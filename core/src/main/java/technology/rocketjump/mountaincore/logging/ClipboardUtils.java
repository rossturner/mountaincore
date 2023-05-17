package technology.rocketjump.mountaincore.logging;

import org.pmw.tinylog.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtils {
    public static void copyToClipboard(String clipboardText) {
        try {
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            systemClipboard.setContents(new StringSelection(clipboardText), null);
        } catch (RuntimeException e) {
            Logger.error("Failed to copy to clipboard: " + clipboardText, e);
        }
    }
}
