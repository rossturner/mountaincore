package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.*;

@Singleton
public class FurnitureAttributesPane extends AbstractAttributesPane {
    private final GameMaterialDictionary materialDictionary;

    @Inject
    public FurnitureAttributesPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher, GameMaterialDictionary materialDictionary) {
        super(editorStateProvider, messageDispatcher);
        this.materialDictionary = materialDictionary;
    }


    public void reload() {
        this.clearChildren();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) currentEntity.getPhysicalEntityComponent().getAttributes();

        FurnitureType furnitureType = attributes.getFurnitureType();
        Collection<GameMaterialType> materialTypes = furnitureType.getRequirements().keySet();


        List<ToStringDecorator<GameMaterial>> availableMaterials = new ArrayList<>();
        for (GameMaterialType type : materialTypes) {
            materialDictionary.getByType(type)
                    .stream()
                    .map(ToStringDecorator::material)
                    .forEach(availableMaterials::add);
        }

        ToStringDecorator<GameMaterial> initalMaterial = ToStringDecorator.material(attributes.getMaterials().get(attributes.getPrimaryMaterialType()));

        Set<FurnitureLayout> furnitureLayouts = new LinkedHashSet<>();
        FurnitureLayout currentLayout = attributes.getCurrentLayout();
        while (currentLayout != null && !furnitureLayouts.contains(currentLayout)) {
            furnitureLayouts.add(currentLayout);
            currentLayout = currentLayout.getRotatesTo();
        }

        add(WidgetBuilder.selectField("Layout", attributes.getCurrentLayout(), furnitureLayouts, null, update(attributes::setCurrentLayout)));

        add(WidgetBuilder.selectField("Material", initalMaterial, availableMaterials, null, update(decorated -> {
            GameMaterial selectedMaterial = decorated.getObject();
            attributes.setPrimaryMaterialType(selectedMaterial.getMaterialType());
            attributes.setMaterial(selectedMaterial);
        })));
        //seed?
    }

}
