package technology.rocketjump.saul.crafting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.crafting.model.CraftingOutputQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class CraftingOutputQualityDictionary {

	private final List<CraftingOutputQuality> outputQualities;

	private static final CraftingOutputQuality DEFAULT_OUTPUT = new CraftingOutputQuality();
	static {
		DEFAULT_OUTPUT.setOutputQuality(Map.of(ItemQuality.STANDARD, 1f));
	}

	@Inject
	public CraftingOutputQualityDictionary() throws IOException {
		FileHandle craftingOutputJsonFile = Gdx.files.internal("assets/definitions/crafting/craftingOutputQuality.json");
		ObjectMapper objectMapper = new ObjectMapper();
		this.outputQualities = objectMapper.readValue(craftingOutputJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, CraftingOutputQuality.class));
	}

	public CraftingOutputQuality getForSkillLevel(int skillLevel) {
		for (CraftingOutputQuality outputQuality : outputQualities) {
			if (outputQuality.getMinSkillLevel() <= skillLevel && skillLevel <= outputQuality.getMaxSkillLevel()) {
				return outputQuality;
			}
		}
		Logger.warn("Could not find a " + CraftingOutputQuality.class.getSimpleName() + " for skill level " + skillLevel);
		return DEFAULT_OUTPUT;
	}
}
