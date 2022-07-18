package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.jobs.model.Profession;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CreatureAssetPane extends VisTable {
    private final CreatureEntityAttributes creatureAttributes;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
    private final EditorStateProvider editorStateProvider;

    public CreatureAssetPane(CreatureEntityAttributes creatureAttributes, EntityAssetTypeDictionary entityAssetTypeDictionary, CreatureEntityAssetDictionary creatureEntityAssetDictionary, EditorStateProvider editorStateProvider) {
        this.creatureAttributes = creatureAttributes;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
        this.editorStateProvider = editorStateProvider;

        int colCount = 0;
        Entity currentEntity = this.editorStateProvider.getState().getCurrentEntity();
        Map<EntityAssetType, EntityAsset> assetMap = currentEntity.getPhysicalEntityComponent().getTypeMap();
        Collection<EntityAssetType> entityAssetTypes = this.entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE);
        for (EntityAssetType type : entityAssetTypes) {
            if (assetMap.containsKey(type)) {
                Profession primaryProfession = currentEntity.getComponent(ProfessionsComponent.class).getPrimaryProfession();
                List<CreatureEntityAsset> matchingAssets = creatureEntityAssetDictionary.getAllMatchingAssets(type, this.creatureAttributes, primaryProfession);
                if (matchingAssets.size() > 1) { //display selection box when more than one
                    add(createAssetWidget(type.getName(), type, matchingAssets)).left();
                    colCount++;
                    if (colCount == 3) {
                        colCount = 0;
                        row();
                    }

                }
            }
        }
    }


    private VisTable createAssetWidget(String label, EntityAssetType assetType, List<CreatureEntityAsset> items) {
        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        Map<EntityAssetType, EntityAsset> assetMap = currentEntity.getPhysicalEntityComponent().getTypeMap();

        CreatureEntityAsset initialValue = (CreatureEntityAsset) assetMap.get(assetType);

        return WidgetBuilder.selectField(label, initialValue, items, null, creatureEntityAsset -> {
            assetMap.put(assetType, creatureEntityAsset);
        });
    }
}
