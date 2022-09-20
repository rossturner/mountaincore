package technology.rocketjump.saul.production;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

public class StockpileSettings implements ChildPersistable {
    private final Set<StockpileGroup> enabledGroups = new HashSet<StockpileGroup>();
    private final Set<ItemType> enabledItemTypes = new HashSet<ItemType>();
    private final Map<ItemType, Set<GameMaterial>> enabledMaterialsByItemType = new HashMap<ItemType, Set<GameMaterial>>();
    private boolean acceptingCorpses;
    private final Set<Race> enabledRaceCorpses = new HashSet<Race>();

    public StockpileSettings clone() {
        StockpileSettings cloned = new StockpileSettings();
        cloned.enabledGroups.addAll(getEnabledGroups());
        cloned.enabledItemTypes.addAll(getEnabledItemTypes());
        cloned.enabledMaterialsByItemType.putAll(getEnabledMaterialsByItemType());
        return null;
    }


    public void toggle(StockpileGroup group, boolean enabled) {
        if (enabled) {
            getEnabledGroups().add(group);
        } else {
            getEnabledGroups().remove(group);
        }
    }

    public void toggle(ItemType itemType, boolean enabled) {
        if (enabled) {
            getEnabledItemTypes().add(itemType);
        } else {
            getEnabledItemTypes().remove(itemType);
        }
    }

    public void toggle(ItemType itemType, GameMaterial gameMaterial, boolean enabled) {
        Set<GameMaterial> materials = getEnabledMaterialsByItemType().computeIfAbsent(itemType, a -> new LinkedHashSet<>());

        if (enabled) {
            materials.add(gameMaterial);
        } else {
            materials.remove(gameMaterial);

            if (materials.isEmpty()) {
                getEnabledMaterialsByItemType().remove(itemType);
            }
        }
    }

    public void toggleCorpse(Race race, boolean enabled) {
        if (enabled) {
            getEnabledRaceCorpses().add(race);
        } else {
            getEnabledRaceCorpses().remove(race);
        }
    }

    public boolean canHold(Entity entity) {
        if (entity.getType().equals(EntityType.ITEM)) {
            ItemEntityAttributes itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
            return getEnabledItemTypes().contains(itemAttributes.getItemType()) &&
                    getEnabledMaterialsByItemType().getOrDefault(itemAttributes.getItemType(), Collections.emptySet()).contains(itemAttributes.getPrimaryMaterial());
        } else if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
            return getEnabledRaceCorpses().contains(((CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getRace());
        } else {
            return false;
        }
    }

    public boolean isAcceptingCorpses() {
        return acceptingCorpses;
    }

    public void setAcceptingCorpses(boolean acceptingCorpses) {
        this.acceptingCorpses = acceptingCorpses;
    }

    public boolean isEnabled(StockpileGroup group) {
        return getEnabledGroups().contains(group);
    }

    public boolean isEnabled(ItemType itemType) {
        return getEnabledItemTypes().contains(itemType);
    }

    public boolean isEnabled(Race race) {
        return getEnabledRaceCorpses().contains(race);
    }

    public boolean isEnabled(GameMaterial material, ItemType itemType) {
        return getEnabledMaterialsByItemType().getOrDefault(itemType, Collections.emptySet()).contains(material);
    }

    public Set<StockpileGroup> getEnabledGroups() {
        return enabledGroups;
    }

    public Set<ItemType> getEnabledItemTypes() {
        return enabledItemTypes;
    }

    public Map<ItemType, Set<GameMaterial>> getEnabledMaterialsByItemType() {
        return enabledMaterialsByItemType;
    }

    public Set<Race> getEnabledRaceCorpses() {
        return enabledRaceCorpses;
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        JSONArray enabledGroupsJson = new JSONArray();
        for (StockpileGroup enabledGroup : getEnabledGroups()) {
            enabledGroupsJson.add(enabledGroup.getName());
        }
        if (isAcceptingCorpses()) {
            enabledGroupsJson.add("CORPSES");
        }
        asJson.put("enabledGroups", enabledGroupsJson);

        JSONArray enabledItemTypesJson = new JSONArray();
        for (ItemType enabledItemType : getEnabledItemTypes()) {
            enabledItemTypesJson.add(enabledItemType.getItemTypeName());
        }
        asJson.put("enabledItemTypes", enabledItemTypesJson);

        JSONArray enabledRacesJson = new JSONArray();
        for (Race race : getEnabledRaceCorpses()) {
            enabledRacesJson.add(race.getName());
        }
        asJson.put("enabledRaces", enabledRacesJson);


        JSONObject materialMappingJson = new JSONObject(true);
        for (Map.Entry<ItemType, Set<GameMaterial>> entry : getEnabledMaterialsByItemType().entrySet()) {
            JSONArray materialNames = new JSONArray();
            for (GameMaterial material : entry.getValue()) {
                materialNames.add(material.getMaterialName());
            }
            materialMappingJson.put(entry.getKey().getItemTypeName(), materialNames);
        }
        asJson.put("enabledMaterials", materialMappingJson);
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONArray enabledGroupsJson = asJson.getJSONArray("enabledGroups");
        if (enabledGroupsJson != null) {
            for (Object item : enabledGroupsJson) {
                if (item.toString().equals("CORPSES")) {
                    this.setAcceptingCorpses(true);
                } else {
                    StockpileGroup group = relatedStores.stockpileGroupDictionary.getByName(item.toString());
                    if (group == null) {
                        throw new InvalidSaveException("Could not find stockpile group with name " + item.toString());
                    } else {
                        getEnabledGroups().add(group);
                    }
                }
            }
        }

        JSONArray enabledItemTypesJson = asJson.getJSONArray("enabledItemTypes");
        if (enabledItemTypesJson != null) {
            for (Object item : enabledItemTypesJson) {
                ItemType itemType = relatedStores.itemTypeDictionary.getByName(item.toString());
                if (itemType == null) {
                    throw new InvalidSaveException("Could not find itemType with name " + item.toString());
                } else {
                    getEnabledItemTypes().add(itemType);
                }
            }
        }

        JSONArray enabledRacesJson = asJson.getJSONArray("enabledRaces");
        if (enabledRacesJson != null) {
            for (Object item : enabledRacesJson) {
                Race race = relatedStores.raceDictionary.getByName(item.toString());
                if (race == null) {
                    throw new InvalidSaveException("Could not find race with name " + item.toString());
                } else {
                    getEnabledRaceCorpses().add(race);
                }
            }
        }

        JSONObject materialMappingJson = asJson.getJSONObject("enabledMaterials");
        if (materialMappingJson != null) {
            for (String itemNameKey : materialMappingJson.keySet()) {
                JSONArray materialNames = materialMappingJson.getJSONArray(itemNameKey);
                ItemType itemType = relatedStores.itemTypeDictionary.getByName(itemNameKey);
                if (itemType == null) {
                    throw new InvalidSaveException("Could not find itemType with name " + itemNameKey);
                }
                Set<GameMaterial> materials = new HashSet<>();
                for (Object materialName : materialNames) {
                    GameMaterial material = relatedStores.gameMaterialDictionary.getByName(materialName.toString());
                    if (material == null) {
                        throw new InvalidSaveException("Could not find material with name " + materialName.toString());
                    }
                    materials.add(material);
                }
                getEnabledMaterialsByItemType().put(itemType, materials);
            }
        }
    }
}