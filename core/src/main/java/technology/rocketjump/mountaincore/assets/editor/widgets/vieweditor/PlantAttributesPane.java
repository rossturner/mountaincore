package technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.util.List;
import java.util.stream.IntStream;

@Singleton
public class PlantAttributesPane extends AbstractAttributesPane {
    private PlantEntityAttributes attributes;

    @Inject
    public PlantAttributesPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher) {
        super(editorStateProvider, messageDispatcher);
    }


    public void reload() {
        this.clearChildren();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        attributes = (PlantEntityAttributes) currentEntity.getPhysicalEntityComponent().getAttributes();
        List<Integer> growthStages = IntStream.range(0, attributes.getSpecies().getGrowthStages().size()).boxed().toList();

        add(WidgetBuilder.selectField("Growth Stages", attributes.getGrowthStageCursor(), growthStages, 0, update(attributes::setGrowthStageCursor)));
        createColorWidget(ColoringLayer.BRANCHES_COLOR, attributes);
        createColorWidget(ColoringLayer.LEAF_COLOR, attributes);

        //season
        //seed?
    }

    private void createColorWidget(ColoringLayer coloringLayer, PlantEntityAttributes entityAttributes) {
        Color color = entityAttributes.getColor(coloringLayer);
        if (color != null) {
            TextButton colorButton = new TextButton(HexColors.toHexString(color), new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")));
            colorButton.setColor(color);
            colorButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {

                    int currentGrowthStage = attributes.getGrowthStageCursor();
                    attributes = new PlantEntityAttributes(new RandomXS128().nextLong(), entityAttributes.getSpecies());
                    attributes.setGrowthStageCursor(currentGrowthStage);


                    editorStateProvider.getState().getCurrentEntity().getPhysicalEntityComponent().setAttributes(attributes);

                    attributes.updateColors(null);
                    colorButton.setText(HexColors.toHexString(attributes.getColor(coloringLayer)));
                    colorButton.setColor(attributes.getColor(coloringLayer));
                    reload();
                }
            });
            VisTable colorWidget = new VisTable();
            colorWidget.add(WidgetBuilder.label(coloringLayer.name()));
            colorWidget.add(colorButton);
            add(colorWidget);
        }
    }

}
