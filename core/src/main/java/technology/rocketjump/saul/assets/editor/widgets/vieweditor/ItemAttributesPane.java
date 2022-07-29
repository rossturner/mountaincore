package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.item.model.ItemSize;
import technology.rocketjump.saul.assets.entities.item.model.ItemStyle;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Singleton
public class ItemAttributesPane extends AbstractAttributesPane {
    private final GameMaterialDictionary materialDictionary;

    @Inject
    public ItemAttributesPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher, GameMaterialDictionary materialDictionary) {
        super(editorStateProvider, messageDispatcher);
        this.materialDictionary = materialDictionary;
    }


    public void reload() {
        this.clearChildren();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();

        ItemEntityAttributes attributes = (ItemEntityAttributes) currentEntity.getPhysicalEntityComponent().getAttributes();
        ItemType itemType = attributes.getItemType();

        Collection<ItemSize> itemSizes = Arrays.asList(ItemSize.values());
        Collection<ItemStyle> itemStyles = Arrays.asList(ItemStyle.values());
        Collection<ItemQuality> itemQualities = Arrays.asList(ItemQuality.values());
        List<GameMaterialType> itemMaterialTypes = itemType.getMaterialTypes();
        List<ItemPlacement> itemPlacements = Arrays.asList(ItemPlacement.values());

        add(WidgetBuilder.selectField("Size", attributes.getItemSize(), itemSizes, null, update(attributes::setItemSize)));
        add(WidgetBuilder.selectField("Style", attributes.getItemStyle(), itemStyles, null, update(attributes::setItemStyle)));

        for (GameMaterialType type : itemMaterialTypes) {
            List<GameMaterial> materials = materialDictionary.getByType(type);
            add(WidgetBuilder.selectField(type.name() + " Material", attributes.getMaterial(type), materials, null, update(attributes::setMaterial)));
        }

        if (itemType.getMaxStackSize() > 1) {
            add(WidgetBuilder.slider("Quantity", attributes.getQuantity(), 1, itemType.getMaxStackSize(), 1, update(attributes::setQuantity)));
        }
        add(WidgetBuilder.selectField("Quality", attributes.getItemQuality(), itemQualities, null, update(attributes::setItemQuality)));

        add(WidgetBuilder.selectField("Placement", attributes.getItemPlacement(), itemPlacements, null, update(attributes::setItemPlacement)));

        //seed?
    }
}
