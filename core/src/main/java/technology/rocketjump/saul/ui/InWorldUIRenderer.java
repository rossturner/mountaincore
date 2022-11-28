package technology.rocketjump.saul.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.MapVertex;
import technology.rocketjump.saul.mapping.tile.designation.Designation;
import technology.rocketjump.saul.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.saul.mapping.tile.floor.BridgeTile;
import technology.rocketjump.saul.mapping.tile.underground.TileLiquidFlow;
import technology.rocketjump.saul.messaging.types.DoorwayPlacementMessage;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.particles.custom_libgdx.ShaderEffect;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.RenderingOptions;
import technology.rocketjump.saul.rendering.RoomRenderer;
import technology.rocketjump.saul.rendering.TerrainRenderer;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.custom_libgdx.CustomShaderSpriteBatch;
import technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.mechanisms.MechanismsViewModeRenderer;
import technology.rocketjump.saul.rendering.piping.PipingViewModeRenderer;
import technology.rocketjump.saul.rendering.roofing.RoofingViewModeRenderer;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.rooms.constructions.BridgeConstruction;
import technology.rocketjump.saul.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.saul.sprites.IconSpriteCache;
import technology.rocketjump.saul.sprites.TerrainSpriteCache;
import technology.rocketjump.saul.zones.Zone;

import java.util.*;

import static technology.rocketjump.saul.mapping.MapMessageHandler.getAttackableCreatures;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.rendering.camera.TileBoundingBox.*;
import static technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader.defaultShaderInstance;
import static technology.rocketjump.saul.rooms.RoomTypeDictionary.VIRTUAL_PLACING_ROOM;
import static technology.rocketjump.saul.ui.GameInteractionMode.*;
import static technology.rocketjump.saul.ui.GameViewMode.JOB_PRIORITY;

public class InWorldUIRenderer {

	// MODDING expose furniture placement colors
	public static final Color VALID_PLACEMENT_COLOR = HexColors.get("#00ee00aa");
	public static final Color INVALID_PLACEMENT_COLOR = HexColors.get("#ee0000aa");
	private static final long BLINK_DURATION_MILLIS = 800L;

	private final GameInteractionStateContainer interactionStateContainer;
	private final EntityRenderer entityRenderer;
	private final TerrainRenderer terrainRenderer;
	private final SelectableOutlineRenderer selectableOutlineRenderer;
	private final RoofingViewModeRenderer roofingViewModeRenderer;
	private final PipingViewModeRenderer pipingViewModeRenderer;
	private final MechanismsViewModeRenderer mechanismsViewModeRenderer;

	private final ShapeRenderer shapeRenderer = new ShapeRenderer();
	private final SpriteBatch spriteBatch = new SpriteBatch();
	private final SpriteBatch selectedEntitySpriteBatch;
	private final CustomShaderSpriteBatch customShaderSpriteBatch;
	private final RoomRenderer roomRenderer;
	private final RenderingOptions renderingOptions;
	private final JobStore jobStore;
	private final IconSpriteCache iconSpriteCache;
	private final TerrainSpriteCache diffuseTerrainSpriteCache;
	private final Sprite doorIconSprite;
	private boolean blinkState = true;
	private float elapsedSeconds;

	private Set<Entity> targetedCreatures = new HashSet<>();
	private Set<Entity> currentSquadTargetedCreatures = new HashSet<>();
	private final Designation flooringDesignation;
	private final Map<ItemType, FloorType> floorTypeMap = new HashMap<>();

