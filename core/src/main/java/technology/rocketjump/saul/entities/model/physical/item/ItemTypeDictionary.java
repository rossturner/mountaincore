package technology.rocketjump.saul.entities.model.physical.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.jobs.model.SkillType;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.rooms.StockpileGroup;
import technology.rocketjump.saul.rooms.StockpileGroupDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.emptyList;
import static technology.rocketjump.saul.entities.model.physical.item.ItemType.UNARMED_WEAPON;

@Singleton
public class ItemTypeDictionary {

	private final CraftingTypeDictionary craftingTypeDictionary;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ConstantsRepo constantsRepo;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final SkillDictionary skillDictionary;
	private Map<String, ItemType> byName = new HashMap<>();
	private List<ItemType> allTypesList = new ArrayList<>();
	private List<ItemType> itemTypesWithWeaponInfo = new ArrayList<>();
	private Map<CraftingType, List<ItemType>> byCraftingType = new HashMap<>();
	private Map<AmmoType, List<ItemType>> byAmmoType = new HashMap<>();
	private final Map<StockpileGroup, List<ItemType>> byStockpileGroup = new HashMap<>();
	private final Map<Class<? extends Tag>, List<ItemType>> byTag = new HashMap<>();

	@Inject
	public ItemTypeDictionary(CraftingTypeDictionary craftingTypeDictionary,
							  StockpileGroupDictionary stockpileGroupDictionary,
							  SoundAssetDictionary soundAssetDictionary,
							  ConstantsRepo constantsRepo,
							  ParticleEffectTypeDictionary particleEffectTypeDictionary,
							  SkillDictionary skillDictionary) throws IOException {
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.constantsRepo = constantsRepo;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		this.skillDictionary = skillDictionary;
		ObjectMapper objectMapper = new ObjectMapper();
		File itemTypeJsonFile = new File("assets/definitions/types/itemTypes.json");
		List<ItemType> itemTypeList = objectMapper.readValue(FileUtils.readFileToString(itemTypeJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, ItemType.class));

		itemTypeList.sort(Comparator.comparing(ItemType::getItemTypeName));

		for (ItemType itemType : itemTypeList) {
			add(itemType);
		}

		byName.put(UNARMED_WEAPON.getItemTypeName(), UNARMED_WEAPON);

		itemTypesWithWeaponInfo.sort(Comparator.comparing(ItemType::getItemTypeName));

		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			if (craftingType.getDefaultItemTypeName() != null) {
				craftingType.setDefaultItemType(getByName(craftingType.getDefaultItemTypeName()));
			}
		}
	}

	public void add(ItemType itemType) {
		if (itemType.getRelatedCraftingTypeNames() == null) {
			itemType.setRelatedCraftingTypeNames(new ArrayList<>());
		}
		if (itemType.getRelatedCraftingTypes() == null) {
			itemType.setRelatedCraftingTypes(new ArrayList<>());
		}
		for (String relatedCraftingName : itemType.getRelatedCraftingTypeNames()) {
			CraftingType craftingType = craftingTypeDictionary.getByName(relatedCraftingName);
			if (craftingType == null) {
				Logger.error("Could not find related crafting type by name: " + relatedCraftingName + " for " + itemType.getItemTypeName());
			} else {
				itemType.getRelatedCraftingTypes().add(craftingType);

				List<ItemType> itemTypesByCraftingType = byCraftingType.computeIfAbsent(craftingType, k -> new ArrayList<>());
				itemTypesByCraftingType.add(itemType);
			}
		}

		if (itemType.getStockpileGroupName() != null) {
			itemType.setStockpileGroup(stockpileGroupDictionary.getByName(itemType.getStockpileGroupName()));
			if (itemType.getStockpileGroup() == null) {
				Logger.error("Could not find stockpile group '"+ itemType.getStockpileGroupName()+"' for itemType " + itemType.getItemTypeName());
			} else {
				byStockpileGroup.computeIfAbsent(itemType.getStockpileGroup(), a -> new ArrayList<>()).add(itemType);
			}
		}

		if (itemType.getPlacementSoundAssetName() != null) {
			itemType.setPlacementSoundAsset(soundAssetDictionary.getByName(itemType.getPlacementSoundAssetName()));
			if (itemType.getPlacementSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + itemType.getPlacementSoundAssetName() + " for item type " + itemType.getItemTypeName());
			}
		}

		if (itemType.getConsumeSoundAssetName() != null) {
			itemType.setConsumeSoundAsset(soundAssetDictionary.getByName(itemType.getConsumeSoundAssetName()));
			if (itemType.getConsumeSoundAsset() == null) {
				Logger.error("Could not find sound asset with name " + itemType.getConsumeSoundAssetName() + " for item type " + itemType.getItemTypeName());
			}
		}

		byName.put(itemType.getItemTypeName(), itemType);
		allTypesList.add(itemType);

		if (itemType.getWeaponInfo() != null) {
			if (itemType.getWeaponInfo().getFireWeaponSoundAssetName() != null) {
				itemType.getWeaponInfo().setFireWeaponSoundAsset(soundAssetDictionary.getByName(itemType.getWeaponInfo().getFireWeaponSoundAssetName()));
				if (itemType.getWeaponInfo().getFireWeaponSoundAsset() == null) {
					Logger.error(String.format("Could not find sound asset with name %s for item type %s", itemType.getWeaponInfo().getFireWeaponSoundAssetName(), itemType.getItemTypeName()));
				}
			}

			if (itemType.getWeaponInfo().getWeaponHitSoundAssetName() != null) {
				itemType.getWeaponInfo().setWeaponHitSoundAsset(soundAssetDictionary.getByName(itemType.getWeaponInfo().getWeaponHitSoundAssetName()));
				if (itemType.getWeaponInfo().getWeaponHitSoundAsset() == null) {
					Logger.error(String.format("Could not find sound asset with name %s for item type %s", itemType.getWeaponInfo().getWeaponHitSoundAssetName(), itemType.getItemTypeName()));
				}
			}

			if (itemType.getWeaponInfo().getWeaponMissSoundAssetName() != null) {
				itemType.getWeaponInfo().setWeaponMissSoundAsset(soundAssetDictionary.getByName(itemType.getWeaponInfo().getWeaponMissSoundAssetName()));
				if (itemType.getWeaponInfo().getWeaponMissSoundAsset() == null) {
					Logger.error(String.format("Could not find sound asset with name %s for item type %s", itemType.getWeaponInfo().getWeaponMissSoundAssetName(), itemType.getItemTypeName()));
				}
			}

			if (itemType.getWeaponInfo().getAnimatedSpriteEffectName() != null) {
				itemType.getWeaponInfo().setAnimatedEffectType(particleEffectTypeDictionary.getByName(itemType.getWeaponInfo().getAnimatedSpriteEffectName()));
				if (itemType.getWeaponInfo().getAnimatedEffectType() == null) {
					Logger.error(String.format("Could not find particle effect with name %s for item type %s", itemType.getWeaponInfo().getAnimatedSpriteEffectName(), itemType.getItemTypeName()));
				} else if (itemType.getWeaponInfo().getAnimatedEffectType().getAnimatedSpriteName() == null) {
					Logger.error(String.format("Particle effect %s is not an animated-sprite type particle effect, for %s",
							itemType.getWeaponInfo().getAnimatedEffectType().getName(), itemType.getItemTypeName()));
				}
			}

			if (itemType.getWeaponInfo().getCombatSkillName() != null) {
				itemType.getWeaponInfo().setCombatSkill(skillDictionary.getByName(itemType.getWeaponInfo().getCombatSkillName()));
				if (itemType.getWeaponInfo().getCombatSkill() == null) {
					Logger.error("Could not find combat skill with name %s for item type %s", itemType.getWeaponInfo().getCombatSkillName(), itemType.getItemTypeName());
				} else if (!itemType.getWeaponInfo().getCombatSkill().getType().equals(SkillType.COMBAT_SKILL)) {
					Logger.error("Combat skill with name %s for item type %s is not a COMBAT_SKILL-type skill", itemType.getWeaponInfo().getCombatSkillName(), itemType.getItemTypeName());
				}
			}

			itemTypesWithWeaponInfo.add(itemType);
		}
		if (itemType.getIsAmmoType() != null) {
			byAmmoType.computeIfAbsent(itemType.getIsAmmoType(), a -> new ArrayList<>()).add(itemType);
		}
	}

	public ItemType getByName(String itemTypeName) {
		return byName.get(itemTypeName);
	}

	public List<ItemType> getAll() {
		return allTypesList;
	}

	public List<ItemType> getByCraftingType(CraftingType craftingType) {
		return byCraftingType.getOrDefault(craftingType, emptyList());
	}

	public List<ItemType> getByStockpileGroup(StockpileGroup stockpileGroup) {
		return byStockpileGroup.getOrDefault(stockpileGroup, emptyList());
	}

	public List<ItemType> getByTagClass(Class<? extends Tag> tagClass) {
		return byTag.getOrDefault(tagClass, emptyList());
	}

	public ConstantsRepo getConstantsRepo() {
		return constantsRepo;
	}

	public void tagsProcessed() {
		for (ItemType itemType : getAll()) {
			for (Tag tag : itemType.getProcessedTags()) {
				byTag.computeIfAbsent(tag.getClass(), a -> new ArrayList<>()).add(itemType);
			}
		}
	}

	public List<ItemType> getAllWeapons() {
		return itemTypesWithWeaponInfo;
	}

	public Collection<ItemType> getByAmmoType(AmmoType ammoType) {
		return byAmmoType.getOrDefault(ammoType, List.of());
	}

}
