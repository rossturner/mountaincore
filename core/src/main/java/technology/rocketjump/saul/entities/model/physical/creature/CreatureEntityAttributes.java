package technology.rocketjump.saul.entities.model.physical.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.google.common.base.MoreObjects;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.body.Body;
import technology.rocketjump.saul.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.AWAKE;
import static technology.rocketjump.saul.entities.model.physical.creature.Sanity.SANE;

public class CreatureEntityAttributes implements EntityAttributes {

	private long seed;
	private Race race;
	private Gender gender;
	private float strength; // might want to move this to a Stats object

	private Body body; // instance of a bodyStructure with damage/missing parts, for humanoid and animal type entities
	private CreatureBodyShape bodyShape;
	private Map<ColoringLayer, Color> colors = new EnumMap<>(ColoringLayer.class);
	private boolean hasHair;
	private HumanoidName name;
	private Consciousness consciousness = AWAKE;
	private Sanity sanity = SANE;

	public CreatureEntityAttributes() {

	}

	public CreatureEntityAttributes(Race race, long seed) {
		this.seed = seed;
		this.race = race;
		this.body = new Body(race.getBodyStructure());
		Random random = new RandomXS128(seed);
		float strengthRange = race.getMaxStrength() - race.getMinStrength();
		strength = race.getMinStrength();
		// essentially 3d6 distribution with d6 replaced by 1/3 of strength range
		for (int i = 0; i < 3; i++) {
			strength += random.nextFloat() * (strengthRange / 3);
		}

		for (CreatureBodyShapeDescriptor bodyTypeDescriptor : race.getBodyShapes()) {
			if (bodyTypeDescriptor.getMaxStrength() != null) {
				if (strength > bodyTypeDescriptor.getMaxStrength()) {
					continue;
				}
			}
			if (bodyTypeDescriptor.getMinStrength() != null) {
				if (strength < bodyTypeDescriptor.getMinStrength()) {
					continue;
				}
			}

			this.bodyShape = bodyTypeDescriptor.getValue();
			break;
		}

		for (Map.Entry<ColoringLayer, SpeciesColor> colorEntry : race.getColors().entrySet()) {
			colors.put(colorEntry.getKey(), colorEntry.getValue().getColor(seed));
		}
		selectGender(race, random);
		this.hasHair = hasHair(race);
	}

	@Override
	public CreatureEntityAttributes clone() {
		CreatureEntityAttributes cloned = new CreatureEntityAttributes();
		cloned.seed = this.seed;
		cloned.race = this.race;
		cloned.gender = this.gender;
		cloned.bodyShape = this.bodyShape;
		cloned.colors.putAll(this.colors);
		cloned.hasHair = this.hasHair;
		cloned.consciousness= this.consciousness;
		cloned.sanity = this.sanity;
		return cloned;
	}

	@Override
	public Map<GameMaterialType, GameMaterial> getMaterials() {
		if (race.getFeatures().getSkin() != null && race.getFeatures().getSkin().getSkinMaterial() != null) {
			return Map.of(race.getFeatures().getSkin().getSkinMaterial().getMaterialType(), race.getFeatures().getSkin().getSkinMaterial());
		} else {
			return Map.of();
		}
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public CreatureBodyShape getBodyShape() {
		return bodyShape;
	}

	public void setBodyShape(CreatureBodyShape bodyShape) {
		this.bodyShape = bodyShape;
	}

	public void setSkinColor(Color skinColor) {
		this.colors.put(ColoringLayer.SKIN_COLOR, skinColor);
	}

	public void setHairColor(Color hairColor) {
		this.colors.put(ColoringLayer.HAIR_COLOR, hairColor);
	}

	public void setBoneColor(Color boneColor) {
		this.colors.put(ColoringLayer.SKIN_COLOR, boneColor);
	}

	public void setAccessoryColor(Color accessoryColor) {
		this.colors.put(ColoringLayer.ACCESSORY_COLOR, accessoryColor);
	}

	public long getSeed() {
		return seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		return colors.get(coloringLayer);
	}

	public void setGender(Gender gender) {
		this.gender = gender;
		this.hasHair = hasHair(race);
	}

	public Gender getGender() {
		return gender;
	}

	public boolean getHasHair() {
		return hasHair;
	}

	public void setName(HumanoidName name) {
		this.name = name;
	}

	public HumanoidName getName() {
		return name;
	}

	public Consciousness getConsciousness() {
		return consciousness;
	}

	public void setConsciousness(Consciousness consciousness) {
		this.consciousness = consciousness;
	}

	public Sanity getSanity() {
		return sanity;
	}

	public void setSanity(Sanity sanity) {
		this.sanity = sanity;
	}

	public float getStrength() {
		return strength;
	}

	public void setStrength(float strength) {
		this.strength = strength;
	}

	public Body getBody() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("race", race)
				.add("gender", gender)
				.add("bodyShape", bodyShape)
				.add("consciousness", consciousness)
				.toString();
	}

