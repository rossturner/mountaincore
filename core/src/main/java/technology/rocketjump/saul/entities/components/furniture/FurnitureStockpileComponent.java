package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.production.StockpileSettings;

public class FurnitureStockpileComponent implements EntityComponent {
    private final StockpileSettings stockpileSettings;

    public FurnitureStockpileComponent() {
        this(new StockpileSettings());
    }

    private FurnitureStockpileComponent(StockpileSettings stockpileSettings) {
        this.stockpileSettings = stockpileSettings;
    }

    @Override
    public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
        FurnitureStockpileComponent cloned = new FurnitureStockpileComponent(stockpileSettings.clone());


        return cloned;
    }

    public StockpileSettings getStockpileSettings() {
        return stockpileSettings;
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        JSONObject stockpileSettingsJson = new JSONObject();
        stockpileSettings.writeTo(stockpileSettingsJson, savedGameStateHolder);
        asJson.put("stockpileSettings", stockpileSettingsJson);

    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONObject stockpileSettingsJson = asJson.getJSONObject("stockpileSettings");
        if (stockpileSettingsJson != null) {
            stockpileSettings.readFrom(stockpileSettingsJson, savedGameStateHolder, relatedStores);
        }
    }
}
