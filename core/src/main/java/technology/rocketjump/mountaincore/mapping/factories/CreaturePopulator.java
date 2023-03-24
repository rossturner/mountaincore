package technology.rocketjump.mountaincore.mapping.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.factories.CreatureEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.CreatureEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.*;
import technology.rocketjump.mountaincore.entities.model.physical.creature.features.RaceFeatures;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.TileExploration;
import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.settlement.CreatureTracker;

import java.util.*;
import java.util.function.Predicate;

import static technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState.CAVERN;

@Singleton
public class CreaturePopulator {

	private static final int MIN_ANIMALS_ON_SPAWN = 29;
	private static final int MAX_ANIMALS_ON_SPAWN = 57;
	private static final float MIN_DISTANCE_FROM_EMBARK = 30f;
	private static final float MIN_DISTANCE_FROM_OTHER_CREATURES = 20f;
	private static final int MAP_SIZE_ANIMAL_RATIO = 1500;
	private final RaceDictionary raceDictionary;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;
	private final CreatureTracker creatureTracker;

	@Inject
	public CreaturePopulator(RaceDictionary raceDictionary, CreatureEntityAttributesFactory creatureEntityAttributesFactory,
							 CreatureEntityFactory creatureEntityFactory, CreatureTracker creatureTracker) {
		this.raceDictionary = raceDictionary;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
		this.creatureTracker = creatureTracker;
	}


	public void initialiseMap(GameContext gameContext) {
		int animalsToAdd = selectInitialAnimalAmount(gameContext.getRandom());

		Logger.debug("Adding " + animalsToAdd + " animals");

		addAnimalsToMap(animalsToAdd, false, gameContext);
		if (!gameContext.getSettlementState().isPeacefulMode()) {
			addMonstersToMap(100, gameContext);
		}
	}

	public void addAnimalsAtEdge(GameContext gameContext) {
		int maxAnimalsForMapSize = gameContext.getAreaMap().getWidth() * gameContext.getAreaMap().getHeight() / MAP_SIZE_ANIMAL_RATIO;
		int maxAnimalsToAdd = maxAnimalsForMapSize - creatureTracker.count();
		if (maxAnimalsToAdd > 0) {
			int animalsToAdd = gameContext.getRandom().nextInt(maxAnimalsToAdd);
			addAnimalsToMap(animalsToAdd, true, gameContext);
		}
	}

	private void addAnimalsToMap(int animalsToAdd, boolean addingAtMapEdge, GameContext gameContext) {
		List<Race> animalRaces = raceDictionary.getAll().stream()
				.filter(r -> CreatureMapPlacement.ANIMAL.equals(r.getMapPlacement()))
				.toList();
		Set<GridPoint2> creatureSpawnLocations = new HashSet<>();

		Logger.debug("Adding " + animalsToAdd + " animals");

		while (animalsToAdd > 0) {
			Race selectedRace = animalRaces.get(gameContext.getRandom().nextInt(animalRaces.size()));
			MapTile spawnLocation = findSpawnLocation(gameContext, creatureSpawnLocations, addingAtMapEdge);
			if (spawnLocation == null) {
				Logger.warn("Could not find valid spawn location for more animals");
				break;
			} else {
				creatureSpawnLocations.add(spawnLocation.getTilePosition());
			}

			if (selectedRace.getBehaviour().getGroup() != null) {
				CreatureGroup group = new CreatureGroup();
				group.setGroupId(SequentialIdGenerator.nextId());
				group.setHomeLocation(spawnLocation.getTilePosition());

				int numToAddInGroup = selectGroupSize(selectedRace.getBehaviour().getGroup(), gameContext.getRandom());
				while (numToAddInGroup > 0) {
					MapTile spawnTile = addingAtMapEdge ? spawnLocation : pickNearbyTileInRegion(5, spawnLocation, gameContext);
					CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(selectedRace);
					Entity entity = creatureEntityFactory.create(attributes, spawnTile.getWorldPositionOfCenter(), new Vector2(), gameContext, Faction.WILD_ANIMALS);
					if (entity.getBehaviourComponent() instanceof CreatureBehaviour) {
						((CreatureBehaviour) entity.getBehaviourComponent()).setCreatureGroup(group);
					}
					numToAddInGroup--;
					animalsToAdd--;
				}
			} else {
				// add individual to map
				CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(selectedRace);
				creatureEntityFactory.create(attributes, spawnLocation.getWorldPositionOfCenter(), new Vector2(), gameContext, Faction.WILD_ANIMALS);
				animalsToAdd--;
			}

		}
	}

