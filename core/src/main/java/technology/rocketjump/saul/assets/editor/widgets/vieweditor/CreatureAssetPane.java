package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.rendering.utils.HexColors;

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
                    add(createAssetWidget(type, matchingAssets)).left().uniform();
                    colCount++;
                    if (colCount == 3) {
                        colCount = 0;
                        row();
                    }

                }
            }
        }
        row();

        //Colours
        for (ColoringLayer coloringLayer : creatureAttributes.getColors().keySet()) {
            createColorWidget(coloringLayer, creatureAttributes);
        }
        debug();
    }

    private void createColorWidget(ColoringLayer coloringLayer, CreatureEntityAttributes entityAttributes) {
        Color color = entityAttributes.getColor(coloringLayer);
        TextButton colorButton = new TextButton(HexColors.toHexString(color), new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")));
        colorButton.setColor(color);
        colorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SpeciesColor speciesColor = entityAttributes.getRace().getColors().get(coloringLayer);
                Color newColor = speciesColor.getColor(new RandomXS128().nextLong());
                entityAttributes.getColors().put(coloringLayer, newColor);
                colorButton.setText(HexColors.toHexString(entityAttributes.getColor(coloringLayer)));
                colorButton.setColor(entityAttributes.getColor(coloringLayer));
            }
        });
        VisTable colorWidget = new VisTable();
        colorWidget.add(new VisLabel(toNiceName(coloringLayer.name())));
        colorWidget.add(colorButton);
        add(colorWidget).uniform();
    }

    private VisTable createAssetWidget(EntityAssetType assetType, List<CreatureEntityAsset> items) {
        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        Map<EntityAssetType, EntityAsset> assetMap = currentEntity.getPhysicalEntityComponent().getTypeMap();
        CreatureEntityAsset initialValue = (CreatureEntityAsset) assetMap.get(assetType);
        return WidgetBuilder.selectField(toNiceName(assetType.name), initialValue, items, null, creatureEntityAsset -> {
            assetMap.put(assetType, creatureEntityAsset);
        });
    }

    private String toNiceName(String value) {
        return WordUtils.capitalizeFully(value, '_');
    }
}
