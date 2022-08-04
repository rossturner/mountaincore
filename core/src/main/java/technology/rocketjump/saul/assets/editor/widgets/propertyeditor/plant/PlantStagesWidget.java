package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.kotcrab.vis.ui.widget.VisTable;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;

import java.nio.file.Path;
import java.util.List;

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



        /*
        private String name; // Mostly to help with debugging, not used by code (index in list of growth stages used instead)
	private Integer nextGrowthStage = null; // Null representing no subsequent stage to go to
	private double seasonsUntilComplete = 1;

	private float initialPlantScale = 1;
	private float completionPlantScale = 1;
	private float initialFruitScale = 1;
	private float completionFruitScale = 1;
	private int tileHeight = 1;
	private boolean showFruit = false;
	private Map<ColoringLayer, SpeciesColor> colors = new EnumMap<>(ColoringLayer.class);
	private List<PlantGrowthCompleteTag> onCompletion = new LinkedList<>();

	private PlantSpeciesHarvestType harvestType = null;
	private Integer harvestSwitchesToGrowthStage = null;
	private List<PlantSpeciesItem> harvestedItems = new ArrayList<>();

	// TODO might work better to replace this with JobType
	public enum PlantSpeciesHarvestType {

		LOGGING, FORAGING, FARMING

	}

	public enum PlantGrowthCompleteTag {

		DISPERSE_SEEDS, DESTROY_PLANT

	}
         */

//        Collection<ToStringDecorator<Integer>> growthStageNumbers = new ArrayList<>();
//        growthStageNumbers.add(ToStringDecorator.none());
//        for (int i = 0; i < plantSpecies.getGrowthStages().size(); i++) {
//            growthStageNumbers.add(new ToStringDecorator<>(i, Object::toString));
//        }
//
//        for (Season season : Season.values()) {
//            PlantSeasonSettings settings = plantSpecies.getSeasons().get(season);
//            if (settings != null) {
//                this.add(WidgetBuilder.label(season.name())).padTop(20);
//                this.add(WidgetBuilder.button("x", x -> {
//                    plantSpecies.getSeasons().remove(season);
//                    this.reload();
//                })).bottom().right();
//                this.row();
//                this.addSeparator().colspan(2);
//                this.row();
//
//
//                this.add(WidgetBuilder.label("Growth"));
//                this.add(WidgetBuilder.toggle(settings.isGrowth(), settings::setGrowth)).fillX();
//                this.row();
//                this.add(WidgetBuilder.label("Sheds"));
//                this.add(WidgetBuilder.toggle(settings.isShedsLeaves(), settings::setShedsLeaves)).fillX();
//                this.row();
//                this.add(WidgetBuilder.label("Switch to Growth Stage"));
//                this.add(WidgetBuilder.select(new ToStringDecorator<>(settings.getSwitchToGrowthStage(), Objects::toString), growthStageNumbers, null, selected -> {
//                    settings.setSwitchToGrowthStage(selected.getObject());
//                })).fillX();
//                this.row();
//                this.add(WidgetBuilder.label("Colors"));
//                this.row();
//                this.add(new ColorsWidget(settings.getColors(), applicableColoringLayers,
//                        EntityType.PLANT, basePath, fileChooser, messageDispatcher)).colspan(2).pad(15);
//
//                this.row();
//                this.addSeparator().colspan(2);
//                this.row();
//            }
//
//            this.row();
//        }
    }
}
