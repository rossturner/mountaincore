package technology.rocketjump.mountaincore.entities.model.physical;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;

import java.util.Map;

public interface EntityAttributes extends ChildPersistable {

	long getSeed();

	Color getColor(ColoringLayer coloringLayer);

	EntityAttributes clone();

	Map<GameMaterialType, GameMaterial> getMaterials();

}
