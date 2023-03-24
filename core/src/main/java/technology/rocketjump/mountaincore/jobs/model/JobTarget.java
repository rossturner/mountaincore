package technology.rocketjump.mountaincore.jobs.model;

import com.badlogic.gdx.graphics.Color;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.cooking.model.CookingRecipe;
import technology.rocketjump.mountaincore.crafting.model.CraftingRecipe;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoof;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.rooms.Bridge;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;

import static technology.rocketjump.mountaincore.entities.model.EntityType.ITEM;

public class JobTarget {

	public final JobTargetType type;
	private CookingRecipe cookingRecipe;
	private CraftingRecipe craftingRecipe;
	private Entity entity;
	private Construction construction;
	private Bridge bridge;
	private MapTile tile;
	private TileRoof roof;

	public JobTarget(CookingRecipe cookingRecipe) {
		this.type = JobTargetType.COOKING_RECIPE;
		this.cookingRecipe = cookingRecipe;
	}

	public JobTarget(Entity entity) {
		this.type = JobTargetType.ENTITY;
		this.entity = entity;
	}

	public JobTarget(Construction construction) {
		this.type = JobTargetType.CONSTRUCTION;
		this.construction = construction;
	}

	public JobTarget(Bridge bridge) {
		this.type = JobTargetType.BRIDGE;
		this.bridge = bridge;
	}

	public JobTarget(MapTile tile) {
		this.type = JobTargetType.TILE;
		this.tile = tile;
	}

	public JobTarget(MapTile tile, TileRoof roof) {
		this.type = JobTargetType.ROOF;
		this.roof = roof;
		this.tile = tile;
	}

	public JobTarget(CraftingRecipe craftingRecipe, Entity craftingStation) {
		this.type = JobTargetType.CRAFTING_RECIPE;
		this.craftingRecipe = craftingRecipe;
		this.entity = craftingStation;
	}

	public CookingRecipe getCookingRecipe() {
		return cookingRecipe;
	}

	public Entity getEntity() {
		return entity;
	}

	public Construction getConstruction() {
		return construction;
	}

	public Bridge getBridge() {
		return bridge;
	}

	public MapTile getTile() {
		return tile;
	}

	public TileRoof getRoof() {
		return roof;
	}

	public GameMaterial getTargetMaterial() {
		switch (type) {
			case CRAFTING_RECIPE: {
				if (entity == null) {
					// Might happen if entity is deleted?
					return GameMaterial.NULL_MATERIAL;
				}
				InventoryComponent craftingStationInventory = entity.getComponent(InventoryComponent.class);
				if (craftingRecipe.getMaterialTypesToCopyOver() != null && !craftingRecipe.getMaterialTypesToCopyOver().isEmpty()) {
					for (QuantifiedItemTypeWithMaterial requirement : craftingRecipe.getInput()) {
						if (requirement.getItemType() != null && requirement.getItemType().getPrimaryMaterialType().equals(craftingRecipe.getMaterialTypesToCopyOver().get(0))) {
							InventoryComponent.InventoryEntry inventoryEntry = craftingStationInventory.findByItemType(requirement.getItemType(), null);
							if (inventoryEntry != null && inventoryEntry.entity.getType().equals(ITEM)) {
								return ((ItemEntityAttributes)inventoryEntry.entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
							}
						}
					}
				} else {
					for (QuantifiedItemTypeWithMaterial requirement : craftingRecipe.getInput()) {
						if (requirement.getItemType() != null) {
							InventoryComponent.InventoryEntry inventoryEntry = craftingStationInventory.findByItemType(requirement.getItemType(), null);
							if (inventoryEntry != null && inventoryEntry.entity.getType().equals(ITEM)) {
								return ((ItemEntityAttributes)inventoryEntry.entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
							}
						}
					}
				}
				break;
			}
			case TILE: {
				if (tile.hasWall()) {
					if (tile.getWall().hasOre()) {
						return tile.getWall().getOreMaterial();
					} else {
						return tile.getWall().getMaterial();
					}
				} else {
					return tile.getFloor().getMaterial();
				}
			}
			case ROOF: {
				return roof.getRoofMaterial();
			}
			case CONSTRUCTION: {
				GameMaterial constructionMaterial = construction.getPrimaryMaterial();
				if (constructionMaterial.equals(GameMaterial.NULL_MATERIAL)) {
					return null;
				} else {
					return constructionMaterial;
				}
			}
			case ENTITY: {
				switch (entity.getType()) {
					case FURNITURE:
						return ((FurnitureEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
					case PLANT:
						return ((PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getSpecies().getMaterial();
					case ITEM:
						return ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();
					default:
						return null;
				}
			}
			default:
		}
		Logger.warn("Not yet implemented: JobTarget.getTargetMaterial() for " + type);
		return null;
	}

	public Color getTargetColor() {
		switch (type) {
			case ENTITY: {
				switch (entity.getType()) {
					case PLANT: {
						PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						switch (attributes.getSpecies().getPlantType()) {
							case MUSHROOM:
							case MUSHROOM_TREE:
								return null;
							case CROP:
								if (attributes.getColor(ColoringLayer.LEAF_COLOR) != null) {
									return attributes.getColor(ColoringLayer.LEAF_COLOR);
								} else {
									return attributes.getColor(ColoringLayer.BRANCHES_COLOR);
								}
							case TREE:
							case SHRUB:
								return attributes.getColor(ColoringLayer.LEAF_COLOR);
						}
					}
					case ITEM: {
						ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						return attributes.getPrimaryMaterial().getColor();
					}
					default: {
						return null;
					}
				}
			}
		}

		GameMaterial targetMaterial = getTargetMaterial();
		if (targetMaterial != null) {
			return targetMaterial.getColor();
		}
		Logger.warn("Not yet implemented: JobTarget.getTargetColor() for " + type);
		return null;
	}

	public enum JobTargetType {

		COOKING_RECIPE,
		CRAFTING_RECIPE,
		ENTITY,
		CONSTRUCTION,
		BRIDGE,
		TILE,
		ROOF

	}

	public static JobTarget NULL_TARGET = new NullJobTarget();

	private static class NullJobTarget extends JobTarget {

		public NullJobTarget() {
			super(Entity.NULL_ENTITY);
		}

		@Override
		public GameMaterial getTargetMaterial() {
			return null;
		}

		@Override
		public Color getTargetColor() {
			return null;
		}
	}

	public static class AnimationTarget extends JobTarget {

		public AnimationTarget(Entity entity) {
			super(entity);
		}
	}
}
