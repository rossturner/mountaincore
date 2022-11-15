package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesItem;
import technology.rocketjump.saul.materials.GameMaterialDictionary;

public class PlantSpeciesItemWidget extends VisTable {
    private final PlantSpeciesGrowthStage growthStage;
    private final GameMaterialDictionary materialDictionary;
    private final ItemTypeDictionary itemTypeDictionary;

    public PlantSpeciesItemWidget(PlantSpeciesGrowthStage growthStage, GameMaterialDictionary materialDictionary, ItemTypeDictionary itemTypeDictionary) {
        this.growthStage = growthStage;
        this.materialDictionary = materialDictionary;
        this.itemTypeDictionary = itemTypeDictionary;
        reload();
    }

    public void reload() {
        this.clearChildren();
        this.columnDefaults(0).left().uniformX();
        this.columnDefaults(1).uniformX().fillX();

        for (PlantSpeciesItem harvestedItem : growthStage.getHarvestedItems()) {
            this.add(WidgetBuilder.label("Harvest Item")).padTop(20);
            this.add(WidgetBuilder.button("x", x -> {
                growthStage.getHarvestedItems().remove(harvestedItem);
                this.reload();
            })).bottom().right().fill(false, false);
            this.row();


            this.add(WidgetBuilder.label("Item Type"));
            this.add(WidgetBuilder.select(harvestedItem.getItemType(), itemTypeDictionary.getAll(), null, selected -> {
                harvestedItem.setItemType(selected);
                harvestedItem.setItemTypeName(selected.getItemTypeName());
            }));
            this.row();

            this.add(WidgetBuilder.label("Material"));
            this.add(WidgetBuilder.select(harvestedItem.getMaterial(), materialDictionary.getAll(), null, material -> {
                harvestedItem.setMaterial(material);
                harvestedItem.setMaterialName(material.getMaterialName());
            }));
            this.row();

            this.add(WidgetBuilder.label("Quantity"));
            this.add(WidgetBuilder.intSpinner(harvestedItem.getQuantity(), 1, Integer.MAX_VALUE, harvestedItem::setQuantity));
            this.row();

            this.add(WidgetBuilder.label("Chance"));
            this.add(WidgetBuilder.floatSpinner(harvestedItem.getChance(), 0.0f, Float.MAX_VALUE, harvestedItem::setChance));
            this.row().padBottom(15);
        }
    }

}
