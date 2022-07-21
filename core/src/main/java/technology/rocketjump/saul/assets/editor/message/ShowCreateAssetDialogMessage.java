package technology.rocketjump.saul.assets.editor.message;

import technology.rocketjump.saul.entities.model.EntityType;

import java.nio.file.Path;

public record ShowCreateAssetDialogMessage(EntityType entityType, Path path, Object typeDescriptor) {
}
