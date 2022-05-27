package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesItem;

import java.util.List;
import java.util.Optional;

public class TreeFallenMessage {

	private final Vector2 treeWorldPosition;
	private final Color branchColor;
	private final Optional<Color> leafColor;
	private final boolean fallToWest;
	private final List<PlantSpeciesItem> itemsToCreate;

	public TreeFallenMessage(Vector2 treeWorldPosition, Color actualBranchColor, Optional<Color> leafColor, boolean fallToWest, List<PlantSpeciesItem> itemsToCreate) {
		this.treeWorldPosition = treeWorldPosition;
		this.branchColor = actualBranchColor;
		this.leafColor = leafColor;
		this.fallToWest = fallToWest;
		this.itemsToCreate = itemsToCreate;
	}

	public Vector2 getTreeWorldPosition() {
		return treeWorldPosition;
	}

	public Color getBranchColor() {
		return branchColor;
	}

	public Optional<Color> getLeafColor() {
		return leafColor;
	}

	public boolean isFallToWest() {
		return fallToWest;
	}

	public List<PlantSpeciesItem> getItemsToCreate() {
		return itemsToCreate;
	}
}
