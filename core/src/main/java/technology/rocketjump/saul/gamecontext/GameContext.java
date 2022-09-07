package technology.rocketjump.saul.gamecontext;

import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.mapping.model.MapEnvironment;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.types.JobRequestMessage;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.settlement.SettlementState;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Simple bean to hold all state related to a game instance
 */
public class GameContext {

	private final Map<Long, Job> jobs = new ConcurrentHashMap<>();
	private final Map<Long, Entity> entities = new ConcurrentHashMap<>();
	private final Map<Long, Construction> constructions = new ConcurrentHashMap<>();
	private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
	private final Map<Long, Squad> squads = new ConcurrentHashMap<>();
	private final Deque<JobRequestMessage> jobRequestQueue = new LinkedBlockingDeque<>();
	private final Map<String, GameMaterial> dynamicallyCreatedMaterialsByCombinedId = new HashMap<>();
	private SettlementState settlementState = new SettlementState();

	private TiledMap areaMap;
	private MapEnvironment mapEnvironment;
	private Random random;
	private GameClock gameClock;
	private ConstantsRepo constantsRepo; // not persisted, assigned by ConstantsRepo singleton

	public TiledMap getAreaMap() {
		return areaMap;
	}

	public void setAreaMap(TiledMap areaMap) {
		this.areaMap = areaMap;
	}

	public MapEnvironment getMapEnvironment() {
		return mapEnvironment;
	}

	public void setMapEnvironment(MapEnvironment mapEnvironment) {
		this.mapEnvironment = mapEnvironment;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public void setGameClock(GameClock gameClock) {
		this.gameClock = gameClock;
	}

	public GameClock getGameClock() {
		return gameClock;
	}

	public Map<Long, Job> getJobs() {
		return jobs;
	}

	public Map<Long, Entity> getEntities() {
		return entities;
	}

	public Map<Long, Room> getRooms() {
		return rooms;
	}

	public Map<Long, Squad> getSquads() {
		return squads;
	}

	public Map<Long, Construction> getConstructions() {
		return constructions;
	}

	public Deque<JobRequestMessage> getJobRequestQueue() {
		return jobRequestQueue;
	}

	public void setSettlementState(SettlementState settlementState) {
		this.settlementState = settlementState;
	}

	public Map<String, GameMaterial> getDynamicallyCreatedMaterialsByCombinedId() {
		return dynamicallyCreatedMaterialsByCombinedId;
	}

	public SettlementState getSettlementState() {
		return settlementState;
	}

	public void setConstantsRepo(ConstantsRepo constantsRepo) {
		this.constantsRepo = constantsRepo;
	}

	public ConstantsRepo getConstantsRepo() {
		return constantsRepo;
	}

}
