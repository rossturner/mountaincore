package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.kotcrab.vis.ui.widget.VisTable;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.ColorsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesGrowthStage;

import java.nio.file.Path;
import java.util.*;

public class PlantStagesWidget extends VisTable {
    private final PlantSpecies plantSpecies;
    private final MessageDispatcher messageDispatcher;
    private final NativeFileChooser fileChooser;
    private final Path basePath;
    private final List<ColoringLayer> applicableColoringLayers;

    public PlantStagesWidget(PlantSpecies plantSpecies, MessageDispatcher messageDispatcher,
                             NativeFileChooser fileChooser, Path basePath, List<ColoringLayer> applicableColoringLayers) {
        this.plantSpecies = plantSpecies;
        this.messageDispatcher = messageDispatcher;
        this.fileChooser = fileChooser;
        this.basePath = basePath;
        this.applicableColoringLayers = applicableColoringLayers;
        reload();
    }

    public void reload() {
        this.clearChildren();
        this.columnDefaults(0).left().uniformX();
        this.columnDefaults(1).expandX();

        Collection<ToStringDecorator<Integer>> growthStageNumbers = new ArrayList<>();
        growthStageNumbers.add(ToStringDecorator.none());
        for (int i = 0; i < plantSpecies.getGrowthStages().size(); i++) {
            growthStageNumbers.add(new ToStringDecorator<>(i, Object::toString));
        }

        for (int growthStageIndex = 0; growthStageIndex < plantSpecies.getGrowthStages().size(); growthStageIndex++) {
            PlantSpeciesGrowthStage growthStage = plantSpecies.getGrowthStages().get(growthStageIndex);

            if (growthStage != null) {
                this.add(WidgetBuilder.label("Growth stage " + growthStageIndex)).padTop(20);
                this.add(WidgetBuilder.button("x", x -> {
                    plantSpecies.getGrowthStages().remove(growthStage);
                    this.reload();
                })).bottom().right();
                this.row();
                this.addSeparator().colspan(2);
                this.row();

                this.add(WidgetBuilder.label("Next Growth Stage"));
                this.add(WidgetBuilder.select(new ToStringDecorator<>(growthStage.getNextGrowthStage(), Objects::toString), growthStageNumbers, null, selected -> {
                    growthStage.setNextGrowthStage(selected.getObject());
                })).fillX();
                this.row();

                this.add(WidgetBuilder.label("Seasons Until Complete"));
                this.add(WidgetBuilder.doubleSpinner(growthStage.getSeasonsUntilComplete(), 0.0, Double.MAX_VALUE, growthStage::setSeasonsUntilComplete)).fillX();
                this.row();

                this.add(WidgetBuilder.label("Initial Plant Scale"));
                this.add(WidgetBuilder.floatSpinner(growthStage.getInitialPlantScale(), 0.0f, Float.MAX_VALUE, growthStage::setInitialPlantScale)).fillX();
                this.row();

                this.add(WidgetBuilder.label("Complete Plant Scale"));
                this.add(WidgetBuilder.floatSpinner(growthStage.getCompletionPlantScale(), 0.0f, Float.MAX_VALUE, growthStage::setCompletionPlantScale)).fillX();
                this.row();

                this.add(WidgetBuilder.label("Initial Fruit Scale"));
                this.add(WidgetBuilder.floatSpinner(growthStage.getInitialFruitScale(), 0.0f, Float.MAX_VALUE, growthStage::setInitialFruitScale)).fillX();
                this.row();

                this.add(WidgetBuilder.label("Complete Fruit Scale"));
                this.add(WidgetBuilder.floatSpinner(growthStage.getCompletionFruitScale(), 0.0f, Float.MAX_VALUE, growthStage::setCompletionPlantScale)).fillX();
                this.row();

                this.add(WidgetBuilder.label("Tile Height"));
                this.add(WidgetBuilder.intSpinner(growthStage.getTileHeight(), 0, Integer.MAX_VALUE, growthStage::setTileHeight)).fillX();
                this.row();

                this.add(WidgetBuilder.label("Show Fruit"));
                this.add(WidgetBuilder.toggle(growthStage.isShowFruit(), growthStage::setShowFruit)).fillX();
                this.row();

                this.add(WidgetBuilder.label("On Completion"));
                this.add(WidgetBuilder.checkboxes(growthStage.getOnCompletion(), Arrays.asList(PlantSpeciesGrowthStage.PlantGrowthCompleteTag.values()), growthStage.getOnCompletion()::add, growthStage.getOnCompletion()::remove));
                this.row();

                this.add(WidgetBuilder.label("Colors"));
                this.row();
                this.add(new ColorsWidget(growthStage.getColors(), applicableColoringLayers,
                        EntityType.PLANT, basePath, fileChooser, messageDispatcher)).colspan(2).pad(15);
                this.row();


                this.row();
                this.addSeparator().colspan(2);
                this.row();
            }
        }



        /*

	private Map<ColoringLayer, SpeciesColor> colors = new EnumMap<>(ColoringLayer.class);

	private PlantSpeciesHarvestType harvestType = null;
	private Integer harvestSwitchesToGrowthStage = null;
	private List<PlantSpeciesItem> harvestedItems = new ArrayList<>();

	// TODO might work better to replace this with JobType
	public enum PlantSpeciesHarvestType {

		LOGGING, FORAGING, FARMING

	}
         */

    }
}
