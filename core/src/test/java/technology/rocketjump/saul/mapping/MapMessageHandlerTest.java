package technology.rocketjump.saul.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.factories.MechanismEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.MechanismEntityFactory;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.TileNeighbours;
import technology.rocketjump.saul.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.rooms.RoomFactory;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.zones.Zone;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MapMessageHandlerTest {

	@Mock
	private FloorType mockFloorType;
	@Mock
	private WallType mockWallType;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	@Mock
	private OutdoorLightProcessor mockOutdoorLightProcessor;
	@Mock
	private GameInteractionStateContainer mockInteractionStateContainer;
	@Mock
	private RoomFactory mockRoomfactory;
	@Mock
	private RoomStore mockRoomStore;
	@Mock
	private JobStore mockJobStore;
	@Mock
	private StockpileComponentUpdater mockStockpileComponentUpdater;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectTypeDictionary;
	@Mock
	private SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	private RoofConstructionManager mockRoofConstructionManager;
	@Mock
	private FloorTypeDictionary mockFloorTypeDictionary;
	@Mock
	private MechanismTypeDictionary mockMechanismTypeDictionary;
	@Mock
	private MechanismEntityAttributesFactory mockMechanismEntityAttributesFactory;
	@Mock
	private MechanismEntityFactory mockMechanismEntityFactory;
	@Mock
	private I18nTranslator mockI18nTranslator;
	@Mock
	private DesignationDictionary mockDesignationDictionary;

	@Test
	public void removeWall_joinsRegions_keepsZones() {
		when(mockFloorType.getColorForHeightValue(Mockito.anyFloat())).thenReturn(Color.MAGENTA);
		TiledMap map = new TiledMap(1, 5, 5, mockFloorType, GameMaterial.NULL_MATERIAL);

		int region1 = map.createNewRegionId();
		int region2 = map.createNewRegionId();
		int region3 = map.createNewRegionId();

		map.getTile(0, 0).setRegionId(region1);
		map.getTile(0, 1).setRegionId(region1);
		map.getTile(0, 2).setRegionId(region1);
		map.getTile(0, 3).setRegionId(region1);
		map.getTile(0, 4).setRegionId(region1);
		map.getTile(1, 0).setRegionId(region1);
		map.getTile(1, 1).setRegionId(region1);
		map.getTile(1, 2).setRegionId(region1);
		map.getTile(1, 3).setRegionId(region1);
		map.getTile(1, 4).setRegionId(region1);

		map.getTile(2, 0).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 0).setRegionId(region2);
		map.getTile(2, 1).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 1).setRegionId(region2);
		map.getTile(2, 2).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 2).setRegionId(region2);
		map.getTile(2, 3).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 3).setRegionId(region2);
		map.getTile(2, 4).addWall(new TileNeighbours(), GameMaterial.NULL_MATERIAL, mockWallType);
		map.getTile(2, 4).setRegionId(region2);


		map.getTile(3, 0).setRegionId(region3);
		map.getTile(3, 1).setRegionId(region3);
		map.getTile(3, 2).setRegionId(region3);
		map.getTile(3, 3).setRegionId(region3);
		map.getTile(3, 4).setRegionId(region3);
		map.getTile(4, 0).setRegionId(region3);
		map.getTile(4, 1).setRegionId(region3);
		map.getTile(4, 2).setRegionId(region3);
		map.getTile(4, 3).setRegionId(region3);
		map.getTile(4, 4).setRegionId(region3);

		Zone leftEdgeZone = new Zone();
		leftEdgeZone.add(
				map.getTile(1, 0), map.getTile(0, 0)
		);
		leftEdgeZone.add(
				map.getTile(1, 1), map.getTile(0, 1)
		);
		leftEdgeZone.add(
				map.getTile(1, 2), map.getTile(0, 2)
		);
		leftEdgeZone.add(
				map.getTile(1, 3), map.getTile(0, 3)
		);
		leftEdgeZone.add(
				map.getTile(1, 4), map.getTile(0, 4)
		);
		map.addZone(leftEdgeZone);


		Zone rightEdgeZone = new Zone();
		rightEdgeZone.add(
				map.getTile(3, 0), map.getTile(4, 0)
		);
		rightEdgeZone.add(
				map.getTile(3, 1), map.getTile(4, 1)
		);
		rightEdgeZone.add(
				map.getTile(3, 2), map.getTile(4, 2)
		);
		rightEdgeZone.add(
				map.getTile(3, 3), map.getTile(4, 3)
		);
		rightEdgeZone.add(
				map.getTile(3, 4), map.getTile(4, 4)
		);
		map.addZone(rightEdgeZone);

		MapMessageHandler mapMessageHandler = new MapMessageHandler(mockMessageDispatcher, mockOutdoorLightProcessor,
				mockInteractionStateContainer, mockRoomfactory, mockRoomStore, mockJobStore, mockStockpileComponentUpdater,
				mockRoofConstructionManager, mockParticleEffectTypeDictionary, mockSoundAssetDictionary, mockFloorTypeDictionary,
				mockMechanismTypeDictionary, mockMechanismEntityAttributesFactory, mockMechanismEntityFactory, mockI18nTranslator,
				mockDesignationDictionary);
		GameContext gameContext = new GameContext();
		gameContext.setAreaMap(map);
		mapMessageHandler.onContextChange(gameContext);

		Telegram telegram = new Telegram();
		telegram.message = MessageType.REMOVE_WALL;
		telegram.extraInfo = new GridPoint2(2, 2);

		mapMessageHandler.handleMessage(telegram);

		MapTile leftTile = map.getTile(1, 0);
		MapTile rightTile = map.getTile(3, 0);
		assertThat(leftTile.getRegionId()).isEqualTo(rightTile.getRegionId());

		assertThat(leftTile.getZones()).hasSize(1);
		assertThat(rightTile.getZones()).hasSize(1);
	}

}