	@Inject
	public InWorldUIRenderer(GameInteractionStateContainer interactionStateContainer, EntityRenderer entityRenderer,
							 TerrainRenderer terrainRenderer, RoomRenderer roomRenderer, RenderingOptions renderingOptions, JobStore jobStore,
							 FurnitureTypeDictionary furnitureTypeDictionary, IconSpriteCache iconSpriteCache,
							 SelectableOutlineRenderer selectableOutlineRenderer, RoofingViewModeRenderer roofingViewModeRenderer,
							 PipingViewModeRenderer pipingViewModeRenderer, MechanismsViewModeRenderer mechanismsViewModeRenderer,
							 DesignationDictionary designationDictionary, DiffuseTerrainSpriteCacheProvider diffuseTerrainSpriteCacheProvider,
							 FloorTypeDictionary floorTypeDictionary) {
		this.interactionStateContainer = interactionStateContainer;
		this.entityRenderer = entityRenderer;
		this.terrainRenderer = terrainRenderer;
		this.roomRenderer = roomRenderer;
		this.renderingOptions = renderingOptions;
		this.jobStore = jobStore;
		this.selectableOutlineRenderer = selectableOutlineRenderer;
		this.iconSpriteCache = iconSpriteCache;
		this.roofingViewModeRenderer = roofingViewModeRenderer;
		this.pipingViewModeRenderer = pipingViewModeRenderer;
		this.mechanismsViewModeRenderer = mechanismsViewModeRenderer;

		customShaderSpriteBatch = new CustomShaderSpriteBatch(1000, defaultShaderInstance);

		FurnitureType singleDoorType = furnitureTypeDictionary.getByName("SINGLE_DOOR");
		this.doorIconSprite = iconSpriteCache.getByName(singleDoorType.getIconName());

		FileHandle vertexShaderFile = ShaderLoader.DEFAULT_VERTEX_SHADER;
		FileHandle alphaPreservingFragmentShader = Gdx.files.classpath("shaders/alpha_preserving_fragment_shader.glsl");
		this.selectedEntitySpriteBatch = new SpriteBatch(100, ShaderLoader.createShader(vertexShaderFile, alphaPreservingFragmentShader));

		this.diffuseTerrainSpriteCache = diffuseTerrainSpriteCacheProvider.get();

		this.flooringDesignation = designationDictionary.getByName("FLOORING");
		if (this.flooringDesignation == null) {
			throw new RuntimeException("Could not find designation for flooring");
		}

		floorTypeDictionary.getAllDefinitions().stream()
				.filter(FloorType::isConstructed)
				.forEach(ft -> {
					ItemType itemType = ft.getRequirements().values().iterator().next().get(0).getItemType();
					floorTypeMap.put(itemType, ft);
				});
	}

	public boolean renderEntityMasks(OrthographicCamera camera, GameContext gameContext) {
		determineTargetedCreatures(gameContext);
		boolean hasEntitySelected = interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type == Selectable.SelectableType.ENTITY;
		boolean hasConstructionSelected = interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type == Selectable.SelectableType.CONSTRUCTION;
		boolean hasSquadSelected = interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type == Selectable.SelectableType.SQUAD;
		boolean hasTargets = !targetedCreatures.isEmpty() || !currentSquadTargetedCreatures.isEmpty();

		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		if (hasEntitySelected || hasTargets || hasSquadSelected || hasConstructionSelected) {
			selectedEntitySpriteBatch.setProjectionMatrix(camera.combined);
			selectedEntitySpriteBatch.enableBlending();
			selectedEntitySpriteBatch.begin();


			Selectable selectable = interactionStateContainer.getSelectable();
			if (hasEntitySelected && selectable.getEntity() != null) {
				Entity selectableEntity = selectable.getEntity();
				renderEntityWithFactionColor(selectableEntity);
			}

			if (hasConstructionSelected && selectable.getConstruction().getEntity() != null) {
				Entity selectableEntity = selectable.getConstruction().getEntity();
				renderEntityWithFactionColor(selectableEntity);
			}


			if (hasSquadSelected) {
				Squad squad = selectable.getSquad();
				for (Long memberEntityId : squad.getMemberEntityIds()) {
					Entity squadMember = gameContext.getEntities().get(memberEntityId);
					if (squadMember != null) {
						renderEntityWithFactionColor(squadMember);
					}
				}
			}

			for (Entity targetedCreature : targetedCreatures) {
				Color color = Color.ORANGE;
				entityRenderer.render(targetedCreature, selectedEntitySpriteBatch, RenderMode.DIFFUSE, null, color, null);
			}
			for (Entity targetedCreature : currentSquadTargetedCreatures) {
				Color color = Color.RED;
				entityRenderer.render(targetedCreature, selectedEntitySpriteBatch, RenderMode.DIFFUSE, null, color, null);
			}

			selectedEntitySpriteBatch.end();
			return true;
		} else {
			return false;
		}
	}