	private void selectGender(Race race, Random random) {
		float genderTotal = 0;
		for (RaceGenderDescriptor genderDescriptor : race.getGenders().values()) {
			genderTotal += genderDescriptor.getWeighting();
		}
		float genderRoll = random.nextFloat() * genderTotal;
		this.gender = Gender.NONE;
		for (Map.Entry<Gender, RaceGenderDescriptor> genderDescriptorEntry : race.getGenders().entrySet()) {
			genderRoll -= genderDescriptorEntry.getValue().getWeighting();
			if (genderRoll <= 0) {
				this.gender = genderDescriptorEntry.getKey();
				break;
			}
		}
	}

	private boolean hasHair(Race race) {
		RaceGenderDescriptor genderDescriptor = race.getGenders().get(gender);
		if (genderDescriptor == null) {
			return false;
		} else {
			return new RandomXS128(seed).nextFloat() < genderDescriptor.getHasHair();
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		asJson.put("race", race.getName());
		asJson.put("gender", gender.name());
		asJson.put("strength", strength);


		JSONObject bodyJson = new JSONObject(true);
		body.writeTo(bodyJson, savedGameStateHolder);
		asJson.put("body", bodyJson);
		if (!bodyShape.equals(CreatureBodyShape.AVERAGE)) {
			asJson.put("bodyShape", bodyShape.name());
		}

		JSONObject colorsJson = new JSONObject(true);
		for (Map.Entry<ColoringLayer, Color> entry : colors.entrySet()) {
			colorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
		}
		asJson.put("colors", colorsJson);

		if (hasHair) {
			asJson.put("hasHair", true);
		}
		if (name != null) {
			JSONObject nameJson = new JSONObject(true);
			name.writeTo(nameJson, savedGameStateHolder);
			asJson.put("name", nameJson);
		}
		if (!consciousness.equals(AWAKE)) {
			asJson.put("consciousness", consciousness.name());
		}
		if (!sanity.equals(SANE)) {
			asJson.put("sanity", sanity.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		race = relatedStores.raceDictionary.getByName(asJson.getString("race"));
		if (race == null) {
			throw new InvalidSaveException("Could not find race with name " + asJson.getString("race"));
		}
		gender = EnumParser.getEnumValue(asJson, "gender", Gender.class, Gender.FEMALE);
		strength = asJson.getFloatValue("strength");

		JSONObject bodyJson = asJson.getJSONObject("body");
		body = new Body();
		body.readFrom(bodyJson, savedGameStateHolder, relatedStores);

		bodyShape = EnumParser.getEnumValue(asJson, "bodyShape", CreatureBodyShape.class, CreatureBodyShape.AVERAGE);

		JSONObject colorsJson = asJson.getJSONObject("colors");
		if (colorsJson != null) {
			for (String colorLayerName : colorsJson.keySet()) {
				this.colors.put(ColoringLayer.valueOf(colorLayerName), HexColors.get(colorsJson.getString(colorLayerName)));
			}
		}

		hasHair = asJson.getBooleanValue("hasHair");
		JSONObject nameJson = asJson.getJSONObject("name");
		if (nameJson != null) {
			name = new HumanoidName();
			name.readFrom(nameJson, savedGameStateHolder, relatedStores);
		}
		consciousness = EnumParser.getEnumValue(asJson, "consciousness", Consciousness.class, AWAKE);
		sanity = EnumParser.getEnumValue(asJson, "sanity", Sanity.class, SANE);
	}

	public Map<ColoringLayer, Color> getColors() {
		return colors;
	}
}
