package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.production.FurnitureStockpile;
import technology.rocketjump.saul.production.StockpileSettings;

public class FurnitureStockpileComponent implements ParentDependentEntityComponent, Prioritisable {
    private final StockpileSettings stockpileSettings;
    private final FurnitureStockpile stockpile;
    private JobPriority priority = JobPriority.HIGHER;

    //Used in game loading
    public FurnitureStockpileComponent() {
        this(new StockpileSettings(), new FurnitureStockpile());
    }

    public FurnitureStockpileComponent(StockpileSettings stockpileSettings, FurnitureStockpile furnitureStockpile) {
        this.stockpileSettings = stockpileSettings;
        this.stockpile = furnitureStockpile;
    }

    @Override
    public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
        this.stockpile.setParentEntity(parentEntity);
    }

    @Override
    public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
        FurnitureStockpileComponent cloned = new FurnitureStockpileComponent(stockpileSettings.clone(), stockpile.clone());
        cloned.priority = getPriority();
        return cloned;
    }

    public StockpileSettings getStockpileSettings() {
        return stockpileSettings;
    }

    public FurnitureStockpile getStockpile() {
        return stockpile;
    }

    @Override
    public JobPriority getPriority() {
        return priority;
    }

    @Override
    public void setPriority(JobPriority jobPriority) {
        this.priority = jobPriority;
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        JSONObject stockpileSettingsJson = new JSONObject();
        stockpileSettings.writeTo(stockpileSettingsJson, savedGameStateHolder);
        asJson.put("stockpileSettings", stockpileSettingsJson);

        JSONObject stockpileJson = new JSONObject();
        stockpile.writeTo(stockpileJson, savedGameStateHolder);
        asJson.put("stockpile", stockpileJson);

        asJson.put("priority", priority);
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONObject stockpileSettingsJson = asJson.getJSONObject("stockpileSettings");
        if (stockpileSettingsJson != null) {
            stockpileSettings.readFrom(stockpileSettingsJson, savedGameStateHolder, relatedStores);
        }

        JSONObject stockpileJson = asJson.getJSONObject("stockpile");
        if (stockpileJson != null) {
            stockpile.readFrom(stockpileJson, savedGameStateHolder, relatedStores);
        }

        this.priority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.HIGHER);
    }

}
