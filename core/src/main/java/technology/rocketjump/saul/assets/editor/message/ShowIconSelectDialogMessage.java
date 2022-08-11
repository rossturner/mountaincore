package technology.rocketjump.saul.assets.editor.message;

import java.util.function.Consumer;

public record ShowIconSelectDialogMessage(String initialIconName, Consumer<String> callback) {
}
