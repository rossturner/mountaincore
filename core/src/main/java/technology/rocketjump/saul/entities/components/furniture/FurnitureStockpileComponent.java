package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.production.FurnitureStockpile;
import technology.rocketjump.saul.production.StockpileSettings;

public class FurnitureStockpileComponent implements ParentDependentEntityComponent {
    private final StockpileSettings stockpileSettings;
    private FurnitureStockpile stockpile;

    public FurnitureStockpileComponent() {
        this(new StockpileSettings());
    }

    private FurnitureStockpileComponent(StockpileSettings stockpileSettings) {
        this.stockpileSettings = stockpileSettings;
    }

    @Override
    public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
        this.stockpile = new FurnitureStockpile(parentEntity);
    }

    @Override
    public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
        FurnitureStockpileComponent cloned = new FurnitureStockpileComponent(stockpileSettings.clone());
        //TODO: clone allocations

        return cloned;
    }

    public StockpileSettings getStockpileSettings() {
        return stockpileSettings;
    }

    public FurnitureStockpile getStockpile() {
        return stockpile;
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        JSONObject stockpileSettingsJson = new JSONObject();
        stockpileSettings.writeTo(stockpileSettingsJson, savedGameStateHolder);
        asJson.put("stockpileSettings", stockpileSettingsJson);

        //TODO: persist stockpile allocations
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONObject stockpileSettingsJson = asJson.getJSONObject("stockpileSettings");
        if (stockpileSettingsJson != null) {
            stockpileSettings.readFrom(stockpileSettingsJson, savedGameStateHolder, relatedStores);
        }

        //TODO: persist stockpile allocations
    }

}
