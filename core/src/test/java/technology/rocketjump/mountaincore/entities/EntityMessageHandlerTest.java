package technology.rocketjump.mountaincore.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.JobFactory;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.rooms.RoomStore;
import technology.rocketjump.mountaincore.settlement.*;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityMessageHandlerTest {

	private static final long TARGET_ENTITY_ID = 7L;
	private EntityMessageHandler entityMessageHandler;
	private MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	@Mock
	private EntityStore mockEntityStore;
	@Mock
	private TiledMap mockAreaMap;
	@Mock
	private Entity mockEntity;
	@Mock
	private LocationComponent mockLocationComponent;
	@Mock
	private MapTile mockTile;
	@Mock
	private EntityAssetUpdater mockAssetUpdater;
	@Mock
	private JobFactory mockJobFactory;
	@Mock
	private SettlementItemTracker mockSettlementItemTracker;
	@Mock
	private SettlementFurnitureTracker mockSettlementFurnitureTracker;
	@Mock
	private SettlerTracker mockSettlerTracker;
	@Mock
	private RoomStore mockRoomStore;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;
	@Mock
	private ItemEntityFactory mockItemEntityFactory;
	@Mock
	private ItemTypeDictionary mockItemTypeDictionary;
	@Mock
	private I18nTranslator mockI18nTranslator;
	@Mock
	private JobStore mockJobStore;
	@Mock
	private SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectDictionary;
	@Mock
	private OngoingEffectTracker mockOngoingEffectTracker;
	@Mock
	private GameMaterialDictionary mockMaterialDictionary;
	@Mock
	private CreatureTracker mockCreatureTracker;
	@Mock
	private DesignationDictionary mockDesignationDictionary;
	@Mock
	private VehicleTracker mockVehicleTracker;

	@Before
	public void setUp() throws Exception {
		messageDispatcher = new MessageDispatcher();

		entityMessageHandler = new EntityMessageHandler(messageDispatcher, mockAssetUpdater, mockJobFactory,
				mockEntityStore, mockSettlementItemTracker, mockSettlementFurnitureTracker, mockSettlerTracker, mockCreatureTracker, mockOngoingEffectTracker, mockRoomStore,
				mockItemEntityAttributesFactory, mockItemEntityFactory, mockItemTypeDictionary, mockI18nTranslator, mockJobStore,
				mockMaterialDictionary, mockSoundAssetDictionary, mockVehicleTracker, mockParticleEffectDictionary, mockDesignationDictionary);

		gameContext = new GameContext();
		gameContext.setAreaMap(mockAreaMap);

		entityMessageHandler.onContextChange(gameContext);
	}

	@Test
	public void handles_DestroyEntityMessage() {
		when(mockEntityStore.remove(TARGET_ENTITY_ID)).thenReturn(mockEntity);
		Vector2 entityWorldPosition = new Vector2(0.5f, 0.5f);
		when(mockEntity.getId()).thenReturn(TARGET_ENTITY_ID);
		when(mockEntity.getLocationComponent()).thenReturn(mockLocationComponent);
		when(mockEntity.getType()).thenReturn(EntityType.CREATURE);
		when(mockLocationComponent.getWorldPosition()).thenReturn(entityWorldPosition);
		when(mockAreaMap.getTile(entityWorldPosition)).thenReturn(mockTile);
		when(mockEntityStore.getById(TARGET_ENTITY_ID)).thenReturn(mockEntity);

		messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, mockEntity);

		verify(mockEntityStore).remove(TARGET_ENTITY_ID);
		verify(mockTile).removeEntity(TARGET_ENTITY_ID);
	}

}