package technology.rocketjump.mountaincore.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class CopyGameMaterialsFromInventoryComponent implements EntityComponent {
    private static final String JSON_KEY_GAME_MATERIAL_TYPES = "gameMaterialTypes";
    private List<GameMaterialType> gameMaterialTypes;

    public void setGameMaterialTypes(List<GameMaterialType> gameMaterialTypes) {
        this.gameMaterialTypes = gameMaterialTypes;
    }

    public List<GameMaterialType> getGameMaterialTypes() {
        return gameMaterialTypes;
    }

    @Override
    public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
        CopyGameMaterialsFromInventoryComponent clone = new CopyGameMaterialsFromInventoryComponent();
        clone.setGameMaterialTypes(this.getGameMaterialTypes());
        return clone;
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        JSONArray jsonArray = new JSONArray();
        for (GameMaterialType materialType : getGameMaterialTypes()) {
            jsonArray.add(materialType.name());
        }
        asJson.put(JSON_KEY_GAME_MATERIAL_TYPES, jsonArray);
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONArray jsonArray = asJson.getJSONArray(JSON_KEY_GAME_MATERIAL_TYPES);
        this.setGameMaterialTypes(new ArrayList<>(jsonArray.size()));
        try {
            for (int i = 0; i < jsonArray.size(); i++) {
                GameMaterialType materialType = GameMaterialType.valueOf(jsonArray.getString(i));
                getGameMaterialTypes().add(materialType);
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidSaveException(e.getMessage());
        }
    }

    public void apply(Entity itemToPlace, Entity targetFurniture) {
        if (
                itemToPlace.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes &&
                targetFurniture.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes furnitureAttributes
        ) {

            for (GameMaterialType materialType : gameMaterialTypes) {
                GameMaterial itemMaterial = itemAttributes.getMaterial(materialType);
                if (itemMaterial != null) {
                    furnitureAttributes.setMaterial(itemMaterial);
                }
            }
        }
    }
}
