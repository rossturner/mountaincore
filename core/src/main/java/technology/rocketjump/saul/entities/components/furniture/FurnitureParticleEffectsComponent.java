package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobTarget;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FurnitureParticleEffectsComponent implements ParentDependentEntityComponent {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;

	private final List<ParticleEffectType> permanentParticleEffects = new ArrayList<>();
	private final List<ParticleEffectType> particleEffectsWhenInUse = new ArrayList<>();
	private final List<ParticleEffectType> particleEffectsWhenProcessing = new ArrayList<>();

	private final List<ParticleEffectInstance> currentParticleInstances = new ArrayList<>();

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FurnitureParticleEffectsComponent clone = new FurnitureParticleEffectsComponent();
		clone.permanentParticleEffects.addAll(this.permanentParticleEffects);
		clone.particleEffectsWhenInUse.addAll(this.particleEffectsWhenInUse);
		clone.particleEffectsWhenProcessing.addAll(particleEffectsWhenProcessing);
		return clone;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
	}

	public List<ParticleEffectType> getPermanentParticleEffects() {
		return permanentParticleEffects;
	}

	public List<ParticleEffectType> getParticleEffectsWhenInUse() {
		return particleEffectsWhenInUse;
	}

	public List<ParticleEffectType> getParticleEffectsWhenProcessing() {
		return particleEffectsWhenProcessing;
	}

	public List<ParticleEffectInstance> getCurrentParticleInstances() {
		return currentParticleInstances;
	}

	public void triggerPermanentEffects() {
		this.getCurrentParticleInstances().removeIf(p -> p == null || !p.isActive());
		if (!this.getPermanentParticleEffects().isEmpty() && this.getCurrentParticleInstances().isEmpty()) {
			Optional<Entity> optionalParent = Optional.of(parentEntity);

			for (ParticleEffectType effectType : this.getPermanentParticleEffects()) {
				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(effectType,
						optionalParent,
						parentEntity == null ? Optional.empty() : Optional.of(new JobTarget(parentEntity)),
						this.getCurrentParticleInstances()::add));
			}
		}
	}

	public void triggerProcessingEffects(Optional<JobTarget> effectTarget) {
		this.getCurrentParticleInstances().removeIf(p -> p == null || !p.isActive());
		if (!this.getParticleEffectsWhenProcessing().isEmpty() && this.getCurrentParticleInstances().isEmpty()) {
			Optional<Entity> optionalParent = Optional.of(parentEntity);
			if (parentEntity.getType().equals(EntityType.ONGOING_EFFECT)) {
				optionalParent = Optional.ofNullable(effectTarget.orElse(new JobTarget((Entity)null)).getEntity());
			}

			for (ParticleEffectType effectType : this.getParticleEffectsWhenProcessing()) {
				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(effectType,
						optionalParent,
						effectTarget,
						this.getCurrentParticleInstances()::add));
			}
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!permanentParticleEffects.isEmpty()) {
			JSONArray permanentEffectsJson = new JSONArray();
			for (ParticleEffectType permanentParticleEffect : permanentParticleEffects) {
				permanentEffectsJson.add(permanentParticleEffect.getName());
			}
			asJson.put("permanentParticleEffects", permanentParticleEffects);
		}

		if (!particleEffectsWhenProcessing.isEmpty()) {
			JSONArray particleEffectsJson = new JSONArray();
			for (ParticleEffectType particleEffectType : particleEffectsWhenProcessing) {
				particleEffectsJson.add(particleEffectType.getName());
			}
			asJson.put("particleEffectsWhenProcessing", particleEffectsJson);
		}

		if (!particleEffectsWhenInUse.isEmpty()) {
			JSONArray particleEffectsJson = new JSONArray();
			for (ParticleEffectType particleEffectType : particleEffectsWhenInUse) {
				particleEffectsJson.add(particleEffectType.getName());
			}
			asJson.put("particleEffectsWhenInUse", particleEffectsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray permanentParticleEffectsJson = asJson.getJSONArray("permanentParticleEffects");
		if (permanentParticleEffectsJson != null) {
			for (int cursor = 0; cursor < permanentParticleEffectsJson.size(); cursor++) {
				ParticleEffectType particleEffectType = relatedStores.particleEffectTypeDictionary.getByName(permanentParticleEffectsJson.getString(cursor));
				if (particleEffectType == null) {
					throw new InvalidSaveException("Could not find particleEffectType with name " + permanentParticleEffectsJson.getString(cursor));
				} else {
					permanentParticleEffects.add(particleEffectType);
				}
			}
		}

		JSONArray particleEffectsJson = asJson.getJSONArray("particleEffectsWhenProcessing");
		if (particleEffectsJson != null) {
			for (int cursor = 0; cursor < particleEffectsJson.size(); cursor++) {
				ParticleEffectType particleEffectType = relatedStores.particleEffectTypeDictionary.getByName(particleEffectsJson.getString(cursor));
				if (particleEffectType == null) {
					throw new InvalidSaveException("Could not find particleEffectType with name " + particleEffectsJson.getString(cursor));
				} else {
					particleEffectsWhenProcessing.add(particleEffectType);
				}
			}
		}

		JSONArray inUseParticleEffectsJson = asJson.getJSONArray("particleEffectsWhenInUse");
		if (inUseParticleEffectsJson != null) {
			for (int cursor = 0; cursor < inUseParticleEffectsJson.size(); cursor++) {
				ParticleEffectType particleEffectType = relatedStores.particleEffectTypeDictionary.getByName(inUseParticleEffectsJson.getString(cursor));
				if (particleEffectType == null) {
					throw new InvalidSaveException("Could not find particleEffectType with name " + inUseParticleEffectsJson.getString(cursor));
				} else {
					particleEffectsWhenInUse.add(particleEffectType);
				}
			}
		}
	}

	public void releaseParticles() {
		for (ParticleEffectInstance particleInstance : getCurrentParticleInstances()) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, particleInstance);
		}
	}
}
