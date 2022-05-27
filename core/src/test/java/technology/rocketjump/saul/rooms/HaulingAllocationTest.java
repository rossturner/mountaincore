package technology.rocketjump.saul.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.ItemEntityMessageHandler;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class HaulingAllocationTest {

	private GameContext gameContext;

	@Mock
	private TiledMap mockMap;
	@Mock
	private MapTile mockTile;
	@Mock
	private EntityAssetUpdater mockAssetUpdater;
	private Entity itemEntity;
	private MessageDispatcher messageDispatcher;
	private ItemEntityMessageHandler itemEntityMessageHandler;
	private ItemEntityAttributes attributes;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;

	@Before
	public void setup() {
		Injector injector = Guice.createInjector((binder) -> {
			binder.bind(EntityAssetUpdater.class).toInstance(mockAssetUpdater);
			binder.bind(ItemEntityAttributesFactory.class).toInstance(mockItemEntityAttributesFactory);
		});

		gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128(1L));
		gameContext.setGameClock(new GameClock());
		gameContext.setAreaMap(mockMap);

		ItemEntityFactory itemEntityFactory = injector.getInstance(ItemEntityFactory.class);
		ItemTypeDictionary itemTypeDictionary = injector.getInstance(ItemTypeDictionary.class);

		messageDispatcher = injector.getInstance(MessageDispatcher.class);
		itemEntityMessageHandler = injector.getInstance(ItemEntityMessageHandler.class);

		itemEntity = itemEntityFactory.createByItemType(itemTypeDictionary.getByName("Ingredient-Vegetable-Crate"), gameContext, true);
		attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();

	}

	@Test
	public void testSimpleAllocation() {
		attributes.setQuantity(2);

		messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(itemEntity, itemEntity, true, JobPriority.NORMAL, job -> {
			assertThat(job.getHaulingAllocation().getItemAllocation().getAllocationAmount()).isEqualTo(1);

			ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);

			assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(2);
			assertThat(itemAllocationComponent.getNumUnallocated()).isEqualTo(0);

			messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, job.getHaulingAllocation());

			assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(1);
			assertThat(itemAllocationComponent.getNumUnallocated()).isEqualTo(1);
		}));
	}

	@Test
	public void testLargeAmountAllocation() {
		attributes.setQuantity(30);
		final ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);
		itemAllocationComponent.createAllocation(10, itemEntity, ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION);

		messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(itemEntity, itemEntity, true, JobPriority.NORMAL, job -> {
			assertThat(job.getHaulingAllocation().getItemAllocation().getAllocationAmount()).isEqualTo(1);
			assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(30);
			assertThat(itemAllocationComponent.getNumUnallocated()).isEqualTo(0);

			messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, job.getHaulingAllocation());

			assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(10);
			assertThat(itemAllocationComponent.getNumUnallocated()).isEqualTo(20);
		}));
	}
}
