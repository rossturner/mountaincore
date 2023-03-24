package technology.rocketjump.mountaincore.constants;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.messaging.MessageType;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

@Singleton
public class ConstantsRepo implements GameContextAware, Telegraph {

	private final WorldConstants worldConstants;
	private final UiConstants uiConstants;
	private final SettlementConstants settlementConstants;
	private JSONObject rawJson;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	public ConstantsRepo(MessageDispatcher messageDispatcher) throws IOException {
		File jsonFile = new File("assets/definitions/constants.json");
		String rawFileContents = FileUtils.readFileToString(jsonFile);
		this.rawJson = JSON.parseObject(rawFileContents);

		this.worldConstants = objectMapper.readValue(rawFileContents, WorldConstants.class);
		this.uiConstants = objectMapper.readValue(rawFileContents, UiConstants.class);
		this.settlementConstants = objectMapper.readValue(rawFileContents, SettlementConstants.class);

		messageDispatcher.addListener(this, MessageType.GET_SETTLEMENT_CONSTANTS);
	}

	public void initialise(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary) {
		settlementConstants.getCurrency().forEach(currencyDefinition -> {
			currencyDefinition.setMaterial(materialDictionary.getByName(currencyDefinition.getMaterialName()));
			if (currencyDefinition.getMaterial() == null) {
				throw new RuntimeException("Currency material not found: " + currencyDefinition.getMaterialName());
			}
			currencyDefinition.setItemType(itemTypeDictionary.getByName(currencyDefinition.getItemTypeName()));
			if (currencyDefinition.getItemType() == null) {
				throw new RuntimeException("Currency item type not found: " + currencyDefinition.getItemTypeName());
			}
		});
	}

	public void initialise(RaceDictionary raceDictionary) {
		for (String fishRaceName : this.settlementConstants.getFishAvailable()) {
			Race fishRace = raceDictionary.getByName(fishRaceName);
			if (fishRace == null) {
				Logger.error("Can not find fish race by name " + fishRaceName);
			} else {
				this.settlementConstants.getFishRacesAvailable().add(fishRace);
			}
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GET_SETTLEMENT_CONSTANTS -> {
				Consumer<SettlementConstants> callback = (Consumer<SettlementConstants>) msg.extraInfo;
				callback.accept(settlementConstants);
				return true;
			}
			default ->
					throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName() + ", " + msg);
		}
	}

	public WorldConstants getWorldConstants() {
		return this.worldConstants;
	}

	public UiConstants getUiConstants() {
		return uiConstants;
	}

	public SettlementConstants getSettlementConstants() {
		return settlementConstants;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		gameContext.setConstantsRepo(this);
	}

	@Override
	public void clearContextRelatedState() {

	}
}
