package technology.rocketjump.mountaincore.assets.editor.model;

import java.util.function.Consumer;

public record ShowIconSelectDialogMessage(String initialIconName, Consumer<String> callback) {
}
