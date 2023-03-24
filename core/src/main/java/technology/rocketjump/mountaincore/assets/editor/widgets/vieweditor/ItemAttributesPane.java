package technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemQuality;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

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

        Collection<ItemQuality> itemQualities = Arrays.asList(ItemQuality.values());
        List<GameMaterialType> itemMaterialTypes = itemType.getMaterialTypes();
        List<ItemPlacement> itemPlacements = Arrays.asList(ItemPlacement.values());

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
