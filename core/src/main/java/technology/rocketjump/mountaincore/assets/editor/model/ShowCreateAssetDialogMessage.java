package technology.rocketjump.mountaincore.assets.editor.model;

import technology.rocketjump.mountaincore.entities.model.EntityType;

import java.nio.file.Path;

public record ShowCreateAssetDialogMessage(EntityType entityType, Path path, Object typeDescriptor) {
}
