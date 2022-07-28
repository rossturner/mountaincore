package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;

import java.nio.file.Path;
import java.util.Random;

@Singleton
public class ItemUIFactory implements UIFactory {
    private final ItemEntityFactory itemEntityFactory;
    private final ItemTypeDictionary itemTypeDictionary;

    @Inject
    public ItemUIFactory(ItemEntityFactory itemEntityFactory, ItemTypeDictionary itemTypeDictionary) {
        this.itemEntityFactory = itemEntityFactory;
        this.itemTypeDictionary = itemTypeDictionary;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ITEM;
    }

    @Override
    public Entity createEntityForRendering(String name) {
        Random random = new Random();
        GameContext gameContext = new GameContext();
        gameContext.setRandom(new RandomXS128());
        ItemType itemType = itemTypeDictionary.getByName(name);
        ItemEntityAttributes attributes = new ItemEntityAttributes();
        attributes.setItemType(itemType);
        attributes.setSeed(random.nextLong());
        attributes.setQuantity(1);

        return itemEntityFactory.create(attributes, new GridPoint2(), true, gameContext);
    }

    @Override
    public VisTable getViewEditorControls() {
        return null;
    }

    @Override
    public OkCancelDialog createEntityDialog(Path path) {
        return null;
    }

    @Override
    public OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message) {
        return null;
    }
}
