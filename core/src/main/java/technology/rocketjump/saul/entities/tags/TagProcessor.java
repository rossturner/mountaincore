package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Singleton
public class TagProcessor implements GameContextAware {

	private final TagDictionary tagDictionary;
	private final TagProcessingUtils tagProcessingUtils;
	private final MessageDispatcher messageDispatcher;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final RoomTypeDictionary roomTypeDictionary;
	private final OngoingEffectTypeDictionary ongoingEffectTypeDictionary;
	private final RaceDictionary raceDictionary;
	private GameContext gameContext;

	@Inject
	public TagProcessor(TagDictionary tagDictionary, TagProcessingUtils tagProcessingUtils,
						MessageDispatcher messageDispatcher, FurnitureTypeDictionary furnitureTypeDictionary,
						RoomTypeDictionary roomTypeDictionary, OngoingEffectTypeDictionary ongoingEffectTypeDictionary,
						RaceDictionary raceDictionary) {
		this.tagDictionary = tagDictionary;
		this.tagProcessingUtils = tagProcessingUtils;
		this.messageDispatcher = messageDispatcher;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.roomTypeDictionary = roomTypeDictionary;
		this.ongoingEffectTypeDictionary = ongoingEffectTypeDictionary;
		this.raceDictionary = raceDictionary;
	}

	/**
	 * This initialises processedTags on different types that require them
	 */
	public void init() {
		for (ItemType itemType : tagProcessingUtils.getItemTypeDictionary().getAll()) {
			itemType.setProcessedTags(processRawTags(itemType.getTags()));
		}
		tagProcessingUtils.getItemTypeDictionary().tagsProcessed();
		for (PlantSpecies plantSpecies : tagProcessingUtils.getPlantSpeciesDictionary().getAll()) {
			plantSpecies.setProcessedTags(processRawTags(plantSpecies.getTags()));
		}
		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			furnitureType.setProcessedTags(processRawTags(furnitureType.getTags()));
		}
		for (RoomType roomType : roomTypeDictionary.getAll()) {
			roomType.setProcessedTags(processRawTags(roomType.getTags()));
		}
		for (OngoingEffectType ongoingEffectType : ongoingEffectTypeDictionary.getAll()) {
			ongoingEffectType.setProcessedTags(processRawTags(ongoingEffectType.getTags()));
		}
		for (Race race : raceDictionary.getAll()) {
			race.setProcessedTags(processRawTags(race.getTags()));
		}
	}

	public List<Tag> processRawTags(Map<String, List<String>> rawTags) {
		List<Tag> processedTags = new LinkedList<>();
		if (rawTags != null && !rawTags.isEmpty()) {
			for (Map.Entry<String, List<String>> entry : rawTags.entrySet()) {
				try {
					Tag tag = tagDictionary.newInstanceByName(entry.getKey());
					if (tag != null) {
						tag.setArgs(entry.getValue());
						if (tag.isValid(tagProcessingUtils)) {
							processedTags.add(tag);
						} else {
							Logger.error("Invalid arguments for tag: " + entry.getKey());
						}
					}
				} catch (RuntimeException e) {
					Logger.error("Unrecognised tag: " + entry.getKey());
				}
			}
		}
		return processedTags;

	}

	public void apply(Collection<Tag> tags, Entity entity) {
		for (Tag tag : tags) {
			tag.apply(entity, tagProcessingUtils, messageDispatcher, gameContext);
		}
	}

	public void apply(Tag tag, Entity entity) {
		tag.apply(entity, tagProcessingUtils, messageDispatcher, gameContext);
	}

	public void apply(Collection<Tag> tags, Room room) {
		for (Tag tag : tags) {
			tag.apply(room, tagProcessingUtils);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
