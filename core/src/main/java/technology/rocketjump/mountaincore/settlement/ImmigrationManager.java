package technology.rocketjump.mountaincore.settlement;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.Goal;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.factories.SettlerFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.mapping.factories.CreaturePopulator;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.settlement.notifications.Notification;
import technology.rocketjump.mountaincore.settlement.notifications.NotificationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ImmigrationManager implements Updatable, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final SettlementItemTracker settlementItemTracker;
	private final SettlerTracker settlerTracker;
	private final SettlerFactory settlerFactory;
	private final SkillDictionary skillDictionary;
	private final CreaturePopulator creaturePopulator;
	private final int baseImmigrationVarianceIterations;
	private final int baseImmigrationExtraFixedAmount;
	private final boolean extraFoodImmigrationEnabled;
	private final float extraFoodImmigrationSettlersPerYearSupplyOfBonusFood;
	private final boolean immigrationCapsEnabled;
	private final int immigrationCapsMinAmountForCapsToApply;
	private final float immigrationCapMaxImmigrationPerPopulation;
	private GameContext gameContext;

	private final boolean baseImmigrationEnabled;
	private final int baseImmigrationVariance;

	private float timeSinceLastUpdate;

	@Inject
	public ImmigrationManager(MessageDispatcher messageDispatcher, SettlementItemTracker settlementItemTracker, SettlerTracker settlerTracker,
							  SettlerFactory settlerFactory, SkillDictionary skillDictionary, CreaturePopulator creaturePopulator) {
		this.creaturePopulator = creaturePopulator;
		FileHandle settingsJsonFile = new FileHandle("assets/settings/immigrationSettings.json");
		JSONObject immigrationSettings = JSON.parseObject(settingsJsonFile.readString());

		baseImmigrationEnabled = immigrationSettings.getJSONObject("baseImmigration").getBooleanValue("enabled");
		baseImmigrationVariance = immigrationSettings.getJSONObject("baseImmigration").getIntValue("varianceNumber");
		baseImmigrationVarianceIterations = immigrationSettings.getJSONObject("baseImmigration").getIntValue("varianceIterations");
		baseImmigrationExtraFixedAmount = immigrationSettings.getJSONObject("baseImmigration").getIntValue("extraFixedAmount");

		extraFoodImmigrationEnabled = immigrationSettings.getJSONObject("extraFoodImmigration").getBooleanValue("enabled");
		extraFoodImmigrationSettlersPerYearSupplyOfBonusFood = immigrationSettings.getJSONObject("extraFoodImmigration").getFloatValue("settlersPerYearSupplyOfBonusFood");

		immigrationCapsEnabled = immigrationSettings.getJSONObject("immigrationCaps").getBooleanValue("enabled");
		immigrationCapsMinAmountForCapsToApply = immigrationSettings.getJSONObject("immigrationCaps").getIntValue("minAmountForCapsToApply");
		immigrationCapMaxImmigrationPerPopulation = immigrationSettings.getJSONObject("immigrationCaps").getFloatValue("maxImmigrationPerPopulation");

		this.messageDispatcher = messageDispatcher;
		this.settlementItemTracker = settlementItemTracker;
		this.settlerTracker = settlerTracker;
		this.settlerFactory = settlerFactory;
		this.skillDictionary = skillDictionary;

		messageDispatcher.addListener(this, MessageType.YEAR_ELAPSED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.YEAR_ELAPSED: {
				calculateNextImmigration();
				creaturePopulator.addAnimalsAtEdge(gameContext);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > 1.44f) {
			timeSinceLastUpdate = 0f;

			if (gameContext != null && gameContext.getSettlementState().getNextImmigrationGameTime() != null &&
					gameContext.getSettlementState().getNextImmigrationGameTime() < gameContext.getGameClock().getCurrentGameTime() &&
					!gameContext.getSettlementState().isGameOver()) {
				triggerImmigration();
			}

			if (gameContext != null && gameContext.getSettlementState().getImmigrantCounter() > 0 && gameContext.getSettlementState().getImmigrationPoint() != null) {
				createImmigrant(gameContext.getSettlementState().getImmigrationPoint());
				gameContext.getSettlementState().setImmigrantCounter(gameContext.getSettlementState().getImmigrantCounter() - 1);
			}

		}
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	private void calculateNextImmigration() {
		int numImmigrantsDue = calculateNumNewSettlers();
		if (numImmigrantsDue > 0) {
			gameContext.getSettlementState().setImmigrantsDue(numImmigrantsDue);
			gameContext.getSettlementState().setNextImmigrationGameTime(pickNextImmigrationTime());
		}
	}

	private Double pickNextImmigrationTime() {
		// Assuming this has triggered at midnight at the stroke of a new year
		// Want settlers to arrive between 09:00 and 17:00
		double timeOfDay = 9.0 + (gameContext.getRandom().nextDouble() * 8.0);
		double dayNumber = gameContext.getRandom().nextInt(gameContext.getGameClock().DAYS_IN_SEASON) / 3;
		double hoursFromNow = (gameContext.getGameClock().HOURS_IN_DAY * dayNumber) + timeOfDay;

		return gameContext.getGameClock().getCurrentGameTime() + hoursFromNow;
	}

	private int calculateNumNewSettlers() {
		if (!baseImmigrationEnabled) {
			return 0;
		}

		int totalNumImmigrants = baseImmigrationExtraFixedAmount;
		for (int iteration = 0; iteration < baseImmigrationVarianceIterations; iteration++) {
			totalNumImmigrants += gameContext.getRandom().nextInt(baseImmigrationVariance);
		}

		int currentNumSettlers = settlerTracker.count();

		if (extraFoodImmigrationEnabled) {
			int foodAmount = 0;
			for (Entity entity : settlementItemTracker.getUnallocatedEdibleItems()) {
				foodAmount += entity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
			}

			GameClock clock = gameContext.getGameClock();

			// Need enough food to last each settler ~3 seasons -> 2 meals/day * 30 days -> 60 meals
			// 1 edible item should be able to prepare 4 meals (or one if it is already prepared)
			int foodNeededPerSettler = (clock.DAYS_IN_SEASON * 3 * 2) / 4;
			int surplusFood = foodAmount - (currentNumSettlers * foodNeededPerSettler);
			int numSettlersDueToExtraFood = (int)(((float)surplusFood / (float)foodNeededPerSettler) * extraFoodImmigrationSettlersPerYearSupplyOfBonusFood);
			totalNumImmigrants += numSettlersDueToExtraFood;
		}

		if (immigrationCapsEnabled && currentNumSettlers >= immigrationCapsMinAmountForCapsToApply) {
			int maxImmigration = Math.round((float)currentNumSettlers * immigrationCapMaxImmigrationPerPopulation);
			totalNumImmigrants = Math.min(totalNumImmigrants, maxImmigration);
		}

		return totalNumImmigrants;
	}

	public void triggerImmigration() {
		int numImmigrants = gameContext.getSettlementState().getImmigrantsDue();
		gameContext.getSettlementState().setImmigrantsDue(0);
		gameContext.getSettlementState().setNextImmigrationGameTime(null);
		if (gameContext.getSettlementState().isAllowImmigration()) {
			gameContext.getSettlementState().setImmigrantCounter(numImmigrants);
			gameContext.getSettlementState().setImmigrationPoint(pickImmigrationPoint());
			if (gameContext.getSettlementState().getImmigrationPoint() == null) {
				Logger.warn("Could not find valid map edge to spawn immigration from");
			} else {
				Notification notification = new Notification(NotificationType.IMMIGRANTS_ARRIVED, gameContext.getSettlementState().getImmigrationPoint(), null);
				messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, notification);
			}
		}
	}

	private Vector2 pickImmigrationPoint() {
		// Must be map edge in region where settlers currently are

		TiledMap areaMap = gameContext.getAreaMap();
		Map<Integer, Long> regionIdFrequency = gameContext.getEntities().values().stream()
				.filter(Entity::isSettler)
				.map(settler -> settler.getLocationComponent().getWorldPosition())
				.map(areaMap::getTile)
				.filter(Objects::nonNull)
				.map(MapTile::getRegionId)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		int regionId = regionIdFrequency.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey).orElse(gameContext.getAreaMap().getTile(gameContext.getAreaMap().getEmbarkPoint()).getRegionId());

		List<MapTile> potentialImmigrationPoints = new ArrayList<>();
		for (int x = 0; x < areaMap.getWidth(); x++) {
			MapTile bottomEdgeTile = areaMap.getTile(x, 0);
			if (bottomEdgeTile.getRegionId() == regionId && bottomEdgeTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				potentialImmigrationPoints.add(bottomEdgeTile);
			}

			MapTile topEdgeTile = areaMap.getTile(x, areaMap.getHeight() - 1);
			if (topEdgeTile.getRegionId() == regionId && topEdgeTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				potentialImmigrationPoints.add(topEdgeTile);
			}
		}
		for (int y = 1; y < areaMap.getHeight() - 1; y++) {
			MapTile leftEdgeTile = areaMap.getTile(0, y);
			if (leftEdgeTile.getRegionId() == regionId && leftEdgeTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				potentialImmigrationPoints.add(leftEdgeTile);
			}
			MapTile rightEdgeTile = areaMap.getTile(areaMap.getWidth() -1, y);
			if (rightEdgeTile.getRegionId() ==  regionId && rightEdgeTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				potentialImmigrationPoints.add(rightEdgeTile);
			}
		}
		if (potentialImmigrationPoints.isEmpty()) {
			return null;
		}

		MapTile immigrationTile = potentialImmigrationPoints.get(gameContext.getRandom().nextInt(potentialImmigrationPoints.size()));
		GridPoint2 tilePosition = immigrationTile.getTilePosition();
		boolean leftEdge = tilePosition.x == 0;
		boolean rightEdge = tilePosition.x == areaMap.getWidth() - 1;
		boolean topEdge = tilePosition.y == areaMap.getHeight() - 1;
		boolean bottomEdge = tilePosition.y == 0;

		Vector2 immigrationPoint = immigrationTile.getWorldPositionOfCenter();
		if (leftEdge) {
			immigrationPoint.add(-0.49f, 0f);
		} else if (rightEdge) {
			immigrationPoint.add(0.49f, 0f);
		}
		if (topEdge) {
			immigrationPoint.add(0f, 0.49f);
		} else if (bottomEdge) {
			immigrationPoint.add(0f, -0.49f);
		}
		return immigrationPoint;
	}

	private void createImmigrant(Vector2 spawnPosition) {
		List<Skill> allProfessions = new ArrayList<>(skillDictionary.getSelectableProfessions());
		Skill primaryProfession = allProfessions.get(gameContext.getRandom().nextInt(allProfessions.size()));
		Skill secondaryProfession = null;
		if (!primaryProfession.getName().equals("VILLAGER")) {
			secondaryProfession = allProfessions.get(gameContext.getRandom().nextInt(allProfessions.size()));
		}
		Entity settler = settlerFactory.create(spawnPosition, primaryProfession, secondaryProfession, gameContext, true);
		CreatureBehaviour settlerBehaviour = (CreatureBehaviour) settler.getBehaviourComponent();

		Goal idleGoal = SpecialGoal.IDLE.getInstance();
		AssignedGoal assignedGoal = new AssignedGoal(idleGoal, settler, messageDispatcher, gameContext);
		assignedGoal.actionQueue.pop();
		GoToLocationAction goToLocationAction = new GoToLocationAction(assignedGoal);
		goToLocationAction.setOverrideLocation(VectorUtils.toVector(gameContext.getAreaMap().getEmbarkPoint()));
		assignedGoal.actionQueue.push(goToLocationAction);
		settlerBehaviour.setCurrentGoal(assignedGoal);
	}

}
