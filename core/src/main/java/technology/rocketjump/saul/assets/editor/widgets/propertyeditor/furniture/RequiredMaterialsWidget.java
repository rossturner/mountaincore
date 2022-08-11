package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.furniture;

import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.spinner.Spinner;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.List;

public class RequiredMaterialsWidget extends VisTable {
    private final FurnitureType furnitureType;
    private final ItemTypeDictionary itemTypeDictionary;

    public RequiredMaterialsWidget(FurnitureType furnitureType, ItemTypeDictionary itemTypeDictionary) {
        this.furnitureType = furnitureType;
        this.itemTypeDictionary = itemTypeDictionary;
        reload();
    }

    public void reload() {
        this.clearChildren();

        for (GameMaterialType materialType : furnitureType.getRequirements().keySet()) {
            this.add(WidgetBuilder.label(ToStringDecorator.materialType(materialType).toString())).padTop(10);
            this.add(); this.add(); this.add(); this.add(); this.add();
            this.add(WidgetBuilder.button("x", x -> {
                furnitureType.getRequirements().remove(materialType);
                this.reload();
            })).padLeft(10);
            this.row();
            List<QuantifiedItemType> quantifiedItemTypes = furnitureType.getRequirements().get(materialType);
            for (QuantifiedItemType quantifiedItemType : quantifiedItemTypes) {
                VisSelectBox<ItemType> itemTypeSelect = WidgetBuilder.select(quantifiedItemType.getItemType(), itemTypeDictionary.getAll(), null, selected -> {
                    quantifiedItemType.setItemType(selected);
                    quantifiedItemType.setItemTypeName(selected.getItemTypeName());
                });
                Spinner quantitySpinner = WidgetBuilder.intSpinner(quantifiedItemType.getQuantity(), 1, Integer.MAX_VALUE, quantifiedItemType::setQuantity);
                VisTextButton liquidToggle = WidgetBuilder.toggle(quantifiedItemType.isLiquid(), quantifiedItemType::setLiquid);
                this.add(WidgetBuilder.label("Item"));
                this.add(itemTypeSelect).uniformX().fillX();
                this.add(WidgetBuilder.label("Quantity")).padLeft(10);
                this.add(quantitySpinner);
                this.add(WidgetBuilder.label("Is Liquid")).padLeft(10);
                this.add(liquidToggle);
                this.add(WidgetBuilder.button("x", x -> {
                    quantifiedItemTypes.remove(quantifiedItemType);
                    if (quantifiedItemTypes.isEmpty()) {
                        furnitureType.getRequirements().remove(materialType);
                    }
                    this.reload();
                })).padLeft(10);
                this.row();
            }

            ItemType nullItem = new ItemType();
            nullItem.setItemTypeName("-none-");
            VisSelectBox<ItemType> newItemSelect = WidgetBuilder.select(null, itemTypeDictionary.getAll(), nullItem, selected -> {
                if (selected != nullItem) {
                    QuantifiedItemType newQuantifiedItem = new QuantifiedItemType();
                    newQuantifiedItem.setItemType(selected);
                    newQuantifiedItem.setItemTypeName(selected.getItemTypeName());
                    newQuantifiedItem.setQuantity(1);
                    quantifiedItemTypes.add(newQuantifiedItem);
                    this.reload();
                }

            });
            this.add(WidgetBuilder.label("Add Item"));
            this.add(newItemSelect).uniformX().fillX();


            this.row();
        }
    }
}
