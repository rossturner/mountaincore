package technology.rocketjump.mountaincore.assets.editor.model;

import com.badlogic.gdx.files.FileHandle;

import java.util.function.Consumer;

public record ShowImportFileDialogMessage(FileHandle originalFileHandle, FileHandle destinationDirectory, String suggestedFilename, Consumer<FileHandle> callback) {
}
