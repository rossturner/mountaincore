package technology.rocketjump.saul.assets.editor.factory;

import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;

import java.nio.file.Path;

public interface UIFactory {

    EntityType getEntityType();

    Entity createEntityForRendering(String name);

    OkCancelDialog createEntityDialog(Path path);

    OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message);
}
