package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

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
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.rendering.utils.HexColors;

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
        TextButton colorButton = new TextButton(HexColors.toHexString(color), new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")));
        colorButton.setColor(color);
        colorButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                attributes = new PlantEntityAttributes(new RandomXS128().nextLong(), entityAttributes.getSpecies());

                editorStateProvider.getState().getCurrentEntity().getPhysicalEntityComponent().setAttributes(attributes);

                attributes.updateColors(null);
                colorButton.setText(HexColors.toHexString(attributes.getColor(coloringLayer)));
                colorButton.setColor(attributes.getColor(coloringLayer));
            }
        });
        VisTable colorWidget = new VisTable();
        colorWidget.add(WidgetBuilder.label(coloringLayer.name()));
        colorWidget.add(colorButton);
        add(colorWidget);
    }

}
