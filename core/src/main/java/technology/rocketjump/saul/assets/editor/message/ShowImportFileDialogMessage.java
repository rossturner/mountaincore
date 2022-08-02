package technology.rocketjump.saul.assets.editor.message;

import com.badlogic.gdx.files.FileHandle;

import java.util.function.Consumer;

public record ShowImportFileDialogMessage(FileHandle originalFileHandle, FileHandle destinationDirectory, String suggestedFilename, Consumer<FileHandle> callback) {
}
