package technology.rocketjump.mountaincore.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ItemAllocationComponentTest {

	private ItemAllocationComponent itemAllocationComponent;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	@Mock
	private Entity mockEntity;
	@Mock
	private GameContext mockGameContext;
	@Mock
	private PhysicalEntityComponent mockPhysicalComponent;
	private ItemEntityAttributes itemAttributes;


	@Before
	public void setup() {
		itemAttributes = new ItemEntityAttributes();
		itemAttributes.setQuantity(10);

		when(mockEntity.getType()).thenReturn(EntityType.ITEM);
		when(mockGameContext.getGameClock()).thenReturn(new GameClock());

		itemAllocationComponent = new ItemAllocationComponent();
		itemAllocationComponent.init(mockEntity, mockMessageDispatcher, mockGameContext);

		when(mockEntity.getId()).thenReturn(7L);
		when(mockEntity.getPhysicalEntityComponent()).thenReturn(mockPhysicalComponent);
		when(mockPhysicalComponent.getAttributes()).thenReturn(itemAttributes);
	}

	@Test
	public void cancel() {

		ItemAllocation allocation1 = itemAllocationComponent.createAllocation(1, mockEntity, ItemAllocation.Purpose.FOOD_ALLOCATION);
		ItemAllocation allocation2 = itemAllocationComponent.createAllocation(1, mockEntity, ItemAllocation.Purpose.FOOD_ALLOCATION);

		assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(2);

		itemAllocationComponent.cancel(allocation1);

		assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(1);

		itemAllocationComponent.cancel(allocation1);

		assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(1);

		itemAllocationComponent.cancel(allocation2);

		assertThat(itemAllocationComponent.getNumAllocated()).isEqualTo(0);

	}
}