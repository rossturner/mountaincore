package technology.rocketjump.mountaincore.entities.components.creature;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.List;

public class ItemAssignmentComponent implements EntityComponent, Destructible {

    private final List<HaulingAllocation> haulingAllocations = new ArrayList<>();

    public List<HaulingAllocation> getHaulingAllocations() {
        return haulingAllocations;
    }

    public HaulingAllocation getByHauledItemId(long itemId) {
        return haulingAllocations.stream()
                .filter(alloc -> alloc.getItemAllocation() != null && alloc.getItemAllocation().getTargetItemEntityId() == itemId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
        return null;
    }

    public HaulingAllocation findByItemType(ItemType itemType, GameContext gameContext) {
        for (HaulingAllocation haulingAllocation : haulingAllocations) {
            Long targetItemEntityId = haulingAllocation.getItemAllocation().getTargetItemEntityId();
            Entity targetEntity = gameContext.getEntities().get(targetItemEntityId);
            if (targetEntity != null && targetEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
                if (attributes.getItemType().equals(itemType)) {
                    return haulingAllocation;
                }
            }
        }
        return null;
    }

    @Override
    public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
        for (HaulingAllocation haulingAllocation : new ArrayList<>(haulingAllocations)) {
            messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, haulingAllocation);
        }
        haulingAllocations.clear();
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        JSONArray jsonArray = new JSONArray();
        for (HaulingAllocation haulingAllocation : haulingAllocations) {
            haulingAllocation.writeTo(savedGameStateHolder);
            jsonArray.add(haulingAllocation.getHaulingAllocationId());
        }
        asJson.put("assignedHaulingAllocationIds", jsonArray);
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONArray jsonArray = asJson.getJSONArray("assignedHaulingAllocationIds");
        for (int i = 0; i < jsonArray.size(); i++) {
            Long associatedHaulingAllocationId = jsonArray.getLong(i);
            HaulingAllocation haulingAllocation = savedGameStateHolder.haulingAllocations.get(associatedHaulingAllocationId);
            if (haulingAllocation == null) {
                throw new InvalidSaveException("Could not find hauling allocation with ID " + associatedHaulingAllocationId);
            } else {
                this.haulingAllocations.add(haulingAllocation);
            }
        }
    }
}
