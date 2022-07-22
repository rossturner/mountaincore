package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CreatureAttributesPane extends VisTable {

    private final EditorStateProvider editorStateProvider;
    private final EntityAssetUpdater entityAssetUpdater;
    private final ProfessionDictionary professionDictionary;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
    private final MessageDispatcher messageDispatcher;
    private int colCount = 0;

    public CreatureAttributesPane(CreatureEntityAttributes creatureAttributes, EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater, ProfessionDictionary professionDictionary, EntityAssetTypeDictionary entityAssetTypeDictionary, CreatureEntityAssetDictionary creatureEntityAssetDictionary, MessageDispatcher messageDispatcher) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.entityAssetUpdater = entityAssetUpdater;
        this.professionDictionary = professionDictionary;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
        this.messageDispatcher = messageDispatcher;

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        Collection<Gender> genders = creatureAttributes.getRace().getGenders().keySet();
        Collection<CreatureBodyShape> bodyShapes = creatureAttributes.getRace().getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
        Collection<Consciousness> consciousnesses = Arrays.asList(Consciousness.values());
        Collection<Profession> professions = this.professionDictionary.getAll();

        ProfessionsComponent professionsComponent = currentEntity.getOrCreateComponent(ProfessionsComponent.class);

        //Attributes components
        add(WidgetBuilder.selectField("Gender:", creatureAttributes.getGender(), genders, null, update(creatureAttributes::setGender)));
        add(WidgetBuilder.selectField("Body Shape:", creatureAttributes.getBodyShape(), bodyShapes, null, update(creatureAttributes::setBodyShape)));
        add(WidgetBuilder.selectField("Consciousness:", creatureAttributes.getConsciousness(), consciousnesses, null, update(creatureAttributes::setConsciousness)));
        add(WidgetBuilder.selectField("Profession:", professionsComponent.getPrimaryProfession(), professions, null, update(profession -> {
            professionsComponent.clear();
            professionsComponent.setSkillLevel(profession, 50);
        })));

        row();

        Map<EntityAssetType, EntityAsset> assetMap = currentEntity.getPhysicalEntityComponent().getTypeMap();
        Collection<EntityAssetType> entityAssetTypes = this.entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE);
        for (EntityAssetType type : entityAssetTypes) {
            if (assetMap.containsKey(type)) {
                Profession primaryProfession = currentEntity.getComponent(ProfessionsComponent.class).getPrimaryProfession();
                List<CreatureEntityAsset> matchingAssets = this.creatureEntityAssetDictionary.getAllMatchingAssets(type, creatureAttributes, primaryProfession);
                if (matchingAssets.size() > 1) { //display selection box when more than one
                    add(createAssetWidget(type, matchingAssets));
                }
            }
        }
        row();

        //Colours
        for (ColoringLayer coloringLayer : creatureAttributes.getColors().keySet()) {
            createColorWidget(coloringLayer, creatureAttributes);
        }
//        debug();
    }

    @Override
    public <T extends Actor> Cell<T> add(T actor) {
        if (colCount == 3) {
            row();
        }
        colCount++;
        return super.add(actor).left();
    }

    @Override
    public Cell row() {
        colCount = 0;
        return super.row();
    }

    private <T> Consumer<T> update(Consumer<T> input) {
        Consumer<T> consumer = x -> {
            messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, editorStateProvider.getState().getCurrentEntity());
            editorStateProvider.stateChanged();
        };

        return input.andThen(consumer);
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
        colorWidget.add(new VisLabel(toNiceName(coloringLayer.name()) + ":"));
        colorWidget.add(colorButton);
        add(colorWidget);
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
