package technology.rocketjump.saul.constants;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;

import java.io.File;
import java.io.IOException;

@Singleton
public class ConstantsRepo implements GameContextAware {

	private final WorldConstants worldConstants;
	private final UiConstants uiConstants;
	private final SettlementConstants settlementConstants;
	private JSONObject rawJson;

	private Color backgroundColor;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	public ConstantsRepo() throws IOException {
		File jsonFile = new File("assets/definitions/constants.json");
		String rawFileContents = FileUtils.readFileToString(jsonFile);
		this.rawJson = JSON.parseObject(rawFileContents);

		this.worldConstants = objectMapper.readValue(rawFileContents, WorldConstants.class);
		this.uiConstants = objectMapper.readValue(rawFileContents, UiConstants.class);
		this.settlementConstants = objectMapper.readValue(rawFileContents, SettlementConstants.class);
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
