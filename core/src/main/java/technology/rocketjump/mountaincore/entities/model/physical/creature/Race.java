package technology.rocketjump.mountaincore.entities.model.physical.creature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.BodyStructure;
import technology.rocketjump.mountaincore.entities.model.physical.creature.features.RaceFeatures;
import technology.rocketjump.mountaincore.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Race {

	@Name
	private String name;
	private String i18nKey;
	private String nameGeneration;

	private float minStrength;
	private float maxStrength;

	private String bodyStructureName;
	@JsonIgnore
	private BodyStructure bodyStructure;

	private List<CreatureBodyShapeDescriptor> bodyShapes = new ArrayList<>(); //TODO: this safe?

	private Map<ColoringLayer, SpeciesColor> colors = new EnumMap<>(ColoringLayer.class);

	private CreatureMapPlacement mapPlacement = CreatureMapPlacement.NONE;

	private RaceBehaviour behaviour = new RaceBehaviour();

	private Map<Gender, RaceGenderDescriptor> genders = new EnumMap<>(Gender.class);

	private RaceFeatures features = new RaceFeatures();

	private Map<String, List<String>> tags = new HashMap<>();

	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public float getMinStrength() {
		return minStrength;
	}

	public void setMinStrength(float minStrength) {
		this.minStrength = minStrength;
	}

	public float getMaxStrength() {
		return maxStrength;
	}

	public void setMaxStrength(float maxStrength) {
		this.maxStrength = maxStrength;
	}

	public RaceFeatures getFeatures() {
		return features;
	}

	public void setFeatures(RaceFeatures features) {
		this.features = features;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Race race = (Race) o;
		return name.equals(race.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return name;
	}

	public List<CreatureBodyShapeDescriptor> getBodyShapes() {
		return bodyShapes;
	}

	public void setBodyShapes(List<CreatureBodyShapeDescriptor> bodyShapes) {
		this.bodyShapes = bodyShapes;
	}

	public String getBodyStructureName() {
		return bodyStructureName;
	}

	public void setBodyStructureName(String bodyStructureName) {
		this.bodyStructureName = bodyStructureName;
	}

	public BodyStructure getBodyStructure() {
		return bodyStructure;
	}

	public void setBodyStructure(BodyStructure bodyStructure) {
		this.bodyStructure = bodyStructure;
		if (bodyStructure != null) {
			setBodyStructureName(bodyStructure.getName());
		}
	}

	public Map<ColoringLayer, SpeciesColor> getColors() {
		return colors;
	}

	public void setColors(Map<ColoringLayer, SpeciesColor> colors) {
		this.colors = colors;
	}

	public Map<Gender, RaceGenderDescriptor> getGenders() {
		return genders;
	}

	public void setGenders(Map<Gender, RaceGenderDescriptor> genders) {
		this.genders = genders;
	}

	public RaceBehaviour getBehaviour() {
		return behaviour;
	}

	public void setBehaviour(RaceBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public CreatureMapPlacement getMapPlacement() {
		return mapPlacement;
	}

	public void setMapPlacement(CreatureMapPlacement mapPlacement) {
		this.mapPlacement = mapPlacement;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	public String getNameGeneration() {
		return nameGeneration;
	}

	public void setNameGeneration(String nameGeneration) {
		this.nameGeneration = nameGeneration;
	}

}
