package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.tags.ItemUsageSoundTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.roof.TileRoofState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;
import technology.rocketjump.saul.particles.custom_libgdx.ProgressBarEffect;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.HappinessModifier.WORKED_IN_ENCLOSED_ROOM;
import static technology.rocketjump.saul.entities.model.EntityType.FURNITURE;
import static technology.rocketjump.saul.environment.model.WeatherType.HappinessInteraction.WORKING;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class WorkOnJobAction extends Action {

	private boolean activeSoundTriggered;
	private boolean furnitureInUseNotified;

	public WorkOnJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		Optional<Entity> targetFurniture = getTargetFurniture(parent.getAssignedJob(), gameContext);

		if (completionType == null && inPositionToWorkOnJob()) {
			Job assignedJob = parent.getAssignedJob();
			SkillsComponent skillsComponent = parent.parentEntity.getComponent(SkillsComponent.class);

			float workDone = deltaTime;
			EquippedItemComponent equippedItemComponent = parent.parentEntity.getComponent(EquippedItemComponent.class);
			if (equippedItemComponent != null && equippedItemComponent.getMainHandItem() != null) {
				Entity equippedItem = equippedItemComponent.getMainHandItem();
				if (equippedItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes) {
					workDone *= (1f / itemAttributes.getItemQuality().jobDurationMultiplier);
				}
			}
			assignedJob.applyWorkDone(workDone);

			if (!activeSoundTriggered) {
				SoundAsset jobSoundAsset = getJobSoundAsset();
				if (jobSoundAsset != null) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(jobSoundAsset,
							parent.parentEntity.getId(), parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), null));
				}
				activeSoundTriggered = true;
			}

			if (!furnitureInUseNotified) {
				if (targetFurniture.isPresent()) {
					furnitureInUseNotified = true;
					parent.messageDispatcher.dispatchMessage(MessageType.FURNITURE_IN_USE, targetFurniture.get());
				}
			}

			MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
			HappinessComponent happinessComponent = parent.parentEntity.getComponent(HappinessComponent.class);
			if (currentTile != null && currentTile.hasRoom() && currentTile.getRoomTile().getRoom().isFullyEnclosed() && happinessComponent != null) {
				happinessComponent.add(WORKED_IN_ENCLOSED_ROOM);
			}
			if (currentTile != null && currentTile.getRoof().getState().equals(TileRoofState.OPEN) &&
					gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(WORKING) && happinessComponent != null) {
				happinessComponent.add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(WORKING));
			}

			Action This = this;

			updateProgressBarEffect();

			List<ParticleEffectType> relatedParticleEffectTypes = getRelatedParticleEffectTypes();
			if (relatedParticleEffectTypes != null) {
				for (ParticleEffectType particleEffectType : relatedParticleEffectTypes) {
					if (spawnedParticles.stream()
							.noneMatch(p -> p.getType().equals(particleEffectType))) {
						parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
								particleEffectType,
								Optional.of(parent.parentEntity),
								Optional.ofNullable(assignedJob.getTargetOfJob(gameContext)
								), instance -> {
							This.spawnedParticles.add(instance);
						}));
					}
				}
			}

			if (assignedJob.getTotalWorkToDo(skillsComponent) <= assignedJob.getWorkDoneSoFar()) {
				parent.messageDispatcher.dispatchMessage(MessageType.JOB_COMPLETED, new JobCompletedMessage(assignedJob, skillsComponent, parent.parentEntity));
				completionType = SUCCESS;
			}

		} else {
			completionType = FAILURE;
		}

		if (completionType != null) {
			// finished

			if (furnitureInUseNotified && targetFurniture.isPresent()) {
				parent.messageDispatcher.dispatchMessage(MessageType.FURNITURE_NO_LONGER_IN_USE, targetFurniture.get());
			}

			if (activeSoundTriggered) {
				SoundAsset jobSoundAsset = getJobSoundAsset();
				if (jobSoundAsset != null && jobSoundAsset.isLooping()) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_STOP_SOUND_LOOP, new RequestSoundStopMessage(jobSoundAsset, parent.parentEntity.getId()));
				}
			}
		}
	}

	public void updateProgressBarEffect() {
		Job assignedJob = parent.getAssignedJob();
		SkillsComponent skillsComponent = parent.parentEntity.getComponent(SkillsComponent.class);
		Action This = this;
		parent.messageDispatcher.dispatchMessage(MessageType.GET_PROGRESS_BAR_EFFECT_TYPE, (ParticleEffectTypeCallback) progressBarType -> {
			if (spawnedParticles.stream().noneMatch(p -> p.getType().equals(progressBarType))) {
				parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
						progressBarType,
						Optional.of(parent.parentEntity),
						Optional.empty(),
						instance -> {
					This.spawnedParticles.add(instance);
				}));
			}
		});

		float workCompletionFraction = Math.min(assignedJob.getWorkDoneSoFar() / assignedJob.getTotalWorkToDo(skillsComponent), 1f);

		spawnedParticles.removeIf(p -> p == null || !p.isActive());

		for (ParticleEffectInstance spawnedParticle : spawnedParticles) {
			if (spawnedParticle.getWrappedInstance() instanceof ProgressBarEffect) {
				((ProgressBarEffect) spawnedParticle.getWrappedInstance()).setProgress(workCompletionFraction);
			}
		}
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		completionType = CompletionType.FAILURE;
		update(0f, gameContext); // to clear sound and furniture in use
	}

	private Optional<Entity> getTargetFurniture(Job assignedJob, GameContext gameContext) {
		if (assignedJob.getTargetId() != null) {
			Entity targetEntity = gameContext.getEntities().get(assignedJob.getTargetId());
			if (targetEntity != null && targetEntity.getType().equals(FURNITURE)) {
				return Optional.of(targetEntity);
			}
		}
		return Optional.empty();
	}

	private List<ParticleEffectType> getRelatedParticleEffectTypes() {
		if (parent.getAssignedJob().getCraftingRecipe() != null) {
			return parent.getAssignedJob().getCraftingRecipe().getCraftingType().getParticleEffectTypes();
		} else {
			return parent.getAssignedJob().getType().getWorkOnJobParticleEffectTypes();
		}
	}

	public SoundAsset getJobSoundAsset() {
		Job assignedJob = parent.getAssignedJob();
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getComponent(EquippedItemComponent.class);
		ItemUsageSoundTag itemUsageSoundTag = null;
		if (equippedItemComponent != null && equippedItemComponent.getMainHandItem() != null) {
			itemUsageSoundTag = equippedItemComponent.getMainHandItem().getTag(ItemUsageSoundTag.class);
		}

		if (itemUsageSoundTag != null && itemUsageSoundTag.getSoundAssetName() != null) {
			AtomicReference<SoundAsset> assetHolder = new AtomicReference<>();
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND_ASSET, new RequestSoundAssetMessage(itemUsageSoundTag.getSoundAssetName(), assetHolder::set));
			return assetHolder.get();
		} else if (assignedJob.getCookingRecipe() != null && assignedJob.getCookingRecipe().getActiveSoundAsset() != null) {
			return assignedJob.getCookingRecipe().getActiveSoundAsset();
		} else if (assignedJob.getType().getActiveSoundAsset() != null) {
			return assignedJob.getType().getActiveSoundAsset();
		} else {
			return null;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (activeSoundTriggered) {
			asJson.put("soundTriggered", true);
		}
		if (furnitureInUseNotified) {
			asJson.put("furnitureInUseNotified", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.activeSoundTriggered = asJson.getBooleanValue("soundTriggered");
		this.furnitureInUseNotified = asJson.getBooleanValue("furnitureInUseNotified");
	}

	private boolean inPositionToWorkOnJob() {
		if (parent.getAssignedJob() == null) {
			return false;
		} else if (parent.getAssignedJob().getType().isAccessedFromAdjacentTile()) {
			// Tile distance must be one
			return toGridPoint(parent.parentEntity.getLocationComponent().getWorldPosition()).dst2(parent.getAssignedJob().getJobLocation()) <= 1f;
		} else {
			return toGridPoint(parent.parentEntity.getLocationComponent().getWorldPosition()).equals(parent.getAssignedJob().getJobLocation());
		}
	}
}
