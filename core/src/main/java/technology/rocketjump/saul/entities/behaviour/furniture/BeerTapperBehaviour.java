package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Lists;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.LiquidSplashMessage;
import technology.rocketjump.saul.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.entities.ai.goap.actions.nourishment.LocateDrinkAction.LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION;
import static technology.rocketjump.saul.entities.behaviour.furniture.BeerTapperBehaviour.BeerTapperState.*;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.saul.jobs.model.JobState.REMOVED;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class BeerTapperBehaviour extends FurnitureBehaviour implements Destructible, SelectableDescription, Prioritisable {

	private BeerTapperState state = BeerTapperState.IDLE;
	private List<Job> haulingJobs = new ArrayList<>(1);

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		for (Job outstandingJob : haulingJobs) {
			if (outstandingJob != null && !outstandingJob.getJobState().equals(REMOVED)) {
				messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, outstandingJob);
			}
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job job : haulingJobs) {
			job.setJobPriority(priority);
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		I18nWord descriptionWord = i18nTranslator.getDictionary().getWord(state.descriptionI18nKey);
		return Lists.newArrayList(i18nTranslator.applyReplacements(descriptionWord, Map.of(), Gender.ANY));
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		haulingJobs.removeIf(job -> job.getJobState().equals(REMOVED));

		if (parentEntity.isOnFire()) {
			haulingJobs.forEach(job -> messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job));
			return;
		}

		InventoryComponent parentInventory = parentEntity.getOrCreateComponent(InventoryComponent.class);

		switch (state) {
			case IDLE: {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
								parentEntity, parentEntity.getLocationComponent().getWorldPosition(), relatedItemTypes.get(0), null, true, 1,
								relatedMaterials.get(0), haulingAllocation -> {
							if (haulingAllocation != null) {
								finaliseAllocation(haulingAllocation);
							}
						})
				);
				break;
			}
			case BEER_BARREL_INCOMING: {
				InventoryComponent.InventoryEntry barrelInInventory = parentInventory.findByItemType(relatedItemTypes.get(0), gameContext.getGameClock());
				if (barrelInInventory != null) {
					tapBarrel(barrelInInventory, gameContext);
				} else if (haulingJobs.isEmpty()) {
					state = BeerTapperState.IDLE;
					this.infrequentUpdate(gameContext);
				}
				break;
			}
			case BEER_AVAILABLE: {
				LiquidContainerComponent parentLiquidContainer = parentEntity.getComponent(LiquidContainerComponent.class);
				// Keeping inventory item liquid amount roughly in sync
				for (InventoryComponent.InventoryEntry inventoryEntry : parentInventory.getInventoryEntries()) {
					Entity itemInInventory = inventoryEntry.entity;
					LiquidContainerComponent liquidContainerComponent = itemInInventory.getComponent(LiquidContainerComponent.class);
					if (liquidContainerComponent != null) {
						liquidContainerComponent.setLiquidQuantity(parentLiquidContainer.getLiquidQuantity());
						if (parentLiquidContainer.getLiquidQuantity() < LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION) {
							parentLiquidContainer.setLiquidQuantity(0);
							itemInInventory.getComponent(LiquidContainerComponent.class).setLiquidQuantity(0);

							state = BEER_EXHAUSTED;
							infrequentUpdate(gameContext);
						}
					}
				}
				break;
			}
			case BEER_EXHAUSTED: {
				if (parentInventory.isEmpty()) {
					state = IDLE;
					infrequentUpdate(gameContext);
				} else if (haulingJobs.isEmpty()) {
					Entity itemInInventory = parentInventory.getInventoryEntries().iterator().next().entity;
					itemInInventory.getComponent(ItemAllocationComponent.class).cancelAll(HELD_IN_INVENTORY);
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(
							itemInInventory, parentEntity, true, priority, job -> {
						if (job != null) {
							haulingJobs.add(job);
						}
					}
					));
				}
				break;
			}
		}

	}

	private void tapBarrel(InventoryComponent.InventoryEntry barrelInventoryEntry, GameContext gameContext) {
		LiquidContainerComponent parentLiquidContainer = parentEntity.getComponent(LiquidContainerComponent.class);
		if (parentLiquidContainer == null) {
			Logger.error(this.getClass().getSimpleName() + " expecting to have a parent " + LiquidContainerComponent.class.getSimpleName());
			return;
		}

		LiquidContainerComponent barrelLiquidContainer = barrelInventoryEntry.entity.getComponent(LiquidContainerComponent.class);
		if (barrelLiquidContainer == null) {
			Logger.error(this.getClass().getSimpleName() + " expecting to have a " + LiquidContainerComponent.class.getSimpleName() + " attached to item in inventory");
			return;
		}

		parentLiquidContainer.setTargetLiquidMaterial(barrelLiquidContainer.getTargetLiquidMaterial());
		parentLiquidContainer.setLiquidQuantity(barrelLiquidContainer.getLiquidQuantity());
		messageDispatcher.dispatchMessage(MessageType.LIQUID_SPLASH, new LiquidSplashMessage(parentEntity, barrelLiquidContainer.getTargetLiquidMaterial()));

		state = BeerTapperState.BEER_AVAILABLE;
		infrequentUpdate(gameContext);
	}

	public void finaliseAllocation(HaulingAllocation allocation) {
		// Create hauling job to haul allocation into inventory

		allocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
		allocation.setTargetId(parentEntity.getId());
		allocation.setTargetPosition(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));

		Job haulingJob = new Job(relatedJobTypes.get(0));
		haulingJob.setJobPriority(priority);
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());

		haulingJobs.add(haulingJob);
		state = BEER_BARREL_INCOMING;
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("state", state.name());

		if (!haulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : haulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.state = EnumParser.getEnumValue(asJson, "state", BeerTapperBehaviour.BeerTapperState.class, IDLE);

		JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
		if (incomingHaulingJobsJson != null) {
			for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
				long jobId = incomingHaulingJobsJson.getLongValue(cursor);
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				} else {
					haulingJobs.add(job);
				}
			}
		}
	}

	public enum BeerTapperState {

		IDLE("FURNITURE.BEER_TAPPER.STATE.IDLE"),
		BEER_BARREL_INCOMING("FURNITURE.BEER_TAPPER.STATE.BEER_BARREL_INCOMING"),
		BEER_AVAILABLE("FURNITURE.BEER_TAPPER.STATE.BEER_AVAILABLE"),
		BEER_EXHAUSTED("FURNITURE.BEER_TAPPER.STATE.BEER_EXHAUSTED");

		public final String descriptionI18nKey;

		BeerTapperState(String descriptionI18nKey) {
			this.descriptionI18nKey = descriptionI18nKey;
		}
	}
}
