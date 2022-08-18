package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.List;

public class ItemAssignmentComponent implements EntityComponent, Destructible {

    public static final String JSON_KEY_HAULING_ALLOCATIONS = "haulingAllocations";
    private final List<HaulingAllocation> haulingAllocations = new ArrayList<>();

    public List<HaulingAllocation> getHaulingAllocations() {
        return haulingAllocations;
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
        for (HaulingAllocation haulingAllocation : haulingAllocations) {
            messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, haulingAllocation);
        }
        haulingAllocations.clear();
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

//        this.haulingAllocations.get(0).writeTo();

    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
//        JSONArray jsonArray = asJson.getJSONArray(JSON_KEY_HAULING_ALLOCATIONS);
//        for (int i = 0; i < jsonArray.size(); i++) {
//            JSONObject jsonObject = jsonArray.getJSONObject(i);
//            HaulingAllocation haulingAllocation = new HaulingAllocation();
//            haulingAllocation.readFrom(jsonObject, savedGameStateHolder, relatedStores);
//            this.haulingAllocations.add(haulingAllocation);
//        }
    }
}
