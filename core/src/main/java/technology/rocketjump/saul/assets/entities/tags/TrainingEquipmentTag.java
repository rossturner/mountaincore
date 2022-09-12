package technology.rocketjump.saul.assets.entities.tags;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class TrainingEquipmentTag extends Tag {

	@Override
	public String getTagName() {
		return "TRAINING_EQUIPMENT";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return EnumUtils.isValidEnum(TrainingEquipmentType.class, args.get(0));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		TrainingEquipmentComponent trainingEquipmentComponent = entity.getOrCreateComponent(TrainingEquipmentComponent.class);
		trainingEquipmentComponent.setEquipmentType(TrainingEquipmentType.valueOf(args.get(0)));
	}

	public enum TrainingEquipmentType {
		MELEE,
		RANGED
	}

	public static class TrainingEquipmentComponent implements EntityComponent {

		private TrainingEquipmentType equipmentType;

		public TrainingEquipmentType getEquipmentType() {
			return equipmentType;
		}

		public void setEquipmentType(TrainingEquipmentType equipmentType) {
			this.equipmentType = equipmentType;
		}

		@Override
		public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
			TrainingEquipmentComponent cloned = new TrainingEquipmentComponent();
			cloned.equipmentType = this.equipmentType;
			return cloned;
		}

		@Override
		public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
			EntityComponent.super.writeTo(asJson, savedGameStateHolder);
		}

		@Override
		public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
			EntityComponent.super.readFrom(asJson, savedGameStateHolder, relatedStores);
		}
	}

}
