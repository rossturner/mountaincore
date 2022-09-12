package technology.rocketjump.saul.ui.i18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.creature.Sanity;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamageLevel;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartOrgan;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.underground.TileLiquidFlow;
import technology.rocketjump.saul.mapping.tile.underground.UnderTile;
import technology.rocketjump.saul.mapping.tile.wall.Wall;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.rooms.Bridge;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.constructions.*;
import technology.rocketjump.saul.settlement.production.ProductionAssignment;
import technology.rocketjump.saul.zones.Zone;
import technology.rocketjump.saul.zones.ZoneClassification;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;
import static technology.rocketjump.saul.mapping.tile.underground.TileLiquidFlow.MAX_LIQUID_FLOW_PER_TILE;
import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.saul.rooms.HaulingAllocation.AllocationPositionType.ZONE;
import static technology.rocketjump.saul.ui.i18n.I18nText.BLANK;

@Singleton
public class I18nTranslator implements I18nUpdatable {

	public static DecimalFormat oneDecimalFormat = new DecimalFormat("#.#");

	private final I18nRepo repo;
	private final SkillDictionary skillDictionary;
	private final EntityStore entityStore;
	private I18nLanguageDictionary dictionary;

	@Inject
	public I18nTranslator(I18nRepo repo, SkillDictionary skillDictionary, EntityStore entityStore) {
		this.repo = repo;
		this.skillDictionary = skillDictionary;
		this.dictionary = repo.getCurrentLanguage();
		this.entityStore = entityStore;
	}

	public I18nText getTranslatedString(String i18nKey) {
		return getTranslatedString(i18nKey, I18nWordClass.UNSPECIFIED);
	}

	public I18nText getTranslatedString(String i18nKey, I18nWordClass wordClass) {
		I18nWord word = dictionary.getWord(i18nKey);
		boolean highlightAsTooltip = word.hasTooltip() && !wordClass.equals(I18nWordClass.TOOLTIP);
		String translated = word.get(wordClass);
		if (translated.contains("{{")) {
			return replaceOtherI18nKeys(translated);
		} else {
			return new I18nText(translated, highlightAsTooltip ? word.getKey() : null);
		}
	}

	public LanguageType getCurrentLanguageType() {
		if (repo.getCurrentLanguageType() != null) {
			return repo.getCurrentLanguageType();
		} else {
			return new LanguageType();
		}
	}

	public I18nLanguageDictionary getDictionary() {
		return dictionary;
	}

