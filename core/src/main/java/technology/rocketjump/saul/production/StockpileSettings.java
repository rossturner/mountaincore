package technology.rocketjump.saul.production;

import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.*;

public class StockpileSettings {
    private final Set<StockpileGroup> enabledGroups = new HashSet<StockpileGroup>();
    private final Set<ItemType> enabledItemTypes = new HashSet<ItemType>();
    private final Map<ItemType, Set<GameMaterial>> enabledMaterialsByItemType = new HashMap<ItemType, Set<GameMaterial>>();
    private boolean acceptingCorpses;
    private final Set<Race> enabledRaceCorpses = new HashSet<Race>();


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

}