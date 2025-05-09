package technology.rocketjump.mountaincore.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.guice.MountaincoreGuiceModule;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SettlementItemTrackerTest {

	private SettlementItemTracker settlementItemTracker;
	private ItemTypeDictionary itemTypeDictionary;
	private GameMaterialDictionary gameMaterialDictionary;
	@Mock
	private EntityAssetUpdater mockAssetUpdater;
	@Mock
	private GameContext mockContext;
	@Mock
	private ItemEntityAssetDictionary mockItemEntityAssetDictionary;
	@Mock
	private EntityAssetUpdater mockEntityAssetUpdater;

	@Before
	public void setUp() throws Exception {
		settlementItemTracker = new SettlementItemTracker();

		Injector injector = Guice.createInjector(new MountaincoreGuiceModule());
		itemTypeDictionary = injector.getInstance(ItemTypeDictionary.class);
		gameMaterialDictionary = injector.getInstance(GameMaterialDictionary.class);
	}

	@Test
	public void itemRemoved_cleansUpTree_whenItemRemoved() {
		Entity plankItem = buildItem("Resource-Planks", "Oak");

		settlementItemTracker.itemAdded(plankItem);

		assertThat(settlementItemTracker.getAllByItemType().values()).hasSize(1);

		settlementItemTracker.itemRemoved(plankItem);

		assertThat(settlementItemTracker.getAllByItemType().values()).isEmpty();
	}

	private Entity buildItem(String itemTypeName, String... materialNames) {
		ItemType itemType = itemTypeDictionary.getByName(itemTypeName);
		assertThat(itemType).isNotNull();

		GameMaterial[] materialArray = new GameMaterial[materialNames.length];
		for (int cursor = 0; cursor < materialNames.length; cursor++) {
			materialArray[cursor] = gameMaterialDictionary.getByName(materialNames[cursor]);
			assertThat(materialArray[cursor]).isNotNull();
		}

		ItemEntityAttributes itemAttributes = new ItemEntityAttributesFactory(mockItemEntityAssetDictionary, mockEntityAssetUpdater, gameMaterialDictionary).createItemAttributes(itemType, 1, materialArray);
		return new ItemEntityFactory(new MessageDispatcher(), gameMaterialDictionary, mockAssetUpdater).create(
				itemAttributes, new GridPoint2(), true, mockContext,
				Faction.SETTLEMENT);
	}
}