	public I18nText getDescription(Entity entity) {
		if (entity == null) {
			return BLANK;
		}
		switch (entity.getType()) {
			case CREATURE:
				CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getRace().getBehaviour().getIsSapient()) {
					return getSapientCreatureDescription(entity, (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
				} else {
					return getCreatureDescription(entity, (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
				}
			case ITEM:
				ItemEntityAttributes itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				return getItemDescription(itemAttributes.getQuantity(), itemAttributes.getPrimaryMaterial(),
						itemAttributes.getItemType(), itemAttributes.getItemQuality());
			case MECHANISM:
				MechanismEntityAttributes mechanismEntityAttributes = (MechanismEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				return getMechanismDescription(mechanismEntityAttributes.getMechanismType(), mechanismEntityAttributes.getPrimaryMaterial());
			case PLANT:
				return getDescription(entity, (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
			case FURNITURE:
				return getDescription((FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes());
			default:
				return new I18nText("Not yet implemented description for entity with type " + entity.getType());
		}
	}

	public I18nText getCurrentGoalDescription(Entity entity, AssignedGoal currentGoal, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (currentGoal == null || currentGoal.goal.i18nDescription == null) {
			return BLANK;
		} else {
			I18nWord description = dictionary.getWord(currentGoal.goal.i18nDescription);
			Action currentAction = currentGoal.getCurrentAction();
			if (currentAction != null && currentAction.getDescriptionOverrideI18nKey() != null) {
				description = dictionary.getWord(currentAction.getDescriptionOverrideI18nKey());
			}
			if (entity.isOnFire()) {
				description = dictionary.getWord("GOAL.ON_FIRE.DESCRIPTION");
			}

			Map<String, I18nString> replacements = new HashMap<>();

			if (currentGoal.getFoodAllocation() != null && currentGoal.getFoodAllocation().getTargetEntity() != null) {
				if (currentGoal.getFoodAllocation().getTargetEntity().getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes itemAttributes = (ItemEntityAttributes) currentGoal.getFoodAllocation().getTargetEntity().getPhysicalEntityComponent().getAttributes();
					GameMaterial material = itemAttributes.getMaterial(itemAttributes.getItemType().getPrimaryMaterialType());
					replacements.put("targetDescription", getItemDescription(1, material, itemAttributes.getItemType(), itemAttributes.getItemQuality()));
				}
			} else if (currentGoal.getLiquidAllocation() != null) {
				if (currentGoal.getLiquidAllocation().getType().equals(LiquidAllocation.LiquidAllocationType.FROM_RIVER)) {
					replacements.put("targetDescription", getTranslatedString("ACTION.DRINK_FROM_RIVER"));
				} else {
					replacements.put("targetDescription", currentGoal.getLiquidAllocation().getLiquidMaterial().getI18nValue());
				}
			} else if (currentGoal.getAssignedHaulingAllocation() != null) {
				HaulingAllocation haulingAllocation = currentGoal.getAssignedHaulingAllocation();
				Entity hauledEntity = gameContext.getEntities().get(haulingAllocation.getHauledEntityId());
				I18nString targetDescription = getDescription(hauledEntity);
				if (hauledEntity != null && hauledEntity.getType().equals(EntityType.ITEM)) {
					// Override item description to use hauled quantity
					ItemEntityAttributes hauledEntityAttributes = (ItemEntityAttributes) hauledEntity.getPhysicalEntityComponent().getAttributes();
					targetDescription = getItemDescription(haulingAllocation.getItemAllocation().getAllocationAmount(), hauledEntityAttributes.getPrimaryMaterial(), hauledEntityAttributes.getItemType(), hauledEntityAttributes.getItemQuality());
				}

				replacements.put("targetDescription", targetDescription);

				if (ZONE.equals(haulingAllocation.getTargetPositionType())) {
					MapTile targetTile = gameContext.getAreaMap().getTile(haulingAllocation.getTargetPosition());
					Optional<Zone> filteredZone = targetTile.getZones().stream().filter(zone -> zone.getClassification().getZoneType().equals(ZoneClassification.ZoneType.LIQUID_SOURCE)).findFirst();
					if (filteredZone.isPresent()) {
						replacements.put("targetZoneMaterial", filteredZone.get().getClassification().getTargetMaterial().getI18nValue());
					}
				}
			} else if (currentGoal.getAssignedJob() != null) {
				Job job = currentGoal.getAssignedJob();
				if (job.getType().getOverrideI18nKey() != null) {
					description = dictionary.getWord(job.getType().getOverrideI18nKey());
				}
				Skill requiredProfession = job.getRequiredProfession();
				if (requiredProfession == null) {
					requiredProfession = NULL_PROFESSION;
				}
				replacements.put("profession", dictionary.getWord(requiredProfession.getI18nKey()));

				if (job.getTargetId() != null) {
					Entity targetEntity = entityStore.getById(job.getTargetId());
					if (job.getCookingRecipe() != null && job.getCookingRecipe().getOutputDescriptionI18nKey() != null) {
						Map<String, I18nString> recipeReplacements = new HashMap<>();
						if (job.getCookingRecipe().getOutputMaterial() != null) {
							recipeReplacements.put("materialDescription", job.getCookingRecipe().getOutputMaterial().getI18nValue());
						} else {
							recipeReplacements.put("materialDescription", BLANK);
						}
						I18nWord descriptionWord = dictionary.getWord(job.getCookingRecipe().getOutputDescriptionI18nKey());

						if (job.getCookingRecipe().getVerbOverrideI18nKey() != null) {
							replacements.put("profession", dictionary.getWord(job.getCookingRecipe().getVerbOverrideI18nKey()));
						}

						I18nText targetDescription = applyReplacements(descriptionWord, recipeReplacements, Gender.ANY);
						replacements.put("targetDescription", targetDescription);
					} else if (targetEntity != null) {
						replacements.put("targetDescription", getDescription(targetEntity));
						if (targetEntity.getType().equals(EntityType.PLANT)) {
							replacements.put("targetPlant", getDescription(targetEntity));
						}
					} else if (job.getJobLocation() != null) {
						MapTile targetTile = gameContext.getAreaMap().getTile(job.getJobLocation());
						if (targetTile.hasConstruction()) {
							replacements.put("targetDescription", getConstructionTargetDescrption(targetTile.getConstruction()));
						}
					}

					if (targetEntity != null && targetEntity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
						CraftingStationBehaviour craftingStationBehaviour = (CraftingStationBehaviour) targetEntity.getBehaviourComponent();
						if (craftingStationBehaviour.getCurrentProductionAssignment() != null) {
							ProductionAssignment assignment = craftingStationBehaviour.getCurrentProductionAssignment();
							QuantifiedItemTypeWithMaterial output = assignment.targetRecipe.getOutput().get(0);
							I18nText targetDescription;
							// FIXME some duplication of the below
							if (output.isLiquid()) {
								targetDescription = getLiquidDescription(output.getMaterial(), output.getQuantity());
							} else {
								targetDescription = getItemDescription(output.getQuantity(),
										output.getMaterial(),
										output.getItemType(), null);
							}
							replacements.put("targetDescription", targetDescription);

							if (assignment.targetRecipe.getVerbOverrideI18nKey() != null) {
								replacements.put("profession", dictionary.getWord(assignment.targetRecipe.getVerbOverrideI18nKey()));
							}
						}
					}

				} else if (job.getJobLocation() != null) {
					MapTile targetTile = gameContext.getAreaMap().getTile(job.getJobLocation());
					if (targetTile.hasConstruction()) {
						replacements.put("targetDescription", getConstructionTargetDescrption(targetTile.getConstruction()));
					} else if (targetTile.hasDoorway()) {
						replacements.put("targetDescription", getDescription(targetTile.getDoorway().getDoorEntity()));
					} else if (targetTile.getFloor().hasBridge()) {
						replacements.put("targetDescription", getDescription(targetTile.getFloor().getBridge()));
					} else {
						replacements.put("targetDescription", getDescription(targetTile));
					}

					for (Entity targetTileEntity : targetTile.getEntities()) {
						if (targetTileEntity.getType().equals(EntityType.PLANT)) {
							replacements.put("targetPlant", getDescription(targetTileEntity));
							break;
						}
					}
				}

				if (job.getRequiredItemType() != null) {
					InventoryComponent.InventoryEntry requiredItem;
					if (job.getRequiredItemMaterial() != null) {
						requiredItem = entity.getComponent(InventoryComponent.class).findByItemTypeAndMaterial(job.getRequiredItemType(), job.getRequiredItemMaterial(), gameContext.getGameClock());
					} else {
						requiredItem = entity.getComponent(InventoryComponent.class).findByItemType(job.getRequiredItemType(), gameContext.getGameClock());
					}

					if (requiredItem != null) {
						replacements.put("requiredItem", getDescription(requiredItem.entity));
					}
				}
			}

			if (currentGoal.getRelevantMemory() != null) {
				if (currentGoal.getRelevantMemory().getRelatedEntityId() != null) {
					Entity relatedEntity = gameContext.getEntities().get(currentGoal.getRelevantMemory().getRelatedEntityId());
					if (relatedEntity != null) {
						replacements.put("targetDescription", getDescription(relatedEntity));
					}
				}
			}

			if (entity.getLocationComponent().getContainerEntity() != null) {
				replacements.put("containerDescription", getDescription(entity.getLocationComponent().getContainerEntity()));
			} else if (entity.getLocationComponent().getWorldPosition() != null) {
				// Not in container entity
				MapTile currentTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldPosition());
				replacements.put("tileDescription", getDescription(currentTile));
			}

			return applyReplacements(description, replacements, attributes.getGender());
		}
	}

	private I18nText getConstructionTargetDescrption(Construction construction) {
		// Construction might be wall, doorway or furniture
		I18nText targetDescription = BLANK;
		switch (construction.getConstructionType()) {
			case WALL_CONSTRUCTION:
				WallConstruction wallConstruction = (WallConstruction) construction;
				targetDescription = getWallDescription(wallConstruction.getPrimaryMaterial(), wallConstruction.getWallTypeToConstruct());
				break;
			case DOORWAY_CONSTRUCTION:
			case FURNITURE_CONSTRUCTION:
				FurnitureConstruction furnitureConstruction = (FurnitureConstruction) construction;
				targetDescription = getDescription(furnitureConstruction.getEntity());
				break;
			case BRIDGE_CONSTRUCTION:
				targetDescription = getDescription(((BridgeConstruction) construction).getBridge());
				break;
			default:
				Logger.error("Not yet implemented construction description for " + construction.getConstructionType());
		}
		return targetDescription;
	}

	public I18nText getDescription(Bridge bridge) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (bridge.getMaterial() != null && !NULL_MATERIAL.equals(bridge.getMaterial())) {
			replacements.put("materialType", bridge.getMaterial().getI18nValue());
		} else {
			replacements.put("materialType", I18nWord.BLANK);
		}
		replacements.put("furnitureType", dictionary.getWord(bridge.getBridgeType().getI18nKey()));
		return applyReplacements(dictionary.getWord("FURNITURE.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getWallDescription(MapTile targetTile) {
		if (targetTile == null || !targetTile.hasWall()) {
			return BLANK;
		} else {
			Wall wall = targetTile.getWall();
			if (wall.hasOre()) {
				return getWallDescription(wall.getOreMaterial(), wall.getOreType());
			} else {
				return getWallDescription(wall.getMaterial(), wall.getWallType());
			}
		}
	}

	private I18nText getWallDescription(GameMaterial gameMaterial, WallType wallType) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (gameMaterial != null && !NULL_MATERIAL.equals(gameMaterial)) {
			replacements.put("materialType", gameMaterial.getI18nValue());
		}
		if (wallType != null) {
			replacements.put("wallType", dictionary.getWord(wallType.getI18nKey()));
		}
		return applyReplacements(dictionary.getWord("WALL.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getDescription(MapTile tile) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (tile.hasWall()) {
			if (tile.getWall().hasOre()) {
				replacements.put("materialType", tile.getWall().getOreMaterial().getI18nValue());
				replacements.put("wallType", dictionary.getWord(tile.getWall().getOreType().getI18nKey()));
			} else {
				replacements.put("materialType", tile.getWall().getMaterial().getI18nValue());
				replacements.put("wallType", dictionary.getWord(tile.getWall().getWallType().getI18nKey()));
			}
			return applyReplacements(dictionary.getWord("WALL.DESCRIPTION"), replacements, Gender.ANY);
		} else {
			if (tile.isWaterSource()) {
				return getTranslatedString("FLOOR.RIVER");
			} else if (tile.hasChannel()) {
				int liquidAmount = 0;
				TileLiquidFlow liquidFlow = tile.getUnderTile().getLiquidFlow();
				if (liquidFlow != null) {
					liquidAmount = liquidFlow.getLiquidAmount();
				}

				if (liquidAmount == 0) {
					replacements.put("state", dictionary.getWord("FLOOR.CHANNEL.STATE.EMPTY"));
				} else if (liquidAmount >= MAX_LIQUID_FLOW_PER_TILE) {
					replacements.put("state", dictionary.getWord("FLOOR.CHANNEL.STATE.FULL"));
				} else {
					replacements.put("state", dictionary.getWord("FLOOR.CHANNEL.STATE.PARTIALLY_FILLED"));
				}
				return applyReplacements(dictionary.getWord("FLOOR.CHANNEL.DESCRIPTION"), replacements, Gender.ANY);
			} else {
				replacements.put("floorType", dictionary.getWord(tile.getFloor().getFloorType().getI18nKey()));
				return applyReplacements(dictionary.getWord("FLOOR.DESCRIPTION"), replacements, Gender.ANY);
			}
		}
	}

	public I18nText getPipeDescription(Entity pipeEntity, UnderTile underTile) {
		Map<String, I18nString> replacements = new HashMap<>();
		TileLiquidFlow liquidFlow = underTile.getLiquidFlow();
		int liquidAmount = 0;
		if (liquidFlow != null) {
			liquidAmount = liquidFlow.getLiquidAmount();
		}

		if (liquidAmount == 0) {
			replacements.put("state", dictionary.getWord("FLOOR.CHANNEL.STATE.EMPTY"));
		} else if (liquidAmount >= MAX_LIQUID_FLOW_PER_TILE) {
			replacements.put("state", dictionary.getWord("FLOOR.CHANNEL.STATE.FULL"));
		} else {
			replacements.put("state", dictionary.getWord("FLOOR.CHANNEL.STATE.PARTIALLY_FILLED"));
		}

		replacements.put("material", ((MechanismEntityAttributes)pipeEntity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial().getI18nValue());

		return applyReplacements(dictionary.getWord("FLOOR.PIPE.DESCRIPTION"), replacements, Gender.ANY);
	}


	public I18nText getPowerMechanismDescription(Entity powerMechanismEntity, UnderTile underTile) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("entity", getDescription(powerMechanismEntity));
		replacements.put("powerAmount", new I18nWord(String.valueOf(underTile.getPowerGrid().getTotalPowerAvailable())));
		return applyReplacements(dictionary.getWord("FLOOR.POWER.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getDescription(Construction construction) {

		if (construction instanceof FurnitureConstruction) {
			FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) construction.getEntity().getPhysicalEntityComponent().getAttributes();
			return getConstructionDescription(construction.getPrimaryMaterial(), furnitureEntityAttributes.getFurnitureType().getI18nKey());
		} else if (construction instanceof WallConstruction) {
			return getConstructionDescription(construction.getPrimaryMaterial(), ((WallConstruction) construction).getWallTypeToConstruct().getI18nKey());
		} else if (construction instanceof BridgeConstruction) {
			return getConstructionDescription(construction.getPrimaryMaterial(), ((BridgeConstruction)construction).getBridge().getBridgeType().getI18nKey());
		} else {
			Logger.error("Description of " + construction.getClass().getSimpleName() + " not yet implemented");
			return BLANK;
		}

	}

	public I18nText getDateTimeString(GameClock gameClock) {
		if (gameClock == null) {
			return new I18nText("");
		}
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("timeOfDay", new I18nWord(gameClock.getFormattedGameTime()));
		replacements.put("dayNumber", new I18nWord(String.valueOf(gameClock.getDayOfSeason())));
		replacements.put("season", dictionary.getWord(gameClock.getCurrentSeason().getI18nKey()));
		replacements.put("year", new I18nWord(String.valueOf(gameClock.getCurrentYear())));
		return applyReplacements(dictionary.getWord("GUI.DATE_TIME_LABEL"), replacements, Gender.ANY);
	}

	private I18nText getConstructionDescription(GameMaterial primaryMaterial, String furnitureTypeI18nKey) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (NULL_MATERIAL.equals(primaryMaterial)) {
			replacements.put("materialType", I18nWord.BLANK);
		} else {
			replacements.put("materialType", primaryMaterial.getI18nValue());
		}
		replacements.put("furnitureType", dictionary.getWord(furnitureTypeI18nKey));

		return applyReplacements(dictionary.getWord("CONSTRUCTION.DESCRIPTION"), replacements, Gender.ANY);
	}

	private I18nText getSapientCreatureDescription(Entity entity, CreatureEntityAttributes attributes) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("name", new I18nWord(attributes.getName().toString()));
		replacements.put("race", dictionary.getWord(attributes.getRace().getI18nKey()));

		if (attributes.getSanity().equals(Sanity.BROKEN)) {
			// TODO Other kinds of madness
			replacements.put("madness", dictionary.getWord("MADNESS.BROKEN"));
			return applyReplacements(dictionary.getWord("HUMANOID.BROKEN.DESCRIPTION"), replacements, attributes.getGender());
		} else {
			SkillsComponent skillsComponent = entity.getComponent(SkillsComponent.class);
			if (skillsComponent != null) {
				MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
				if (militaryComponent != null && militaryComponent.isInMilitary()) {
					WeaponInfo assignedWeapon = getAssignedWeapon(entity, militaryComponent.getAssignedWeaponId());
					replacements.put("race", I18nWord.BLANK);
					replacements.put("skillLevelDescription", getSkillLevelDescription(skillsComponent.getSkillLevel(assignedWeapon.getCombatSkill())));
					replacements.put("profession", dictionary.getWord(assignedWeapon.getCombatSkill().getI18nKey()));
				} else {
					Skill primaryProfession = skillsComponent.getPrimaryProfession();
					if (primaryProfession.equals(NULL_PROFESSION)) {
						replacements.put("skillLevelDescription", BLANK);
					} else {
						replacements.put("race", I18nWord.BLANK);
						replacements.put("skillLevelDescription", getSkillLevelDescription(skillsComponent.getSkillLevel(primaryProfession)));
					}
					replacements.put("profession", dictionary.getWord(primaryProfession.getI18nKey()));
				}
			} else {
				replacements.put("profession", I18nWord.BLANK);
			}

			return applyReplacements(dictionary.getWord("HUMANOID.DESCRIPTION"), replacements, attributes.getGender());
		}
	}

	private WeaponInfo getAssignedWeapon(Entity entity, Long assignedWeaponId) {
		if (assignedWeaponId != null) {
			Entity weaponEntity = entityStore.getById(assignedWeaponId);
			if (weaponEntity != null && weaponEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes) {
				if (itemAttributes.getItemType().getWeaponInfo() != null) {
					return itemAttributes.getItemType().getWeaponInfo();
				}
			}
		}

		if (entity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
			if (creatureEntityAttributes.getRace().getFeatures().getUnarmedWeapon() != null) {
				return creatureEntityAttributes.getRace().getFeatures().getUnarmedWeapon();
			}
		}

		return WeaponInfo.UNARMED;
	}

	public I18nText getSkilledProfessionDescription(Skill profession, int skillLevel, Gender gender) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("profession", dictionary.getWord(profession.getI18nKey()));
		replacements.put("skillLevelDescription", getSkillLevelDescription(skillLevel));
		return applyReplacements(dictionary.getWord("PROFESSION.DESCRIPTION"), replacements, gender);
	}

	public I18nString getSkillLevelDescription(int skillLevel) {
		int skillLevelDescriptionIndex;
		if (skillLevel <= 19) {
			skillLevelDescriptionIndex = 1;
		} else  if (skillLevel <= 39) {
			skillLevelDescriptionIndex = 2;
		} else if (skillLevel <= 49) {
			skillLevelDescriptionIndex = 3;
		} else if (skillLevel <= 59) {
			skillLevelDescriptionIndex = 4;
		} else if (skillLevel <= 69) {
			skillLevelDescriptionIndex = 5;
		} else if (skillLevel <= 79) {
			skillLevelDescriptionIndex = 6;
		} else if (skillLevel <= 89) {
			skillLevelDescriptionIndex = 7;
		} else if (skillLevel <= 99) {
			skillLevelDescriptionIndex = 8;
		} else {
			skillLevelDescriptionIndex = 9;
		}
		return dictionary.getWord("PROFESSION.SKILL_LEVEL." + skillLevelDescriptionIndex);
	}


	private I18nText getCreatureDescription(Entity entity, CreatureEntityAttributes attributes) {
		I18nWord raceWord = dictionary.getWord(attributes.getRace().getI18nKey());
		return new I18nText(raceWord.get(I18nWordClass.NOUN, attributes.getGender()), raceWord.hasTooltip() ? raceWord.getKey() : null);
	}

	public I18nText getItemDescription(int quantity, GameMaterial material, ItemType itemType, ItemQuality itemQuality) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("quantity", new I18nWord(String.valueOf(quantity)));
		if (material == null) {
			replacements.put("materialType", I18nWord.BLANK);
		} else {
			replacements.put("materialType", material.getI18nValue());
		}
		if (itemQuality == null || itemQuality.i18nKey == null) {
			replacements.put("quality", I18nWord.BLANK);
		} else {
			replacements.put("quality", dictionary.getWord(itemQuality.i18nKey));
		}
		if (itemType == null) {
			replacements.put("itemType", I18nWord.BLANK);
			return applyReplacements(dictionary.getWord("ITEM.DESCRIPTION"), replacements, Gender.ANY);
		} else {
			replacements.put("itemType", dictionary.getWord(itemType.getI18nKey()));
		}

		if (itemType.isDescribeAsMaterialOnly()) {
			return applyReplacements(dictionary.getWord("ITEM.INGREDIENT.DESCRIPTION"), replacements, Gender.ANY);
		} else {
			return applyReplacements(dictionary.getWord("ITEM.DESCRIPTION"), replacements, Gender.ANY);
		}
	}

	public I18nText getMechanismDescription(MechanismType mechanismType, GameMaterial material) {
		Map<String, I18nString> replacements = new HashMap<>();
		if (material == null) {
			replacements.put("materialType", I18nWord.BLANK);
		} else {
			replacements.put("materialType", material.getI18nValue());
		}
		if (mechanismType == null) {
			replacements.put("itemType", I18nWord.BLANK);
			return applyReplacements(dictionary.getWord("ITEM.DESCRIPTION"), replacements, Gender.ANY);
		} else {
			replacements.put("itemType", dictionary.getWord(mechanismType.getI18nKey()));
		}

		return applyReplacements(dictionary.getWord("ITEM.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getLiquidDescription(GameMaterial material, float quantity) {
		return applyReplacements(new I18nWord("{{material}} ({{quantity}})"),
				Map.of("material", material.getI18nValue(), "quantity", new I18nWord(oneDecimalFormat.format(quantity))), Gender.ANY);
	}

	public I18nText getItemAllocationDescription(int numberAllocated, QuantifiedItemTypeWithMaterial requirement) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("quantity", new I18nWord(String.valueOf(numberAllocated)));
		replacements.put("total", new I18nWord(String.valueOf(requirement.getQuantity())));
		replacements.put("itemDescription", getItemDescription(requirement.getQuantity(), requirement.getMaterial(), requirement.getItemType(), null));

		return applyReplacements(dictionary.getWord("CONSTRUCTION.ITEM_ALLOCATION"), replacements, Gender.ANY);
	}

	private I18nText getDescription(Entity entity, PlantEntityAttributes attributes) {
		Map<String, I18nString> replacements = new HashMap<>();

		switch (attributes.getSpecies().getPlantType()) {
			case TREE:
			case MUSHROOM_TREE:
				replacements.put("materialType", attributes.getSpecies().getMaterial().getI18nValue());
				return applyReplacements(dictionary.getWord("TREE.DESCRIPTION"), replacements, Gender.ANY);
			case SHRUB:
				return applyReplacements(dictionary.getWord("SHRUB.DESCRIPTION"), replacements, Gender.ANY);
			case MUSHROOM:
				replacements.put("materialType", attributes.getSpecies().getMaterial().getI18nValue());
				return applyReplacements(dictionary.getWord("MUSHROOM.DESCRIPTION"), replacements, Gender.ANY);
			case CROP:
				if (attributes.getSpecies().getSeed() != null) {
					// TODO replace with actual plant material
					replacements.put("materialType", attributes.getSpecies().getSeed().getSeedMaterial().getI18nValue());
				}
				return applyReplacements(dictionary.getWord("CROP.DESCRIPTION"), replacements, Gender.ANY);
			default:
				return new I18nText("Not yet implemented description for " + attributes.getSpecies().getPlantType());
		}
	}

	private I18nText getDescription(FurnitureEntityAttributes attributes) {
		Map<String, I18nString> replacements = new HashMap<>();

		GameMaterial gameMaterial = attributes.getPrimaryMaterial();
		if (gameMaterial != null && !NULL_MATERIAL.equals(gameMaterial)) {
			replacements.put("materialType", gameMaterial.getI18nValue());
		} else {
			replacements.put("materialType", I18nWord.BLANK);
		}
		if (attributes.getFurnitureType() != null && attributes.getFurnitureType().getI18nKey() != null) {
			replacements.put("furnitureType", dictionary.getWord(attributes.getFurnitureType().getI18nKey()));
		} else {
			replacements.put("furnitureType", I18nWord.BLANK);
		}

		return applyReplacements(dictionary.getWord("FURNITURE.DESCRIPTION"), replacements, Gender.ANY);
	}

	public I18nText getTranslatedWordWithReplacements(String i18nKey, Map<String, I18nString> replacements) {
		I18nWord word = dictionary.getWord(i18nKey);
		return applyReplacements(word, replacements, Gender.ANY);
	}

	public I18nText applyReplacements(I18nWord originalWord, Map<String, I18nString> replacements, Gender gender) {
		String string = originalWord.get(I18nWordClass.UNSPECIFIED, gender);
		I18nText i18nText = new I18nText(string, originalWord.hasTooltip() ? originalWord.get(I18nWordClass.TOOLTIP) : null);

		String REGEX_START = Pattern.quote("{{");
		String REGEX_END = Pattern.quote("}}");
		Pattern pattern = Pattern.compile(REGEX_START + "([\\w\\\\.]+)" + REGEX_END);


		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			String token = matcher.group(0);
			token = token.substring(2, token.length() - 2);

			I18nString replacement;
			I18nWordClass replacementWordclass = I18nWordClass.UNSPECIFIED;

			if (token.equals("quantity_if_multiple")) {
				if (getQuantity(replacements) > 1) {
					replacement = new I18nWord(String.valueOf(getQuantity(replacements)));
				} else {
					replacement = I18nWord.BLANK;
				}
			} else if (token.toUpperCase().equals("BLANK")) {
				// Always replace {{BLANK}} with ""
				replacement = I18nWord.BLANK;

			} else if (token.contains(".")) {
				// Only expecting one . for now
				String[] split = token.split("\\.");
				replacement = replacements.getOrDefault(split[0], new I18nWord(split[0]));
				if (split[split.length - 1].equals("noun_or_plural")) {
					if (getQuantity(replacements) > 1) {
						replacementWordclass = I18nWordClass.PLURAL;
					} else {
						replacementWordclass = I18nWordClass.NOUN;
					}
				} else {
					replacementWordclass = I18nWordClass.valueOf(split[split.length - 1].toUpperCase());
				}
			} else {
				replacement = replacements.getOrDefault(token, I18nWord.BLANK);
			}

			if (replacement instanceof I18nWord) {
				I18nWord replacementWord = (I18nWord)replacement;

				String replacementText = replacementWord.get(replacementWordclass, gender);
				if (!token.equals("name")) {
					replacementText = replacementText.toLowerCase();
				}

				i18nText.replace(matcher.group(0), replacementText, replacementWord.hasTooltip() ? replacementWord.getKey() : null);
			} else if (replacement instanceof I18nText) {
				I18nText replacementText = (I18nText) replacement;
				i18nText.replace(matcher.group(0), replacementText);
			} else {
				if (replacement == null) {
					Logger.error("Replacement in applyReplacements is null, needs investigating");
					i18nText.replace(matcher.group(0), BLANK);
				} else {
					Logger.error("Not yet implemented: " + replacement.getClass().getSimpleName());
				}
			}

		}

		return i18nText.tidy(true);
	}


	public I18nText replaceOtherI18nKeys(String text) {
		I18nText i18nText = new I18nText(text);

		String REGEX_START = Pattern.quote("{{");
		String REGEX_END = Pattern.quote("}}");
		Pattern pattern = Pattern.compile(REGEX_START + "([\\w\\\\.]+)" + REGEX_END);

		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String token = matcher.group(0);
			token = token.substring(2, token.length() - 2);

			I18nWord replacement = I18nWord.BLANK;
			I18nWordClass replacementWordclass = I18nWordClass.UNSPECIFIED;

			if (token.contains(".")) {
				String[] parts = token.split("\\.");
				String lastPart = parts[parts.length - 1].toUpperCase();
				I18nWordClass wordClass = EnumUtils.getEnum(I18nWordClass.class, lastPart);
				String[] partsWithoutWordClass = parts;
				if (wordClass != null) {
					partsWithoutWordClass = Arrays.copyOfRange(parts, 0, parts.length - 1);
					replacementWordclass = wordClass;
				}

				String rejoinedParts = StringUtils.join(partsWithoutWordClass, ".").toUpperCase();
				if (dictionary.containsKey(rejoinedParts)) {
					replacement = dictionary.getWord(rejoinedParts);
				}
			}

			String replacementText = replacement.get(replacementWordclass, Gender.ANY).toLowerCase();

			i18nText.replace(matcher.group(0), replacementText, replacement.hasTooltip() ? replacement.getKey() : null);
		}

		return i18nText.tidy(true);
	}

	private int getQuantity(Map<String, I18nString> replacements) {
		I18nString quantity = replacements.get("quantity");
		if (quantity == null) {
			return 0;
		} else if (quantity instanceof I18nWord) {
			I18nWord quantityWord = (I18nWord) quantity;
			return Integer.valueOf(quantityWord.get(I18nWordClass.UNSPECIFIED));
		} else {
			Logger.error("Not yet implemented: quantity from " + quantity.getClass().getSimpleName());
			return 0;
		}
	}

	public I18nText getAssignedToLabel(Entity assignedToEntity) {
		if (assignedToEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes) {
			Map<String, I18nString> replacements = new HashMap<>();
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) assignedToEntity.getPhysicalEntityComponent().getAttributes();
			replacements.put("name", new I18nWord(attributes.getName().toString())); // Names aren't translated
			return applyReplacements(dictionary.getWord("GUI.FURNITURE_ASSIGNED_TO"), replacements, Gender.ANY);
		} else {
			Logger.error("Furniture is assigned to a non-humanoid entity");
			return BLANK;
		}
	}
	public I18nText getConstructionStatusDescription(Construction construction) {
		List<I18nText> descriptions = getConstructionStatusDescriptions(construction);
		return descriptions.get(0);
	}

	public List<I18nText> getConstructionStatusDescriptions(Construction construction) {
		List<I18nText> descriptions = new ArrayList<>();

		if (ConstructionState.SELECTING_MATERIALS == construction.getState()) {
			for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
				if (requirement.getMaterial() == null) {
					Map<String, I18nString> replacements = new HashMap<>();
					I18nWord word = dictionary.getWord("CONSTRUCTION.STATUS.SELECTING_MATERIALS");
					ItemType missingItemType = requirement.getItemType();

					if (missingItemType != null) {
						replacements.put("materialType", dictionary.getWord(missingItemType.getPrimaryMaterialType().getI18nKey()));
						replacements.put("itemDescription", dictionary.getWord(missingItemType.getI18nKey()));
					}

					descriptions.add(applyReplacements(word, replacements, Gender.ANY));
				}
			}
		} else {
			Map<String, I18nString> replacements = new HashMap<>();
			I18nWord word;
			switch (construction.getState()) {
				case CLEARING_WORK_SITE:
					word = dictionary.getWord("CONSTRUCTION.STATUS.CLEARING_WORK_SITE");
					break;
				case WAITING_FOR_RESOURCES:
					word = dictionary.getWord("CONSTRUCTION.STATUS.WAITING_FOR_RESOURCES");
					break;
				case WAITING_FOR_COMPLETION:
					word = dictionary.getWord("CONSTRUCTION.STATUS.WAITING_FOR_COMPLETION");
					break;
				default:
					Logger.error("Not yet implemented: Construction state description for " + construction.getState());
					return List.of(BLANK);
			}

			descriptions.add(applyReplacements(word, replacements, Gender.ANY));
		}

		return descriptions;
	}

	public I18nText getHarvestProgress(float progress) {
		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("progress", new I18nWord("progress", oneDecimalFormat.format(progress)));
		return applyReplacements(dictionary.getWord("CROP.HARVEST_PROGRESS"), replacements, Gender.ANY);
	}

	public I18nText getDynamicMaterialDescription(GameMaterial gameMaterial) {
		I18nWord descriptionWord = dictionary.getWord(gameMaterial.getI18nKey());
		Map<String, I18nString> replacements = new HashMap<>();

		if (gameMaterial.getConstituentMaterials() != null && !gameMaterial.getConstituentMaterials().isEmpty()) {
			Iterator<GameMaterial> iterator = gameMaterial.getConstituentMaterials().iterator();
			if (gameMaterial.getConstituentMaterials().size() == 1) {
				replacements.put("materialDescription", iterator.next().getI18nValue());
			} else if (gameMaterial.getConstituentMaterials().size() == 2) {
				replacements.put("materialOne", iterator.next().getI18nValue());
				replacements.put("materialTwo", iterator.next().getI18nValue());

				replacements.put("materialDescription", applyReplacements(dictionary.getWord("COOKING.DUAL_INGREDIENT.DESCRIPTION"), replacements, Gender.ANY));
			} else if (gameMaterial.getConstituentMaterials().size() == 3) {
				replacements.put("materialOne", iterator.next().getI18nValue());
				replacements.put("materialTwo", iterator.next().getI18nValue());
				replacements.put("materialThree", iterator.next().getI18nValue());

				replacements.put("materialDescription", applyReplacements(dictionary.getWord("COOKING.TRIPLE_INGREDIENT.DESCRIPTION"), replacements, Gender.ANY));
			} else {
				replacements.put("materialDescription", dictionary.getWord(iterator.next().getMaterialType().getI18nKey()));
			}
		}

		return applyReplacements(descriptionWord, replacements, Gender.ANY);
	}

	public String getDescription(BodyPart bodyPart) {
		return (bodyPart.getDiscriminator() != null ? bodyPart.getDiscriminator() + " " : "") + bodyPart.getPartDefinition().getName();
	}

	@Override
	public void onLanguageUpdated() {
		dictionary = repo.getCurrentLanguage();
	}

	public I18nText getDamageDescription(BodyPart bodyPart, BodyPartDamageLevel damageLevel) {
		if (bodyPart.getDiscriminator() != null) {
			return applyReplacements(
					dictionary.getWord("CREATURE.DAMAGE.DESCRIPTION_B"),
					Map.of(
						"damageType", dictionary.getWord(damageLevel.i18nKey()),
						"discriminator", dictionary.getWord(bodyPart.getDiscriminator().i18nKey()),
						"bodyPart", dictionary.getWord(bodyPart.getPartDefinition().getI18nKey())
					),
					Gender.ANY
			);
		} else {
			return applyReplacements(
					dictionary.getWord("CREATURE.DAMAGE.DESCRIPTION_A"),
					Map.of(
						"damageType", dictionary.getWord(damageLevel.i18nKey()),
						"bodyPart", dictionary.getWord(bodyPart.getPartDefinition().getI18nKey())
					),
					Gender.ANY
			);
		}
	}

	public I18nText getDamageDescription(BodyPartOrgan bodyPartOrgan, OrganDamageLevel organDamageLevel) {
		if (bodyPartOrgan.getDiscriminator() != null) {
			return applyReplacements(
					dictionary.getWord("CREATURE.DAMAGE.DESCRIPTION_B"),
					Map.of(
							"damageType", dictionary.getWord(organDamageLevel.i18nKey()),
							"discriminator", dictionary.getWord(bodyPartOrgan.getDiscriminator().i18nKey()),
							"bodyPart", dictionary.getWord(bodyPartOrgan.getOrganDefinition().getI18nKey())
					),
					Gender.ANY
			);
		} else {
			return applyReplacements(
					dictionary.getWord("CREATURE.DAMAGE.DESCRIPTION_A"),
					Map.of(
							"damageType", dictionary.getWord(organDamageLevel.i18nKey()),
							"bodyPart", dictionary.getWord(bodyPartOrgan.getOrganDefinition().getI18nKey())
					),
					Gender.ANY
			);
		}
	}
}