	private void addMonstersToMap(int amount, GameContext gameContext) {
		TiledMap areaMap = gameContext.getAreaMap();

		List<Race> monsterRaces = raceDictionary.getAll().stream()
				.filter(r -> CreatureMapPlacement.CAVE_MONSTER.equals(r.getMapPlacement()))
				.sorted(Comparator.comparingInt(this::monsterDifficulty))
				.toList();

		if (monsterRaces.isEmpty()) {
			return;
		}

		int maxDifficulty = monsterDifficulty(monsterRaces.get(monsterRaces.size() - 1));

		Predicate<RegionInformation> allRoofsAreCavern = regionInformation -> {
			boolean allCavern = true;
			for (MapTile mapTile : regionInformation.tilesInRegion) {
				allCavern &= mapTile.getRoof() != null && CAVERN == mapTile.getRoof().getState();
			}
			return allCavern;
		};
		List<RegionInformation> cavesFurthestFirst = Arrays.stream(findRegionsFromEmbarkPoint(areaMap, areaMap.getEmbarkPoint()))
				.filter(allRoofsAreCavern)
				.sorted(Comparator.<RegionInformation>comparingInt(value -> value.distanceFromEmbarkPoint).reversed())
				.toList();

		if (cavesFurthestFirst.size() > 0) {
			int maxDistance = cavesFurthestFirst.get(0).distanceFromEmbarkPoint;

			for (RegionInformation cave : cavesFurthestFirst) {
				if (amount > 0 && cave.tilesInRegion.size() > 1) {
					//TODO: definitely refactor and test common components
					//at 1.0, all difficulties available, at 0.0, no difficulties
					float caveDistanceRatio =  (float) cave.distanceFromEmbarkPoint / maxDistance;
					Race monster = selectMonsterRace(gameContext, monsterRaces, maxDifficulty, caveDistanceRatio);
					if (monster == null) {
						continue; //rocky yuk
					}

					CreatureGroup group = new CreatureGroup();
					int numToAddInGroup = Integer.MAX_VALUE;
					boolean shouldGroup = monster.getBehaviour().getGroup() != null;
					if (shouldGroup) {
						group.setGroupId(SequentialIdGenerator.nextId());
						group.setHomeLocation(cave.tilesInRegion.get(gameContext.getRandom().nextInt(cave.tilesInRegion.size())).getTilePosition());
						numToAddInGroup = selectGroupSize(monster.getBehaviour().getGroup(), gameContext.getRandom());
					}

					for (int i = 0; i < cave.tilesInRegion.size() && numToAddInGroup > 0; i+=40) {
						MapTile randomCaveTile = cave.tilesInRegion.get(gameContext.getRandom().nextInt(cave.tilesInRegion.size()));
						CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(monster);
						Entity entity = creatureEntityFactory.create(attributes, randomCaveTile.getWorldPositionOfCenter(), new Vector2(), gameContext, Faction.MONSTERS);

						if (shouldGroup && entity.getBehaviourComponent() instanceof CreatureBehaviour) {
							((CreatureBehaviour) entity.getBehaviourComponent()).setCreatureGroup(group);
						}

						numToAddInGroup--;
						amount--;
					}
				}
			}
		}
	}

	//TODO: this selection process needs tidying and not the 1.2f fudge factor
	private Race selectMonsterRace(GameContext gameContext, List<Race> monsterRaces, int hardestMonsterDifficulty, float ratioOfDifficultyBarToUse) {
		int difficultyCap = (int) Math.ceil(hardestMonsterDifficulty * ratioOfDifficultyBarToUse * 1.2f);


		int totalMonsterDifficulty = monsterRaces.stream().mapToInt(this::monsterDifficulty).filter(difficulty -> difficulty <= difficultyCap).sum();

		if (totalMonsterDifficulty <= 0) {
			return null;
		}

		int selection = gameContext.getRandom().nextInt(totalMonsterDifficulty);

		Race monster = null;
		Iterator<Race> monsterIterator = monsterRaces.iterator();
		while(selection > 0) {
			monster = monsterIterator.next();
			selection -= this.monsterDifficulty(monster);
		}
		return monster;
	}

