package technology.rocketjump.saul.assets.editor.model;

import technology.rocketjump.saul.entities.model.EntityType;

import java.nio.file.Path;

public record ShowCreateAssetDialogMessage(EntityType entityType, Path path, Object typeDescriptor) {
}
