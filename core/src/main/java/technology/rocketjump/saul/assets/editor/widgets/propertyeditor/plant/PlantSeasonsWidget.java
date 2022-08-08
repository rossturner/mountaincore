package technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.kotcrab.vis.ui.widget.VisTable;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.ColorsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSeasonSettings;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.environment.model.Season;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class PlantSeasonsWidget extends VisTable {
    private final PlantSpecies plantSpecies;
    private final MessageDispatcher messageDispatcher;
    private final NativeFileChooser fileChooser;
    private final Path basePath;
    private final List<ColoringLayer> applicableColoringLayers;

    public PlantSeasonsWidget(PlantSpecies plantSpecies, MessageDispatcher messageDispatcher,
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

        for (Season season : Season.values()) {
            PlantSeasonSettings settings = plantSpecies.getSeasons().get(season);
            if (settings != null) {
                this.add(WidgetBuilder.label(season.name())).padTop(20);
                this.add(WidgetBuilder.button("x", x -> {
                    plantSpecies.getSeasons().remove(season);
                    this.reload();
                })).bottom().right();
                this.row();
                this.addSeparator().colspan(2).expand(false, false);
                this.row();

                this.add(WidgetBuilder.label("Growth"));
                this.add(WidgetBuilder.toggle(settings.isGrowth(), settings::setGrowth)).fillX();
                this.row();
                this.add(WidgetBuilder.label("Sheds"));
                this.add(WidgetBuilder.toggle(settings.isShedsLeaves(), settings::setShedsLeaves)).fillX();
                this.row();
                this.add(WidgetBuilder.label("Switch to Growth Stage"));
                this.add(WidgetBuilder.select(new ToStringDecorator<>(settings.getSwitchToGrowthStage(), Objects::toString), growthStageNumbers, null, selected -> {
                    settings.setSwitchToGrowthStage(selected.getObject());
                })).fillX();
                this.row();
                this.add(WidgetBuilder.label("Colors"));
                this.row();
                this.add(new ColorsWidget(settings.getColors(), applicableColoringLayers,
                        EntityType.PLANT, basePath, fileChooser, messageDispatcher)).colspan(2);

                this.row();
                this.addSeparator().colspan(2).expand(false, false);
                this.row();
            }

            this.row();
        }
    }
}