	private RegionInformation[] findRegionsFromEmbarkPoint(TiledMap areaMap, GridPoint2 embarkPoint) {
		RegionInformation[] regions = new RegionInformation[areaMap.getNumRegions()];
		int mapWidth = areaMap.getWidth();
		int mapHeight = areaMap.getHeight();
		boolean[] visited = new boolean[mapWidth * mapHeight];
		Arrays.fill(visited, false);

		Deque<MapTile> frontier = new LinkedList<>();
		frontier.push(areaMap.getTile(embarkPoint));
		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.pop();
			visited[currentTile.getTileY() * mapWidth + currentTile.getTileX()] = true;
			if (regions[currentTile.getRegionId()-1] == null) {
				regions[currentTile.getRegionId()-1] = new RegionInformation(currentTile, chebyshevDistance(currentTile.getTilePosition(), embarkPoint));
			} else {
				regions[currentTile.getRegionId()-1].tilesInRegion.add(currentTile);
			}

			TileNeighbours neighbours = areaMap.getNeighbours(currentTile.getTilePosition());
			for (MapTile neighbour : neighbours.values()) {
				if (!visited[neighbour.getTileY() * mapWidth + neighbour.getTileX()]) {
					visited[neighbour.getTileY() * mapWidth + neighbour.getTileX()] = true;
					frontier.add(neighbour);
				}
			}
		}
		return regions;
	}

	private int chebyshevDistance(GridPoint2 a, GridPoint2 b) {
		return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}

	private int monsterDifficulty(Race race) {
		int difficulty = 0;
		RaceFeatures features = race.getFeatures();
		if (features.getDefense() != null && features.getDefense().getMaxDefensePoints() != null) {
			difficulty += features.getDefense().getMaxDefensePoints();
		}
		if (features.getUnarmedWeapon() != null) {
			difficulty += features.getUnarmedWeapon().getMaxDamage();
		}
		return difficulty;
	}

	private MapTile findSpawnLocation(GameContext gameContext, Set<GridPoint2> creatureSpawnLocations, boolean addingAtMapEdge) {
		for (int attempt = 0; attempt <= 100; attempt++) {
			TiledMap map = gameContext.getAreaMap();
			MapTile randomTile;
			if (addingAtMapEdge) {
				if (gameContext.getRandom().nextBoolean()) {
					// Adding at top or bottom
					if (gameContext.getRandom().nextBoolean()) {
						// adding as top edge
						randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), map.getHeight() - 1);
					} else {
						// adding at bottom edge
						randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), 0);
					}
				} else {
					// adding at left or right
					if (gameContext.getRandom().nextBoolean()) {
						// adding at left edge
						randomTile = map.getTile(0, gameContext.getRandom().nextInt(map.getHeight()));
					} else {
						// adding at right edge
						randomTile = map.getTile(map.getWidth() - 1, gameContext.getRandom().nextInt(map.getHeight()));
					}
				}
			} else {
				randomTile = map.getTile(gameContext.getRandom().nextInt(map.getWidth()), gameContext.getRandom().nextInt(map.getHeight()));
			}
			// pick random map location


			if (!randomTile.getExploration().equals(TileExploration.EXPLORED)) {
				continue;
			}
			if (!randomTile.isNavigable(null) || !randomTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				continue;
			}

			if (withinDistance(MIN_DISTANCE_FROM_EMBARK, randomTile.getTilePosition(), gameContext.getAreaMap().getEmbarkPoint())) {
				continue;
			}

			boolean tooCloseToOtherAnimalSpawn = false;
			for (GridPoint2 creatureSpawnLocation : creatureSpawnLocations) {
				if (withinDistance(MIN_DISTANCE_FROM_OTHER_CREATURES, randomTile.getTilePosition(), creatureSpawnLocation)) {
					tooCloseToOtherAnimalSpawn = true;
					break;
				}
			}

			if (tooCloseToOtherAnimalSpawn) {
				continue;
			}

			return randomTile;
		}
		return null;
	}

	private MapTile pickNearbyTileInRegion(int radius, MapTile centralPoint, GameContext gameContext) {
		MapTile tileFound = null;
		while (tileFound == null) {
			tileFound = gameContext.getAreaMap().getTile(
					centralPoint.getTileX() - radius + (gameContext.getRandom().nextInt((radius * 2) + 1)),
					centralPoint.getTileY() - radius + (gameContext.getRandom().nextInt((radius * 2) + 1))
			);
			if (tileFound != null && (tileFound.getRegionId() != centralPoint.getRegionId() || !tileFound.isNavigable(null))) {
				tileFound = null;
			}
		}
		return tileFound;
	}

	private boolean withinDistance(float minDistance, GridPoint2 positionA, GridPoint2 positionB) {
		return positionA.dst(positionB) < minDistance;
	}

	private int selectInitialAnimalAmount(Random random) {
		return MIN_ANIMALS_ON_SPAWN + random.nextInt(MAX_ANIMALS_ON_SPAWN - MIN_ANIMALS_ON_SPAWN);
	}

	private int selectGroupSize(RaceBehaviourGroup group, Random random) {
		return group.getMinSize() + random.nextInt(group.getMaxSize() - group.getMinSize());
	}

	class RegionInformation {
		private final MapTile nearestTile;
		private final List<MapTile> tilesInRegion;
		private final int distanceFromEmbarkPoint;

		RegionInformation(MapTile nearestTile, int distanceFromEmbarkPoint) {
			this.nearestTile = nearestTile;
			this.distanceFromEmbarkPoint = distanceFromEmbarkPoint;
			this.tilesInRegion = new ArrayList<>();
			this.tilesInRegion.add(nearestTile);
		}
	}
}
