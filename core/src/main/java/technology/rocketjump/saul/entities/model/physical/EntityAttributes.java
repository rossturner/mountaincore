package technology.rocketjump.saul.entities.model.physical;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.persistence.model.ChildPersistable;

import java.util.Map;

public interface EntityAttributes extends ChildPersistable {

	long getSeed();

	Color getColor(ColoringLayer coloringLayer);

	EntityAttributes clone();

	Map<GameMaterialType, GameMaterial> getMaterials();

}