	private void renderEntityWithFactionColor(Entity selectableEntity) {
		Color color;
		FactionComponent factionComponent = selectableEntity.getComponent(FactionComponent.class);
		if (factionComponent != null) {
			color = factionComponent.getFaction().defensePoolBarColor;
		} else {
			color = Faction.SETTLEMENT.defensePoolBarColor;
		}
		LocationComponent locationComponent = selectableEntity.getLocationComponent();
		if (locationComponent.getWorldPosition() != null) {
			entityRenderer.render(selectableEntity, selectedEntitySpriteBatch, RenderMode.DIFFUSE, null, color, null);
		}
	}

	private void determineTargetedCreatures(GameContext gameContext) {
		targetedCreatures.clear();
		currentSquadTargetedCreatures.clear();

		Squad currentSelectedSquad = interactionStateContainer.getSelectable() != null ? interactionStateContainer.getSelectable().getSquad() : null;

		for (Squad squad : gameContext.getSquads().values()) {

			squad.getAttackEntityIds().stream()
					.map(id -> gameContext.getEntities().get(id))
					.filter(Objects::nonNull)
					.forEach(attackedEntity -> {
						if (currentSelectedSquad != null && currentSelectedSquad.getAttackEntityIds().contains(attackedEntity.getId())) {
							currentSquadTargetedCreatures.add(attackedEntity);
						} else {
							targetedCreatures.add(attackedEntity);
						}
					});
		}


		if ((interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SQUAD_ATTACK_CREATURE) ||
				interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_ATTACK_CREATURE)) &&
				interactionStateContainer.isDragging()) {
			Set<Entity> attackableCreatures = getAttackableCreatures(interactionStateContainer.getMinPoint(), interactionStateContainer.getMaxPoint(), gameContext);

			if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SQUAD_ATTACK_CREATURE)) {
				currentSquadTargetedCreatures.addAll(attackableCreatures);
				targetedCreatures.removeAll(attackableCreatures);
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_ATTACK_CREATURE)) {
				currentSquadTargetedCreatures.removeAll(attackableCreatures);
			}
		}
	}




	public void render(GameContext gameContext, OrthographicCamera camera, List<ParticleEffectInstance> particlesToRenderAsUI, TerrainSpriteCache diffuseSpriteCache) {
		TiledMap map = gameContext.getAreaMap();
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


		shapeRenderer.setProjectionMatrix(camera.combined);
		spriteBatch.setProjectionMatrix(camera.combined);

		interactionStateContainer.update();

		blinkState = (System.currentTimeMillis() % BLINK_DURATION_MILLIS) > BLINK_DURATION_MILLIS / 2;

		int minX = getMinX(camera);
		int maxX = getMaxX(camera, map);
		int minY = getMinY(camera);
		int maxY = getMaxY(camera, map);

		Vector2 minDraggingPoint = interactionStateContainer.getMinPoint();
		Vector2 maxDraggingPoint = interactionStateContainer.getMaxPoint();
		GridPoint2 minDraggingTile = new GridPoint2(MathUtils.floor(minDraggingPoint.x), MathUtils.floor(minDraggingPoint.y));
		GridPoint2 maxDraggingTile = new GridPoint2(MathUtils.floor(maxDraggingPoint.x), MathUtils.floor(maxDraggingPoint.y));

		if (interactionStateContainer.isDragging() && (
				// TODO better definition of which interaction modes should render this outline
				(interactionStateContainer.getInteractionMode().tileDesignationCheck != null && !interactionStateContainer.getInteractionMode().equals(DESIGNATE_POWER_LINES)) ||
						interactionStateContainer.getInteractionMode().equals(SET_JOB_PRIORITY)
		)) {
			drawDragAreaOutline(minDraggingPoint, maxDraggingPoint);
		}

		if (interactionStateContainer.getGameViewMode().equals(GameViewMode.ROOFING_INFO)) {
			roofingViewModeRenderer.render(map, camera, spriteBatch, shapeRenderer, blinkState);
			return;
		}
		if (interactionStateContainer.getGameViewMode().equals(GameViewMode.PIPING)) {
			pipingViewModeRenderer.render(map, camera, spriteBatch, shapeRenderer, blinkState);
			return;
		}
		if (interactionStateContainer.getGameViewMode().equals(GameViewMode.MECHANISMS)) {
			mechanismsViewModeRenderer.render(map, camera, spriteBatch, shapeRenderer, blinkState);
			return;
		}


		Selectable selectable = interactionStateContainer.getSelectable();
		if (selectable != null) {
			selectableOutlineRenderer.render(selectable, shapeRenderer, gameContext);

			if (GlobalSettings.DEV_MODE) {
				showCreatureGroupLocation(gameContext, selectable);
			}
		}

		if (interactionStateContainer.getInteractionMode().equals(PLACE_FURNITURE)) {
			Color furnitureColor = VALID_PLACEMENT_COLOR;
			if (!interactionStateContainer.isValidFurniturePlacement()) {
				furnitureColor = INVALID_PLACEMENT_COLOR;
			}

			Entity furnitureEntity = interactionStateContainer.getFurnitureEntityToPlace();
			spriteBatch.begin();
			entityRenderer.render(furnitureEntity, spriteBatch, RenderMode.DIFFUSE, null, furnitureColor, null);
			spriteBatch.end();

			shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(furnitureColor);

			GridPoint2 furnitureGridPoint = toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition());

			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
			for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
				GridPoint2 workspaceAccessedFrom = furnitureGridPoint.cpy().add(workspace.getAccessedFrom());
				shapeRenderer.circle(workspaceAccessedFrom.x + 0.5f, workspaceAccessedFrom.y + 0.5f, 0.35f, 50);
			}
			for (FurnitureLayout.SpecialTile specialTile : attributes.getCurrentLayout().getSpecialTiles()) {
				GridPoint2 specialTileLocation = furnitureGridPoint.cpy().add(specialTile.getLocation());
				shapeRenderer.setColor(specialTile.getRequirement().color);
				shapeRenderer.circle(specialTileLocation.x + 0.5f, specialTileLocation.y + 0.5f, 0.35f, 50);
			}

			shapeRenderer.end();
		} else if (interactionStateContainer.getInteractionMode().equals(PLACE_DOOR)) {
			DoorwayPlacementMessage virtualDoorPlacement = interactionStateContainer.getVirtualDoorPlacement();
			if (virtualDoorPlacement != null) {
				Color doorPlacementColor = VALID_PLACEMENT_COLOR;
				if (!interactionStateContainer.isValidDoorPlacement()) {
					doorPlacementColor = INVALID_PLACEMENT_COLOR;
				}

				spriteBatch.begin();
				spriteBatch.setColor(doorPlacementColor);
				spriteBatch.draw(doorIconSprite, virtualDoorPlacement.getTilePosition().x, virtualDoorPlacement.getTilePosition().y, 1, 1);
				spriteBatch.end();
			}
		} else if (interactionStateContainer.getInteractionMode().equals(PLACE_BRIDGE)) {
			BridgeConstruction virtualBridgeConstruction = interactionStateContainer.getVirtualBridgeConstruction();
			if (virtualBridgeConstruction != null) {
				Color bridgePlacementColor = VALID_PLACEMENT_COLOR;
				if (!interactionStateContainer.isValidBridgePlacement()) {
					bridgePlacementColor = INVALID_PLACEMENT_COLOR;
				}

				spriteBatch.begin();
				spriteBatch.setColor(bridgePlacementColor);
				for (Map.Entry<GridPoint2, BridgeTile> bridgeEntry : virtualBridgeConstruction.getBridge().entrySet()) {
					terrainRenderer.renderBridgeTile(bridgeEntry.getKey().x, bridgeEntry.getKey().y, virtualBridgeConstruction.getBridge(),
							bridgeEntry.getValue().getBridgeTileLayout(), diffuseSpriteCache, spriteBatch);
				}
				spriteBatch.end();
			}
		}

		spriteBatch.begin();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {

					if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y, interactionStateContainer)) {
						if (interactionStateContainer.getInteractionMode().equals(REMOVE_DESIGNATIONS)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().designationName != null) { // Is a designation
							// This is within dragging area
							if (shouldHighlight(mapTile)) {
								Designation designationToApply = interactionStateContainer.getInteractionMode().getDesignationToApply();
								spriteBatch.setColor(designationToApply.getSelectionColor());
								if (designationToApply.equals(flooringDesignation)) {
									renderFloorToBeConstructed(mapTile, interactionStateContainer.getFloorTypeToPlace());
								} else {
									renderDesignationSprite(designationToApply.getIconSprite(), x, y);
								}
							} else {
								renderExistingDesignation(x, y, mapTile);
							}
						} else if (interactionStateContainer.getInteractionMode().equals(PLACE_ROOM)) {
							// Do something for placing room
						} else {
							// Not a designation-type drag
							renderExistingDesignation(x, y, mapTile);
						}
					} else {
						// Outside selection area
						renderExistingDesignation(x, y, mapTile);
					}

					if (mapTile.hasRoom() && mapTile.getRoomTile().getRoom().getRoomType().getRoomName().equals(VIRTUAL_PLACING_ROOM.getRoomName())) {
						roomRenderer.render(mapTile, spriteBatch, diffuseSpriteCache);
					}

					if (interactionStateContainer.getGameViewMode().equals(JOB_PRIORITY)) {
						renderJobPriority(x, y, mapTile, minDraggingTile, maxDraggingTile);
					}

					if (renderingOptions.debug().showJobStatus()) {
						List<Job> jobsAtLocation = jobStore.getJobsAtLocation(mapTile.getTilePosition());
						for (Job jobAtLocation : jobsAtLocation) {

							switch (jobAtLocation.getJobState()) {
								case ASSIGNABLE:
									spriteBatch.setColor(Color.GREEN);
									shapeRenderer.setColor(Color.GREEN);
									break;
								case ASSIGNED:
									spriteBatch.setColor(Color.BLUE);
									shapeRenderer.setColor(Color.BLUE);
									break;
								case POTENTIALLY_ACCESSIBLE:
									spriteBatch.setColor(Color.ORANGE);
									shapeRenderer.setColor(Color.ORANGE);
									break;
								case INACCESSIBLE:
									spriteBatch.setColor(Color.RED);
									shapeRenderer.setColor(Color.RED);
									break;
							}

							if (jobAtLocation.getType().getName().equals("HAULING") && jobAtLocation.getHaulingAllocation().getTargetPosition() != null) {
								shapeRenderer.line(jobAtLocation.getJobLocation().x + 0.5f, jobAtLocation.getJobLocation().y + 0.5f,
										jobAtLocation.getHaulingAllocation().getTargetPosition().x + 0.5f, jobAtLocation.getHaulingAllocation().getTargetPosition().y + 0.5f);
							} else if (mapTile.getDesignation() != null) {
								renderDesignationSprite(mapTile.getDesignation().getIconSprite(), x, y);
							}


						}
					}

					if (renderingOptions.debug().isShowPathfindingSlowdown()) {
						for (Entity entity : mapTile.getEntities()) {
							if (entity.getType().equals(EntityType.CREATURE)) {
								Vector2 location = entity.getLocationComponent().getWorldPosition();
								Vector2 velocity = entity.getLocationComponent().getLinearVelocity();

								SteeringComponent steeringComponent = entity.getBehaviourComponent().getSteeringComponent();

								if (steeringComponent.getPauseTime() > 0) {
									shapeRenderer.setColor(Color.BLUE);
								} else if (steeringComponent.isSlowed()) {
									shapeRenderer.setColor(Color.RED);
								} else {
									shapeRenderer.setColor(Color.WHITE);
								}

								shapeRenderer.line(location.x, location.y, location.x + velocity.x, location.y + velocity.y);
							}
						}
					}

					if (renderingOptions.debug().isShowLiquidFlow()) {
						shapeRenderer.end();
						shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
						if (mapTile.getUnderTile() != null) {
							TileLiquidFlow liquidFlow = mapTile.getUnderTile().getLiquidFlow();
							if (liquidFlow != null && liquidFlow.getLiquidAmount() > 0) {
								switch (liquidFlow.getLiquidAmount()) {
									case 7:
										shapeRenderer.setColor(HexColors.get("#28ecf866"));
										break;
									case 6:
										shapeRenderer.setColor(HexColors.get("#25d8e366"));
										break;
									case 5:
										shapeRenderer.setColor(HexColors.get("#21bcc666"));
										break;
									case 4:
										shapeRenderer.setColor(HexColors.get("#18969f66"));
										break;
									case 3:
										shapeRenderer.setColor(HexColors.get("#13727866"));
										break;
									case 2:
										shapeRenderer.setColor(HexColors.get("#0c4b4f66"));
										break;
									case 1:
										shapeRenderer.setColor(HexColors.get("#06262866"));
										break;
									default:
										shapeRenderer.setColor(HexColors.get("#00000066"));
								}
								shapeRenderer.rect(mapTile.getTileX(), mapTile.getTileY(), 1,1);

								shapeRenderer.end();
								shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
								shapeRenderer.setColor(Color.RED);

								for (MapVertex vertex : gameContext.getAreaMap().getVertices(mapTile.getTileX(), mapTile.getTileY())) {
									shapeRenderer.line(vertex.getVertexX(), vertex.getVertexY(),
											vertex.getVertexX() + vertex.getWaterFlowDirection().x, vertex.getVertexY() + vertex.getWaterFlowDirection().y);
								}


							}
						}
					}

					if (renderingOptions.debug().isShowZones()) {
						if (!mapTile.getZones().isEmpty()) {
							Zone zone = mapTile.getZones().iterator().next();
							Random random = new RandomXS128(zone.getZoneId());
							Color color = new Color(
									random.nextFloat(),
									random.nextFloat(),
									random.nextFloat(),
									1
							);
							shapeRenderer.rect(x, y, 1, 1, color, color, color, color);
						}
					}

				}

			}
		}

		spriteBatch.setColor(Color.WHITE);

		particlesToRenderAsUI.forEach(p -> {
			if (!(p.getWrappedInstance() instanceof ShaderEffect)) {
				p.getWrappedInstance().draw(spriteBatch, null, RenderMode.DIFFUSE);
			}
		});
		// particle effects can override spritebatch shader without cleaning it up
		spriteBatch.end();
		shapeRenderer.end();


		if (!gameContext.getGameClock().isPaused()) {
			elapsedSeconds += Gdx.graphics.getDeltaTime() * gameContext.getGameClock().getSpeedMultiplier();
		}
		customShaderSpriteBatch.setProjectionMatrix(camera.combined);
		customShaderSpriteBatch.setElapsedTime(elapsedSeconds);
		customShaderSpriteBatch.setColor(Color.WHITE);
		customShaderSpriteBatch.begin();
		particlesToRenderAsUI.forEach(p -> {
			if (p.getWrappedInstance() instanceof ShaderEffect) {
				p.getWrappedInstance().draw(spriteBatch, customShaderSpriteBatch, RenderMode.DIFFUSE);
			}
		});
		customShaderSpriteBatch.end();
	}

	private void renderJobPriority(int x, int y, MapTile mapTile, GridPoint2 minDraggingTile, GridPoint2 maxDraggingTile) {
		JobPriority priority = null;

		if (mapTile.hasConstruction()) {
			priority = mapTile.getConstruction().getPriority();
		}

		List<Job> jobsAtLocation = jobStore.getJobsAtLocation(mapTile.getTilePosition());
		if (jobsAtLocation.size() > 0) {
			priority = jobsAtLocation.get(0).getJobPriority();
		}

		for (Entity entity : mapTile.getEntities()) {
			if (entity.getBehaviourComponent() instanceof Prioritisable) {
				Prioritisable prioritisableBehaviour = (Prioritisable) entity.getBehaviourComponent();
				priority = prioritisableBehaviour.getPriority();
			}
		}

		if (priority != null) {
			if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y, interactionStateContainer)) {
				priority = interactionStateContainer.getJobPriorityToApply();
			}

			spriteBatch.setColor(Color.WHITE);
			Sprite iconSprite = iconSpriteCache.getByName(priority.iconName);
			spriteBatch.draw(iconSprite, x, y, 1f, 1f);
		}
	}

	private void renderExistingDesignation(int x, int y, MapTile mapTile) {
		if (mapTile.getDesignation() != null) {
			Designation designation = mapTile.getDesignation();
			for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
				if (job.getAssignedToEntityId() != null) {
					// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
					if (!blinkState) {
						return;
					}
				}
			}

			spriteBatch.setColor(designation.getDesignationColor());
			if (designation.equals(flooringDesignation)) {
				Job flooringJob = jobStore.getJobsAtLocation(mapTile.getTilePosition())
						.stream().filter(j -> j.getType().equals(designation.getCreatesJobType()))
						.findFirst().orElse(null);
				if (flooringJob != null) {
					renderFloorToBeConstructed(mapTile, floorTypeMap.get(flooringJob.getRequiredItemType()));
				}
			} else {
				renderDesignationSprite(designation.getIconSprite(), x, y);
			}
		}
	}

	private void renderFloorToBeConstructed(MapTile mapTile, FloorType floorType) {
		Sprite spriteForFloor = diffuseTerrainSpriteCache.getFloorSpriteForType(floorType, mapTile.getSeed());
		spriteBatch.draw(spriteForFloor, mapTile.getTileX(), mapTile.getTileY(), 1, 1);
	}

	private void renderDesignationSprite(Sprite designation, int x, int y) {
		spriteBatch.draw(designation, x, y, 1, 1);
	}

	public static boolean insideSelectionArea(GridPoint2 minDraggingTile, GridPoint2 maxDraggingTile, int x, int y, GameInteractionStateContainer interactionStateContainer) {
		return interactionStateContainer.isDragging() &&
				minDraggingTile.x <= x && x <= maxDraggingTile.x &&
				minDraggingTile.y <= y && y <= maxDraggingTile.y;
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().tileDesignationCheck != null) {
			return interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(mapTile);
		} else {
			return false;
		}
	}

	private void drawDragAreaOutline(Vector2 minDraggingPoint, Vector2 maxDraggingPoint) {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.rect(minDraggingPoint.x, minDraggingPoint.y,
				(maxDraggingPoint.x - minDraggingPoint.x), (maxDraggingPoint.y - minDraggingPoint.y));
		shapeRenderer.end();
	}


	private void showCreatureGroupLocation(GameContext gameContext, Selectable selectable) {
		if (selectable.type.equals(Selectable.SelectableType.ENTITY)) {
			Entity selectedEntity = selectable.getEntity();
			if (selectedEntity.getBehaviourComponent() instanceof CreatureBehaviour) {
				CreatureGroup creatureGroup = ((CreatureBehaviour) selectedEntity.getBehaviourComponent()).getCreatureGroup();
				if (creatureGroup != null) {
					selectableOutlineRenderer.render(new Selectable(gameContext.getAreaMap().getTile(creatureGroup.getHomeLocation())), shapeRenderer, gameContext);
				}
			}
		}
	}

}
