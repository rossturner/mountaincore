package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.GameMaterialDictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class is used to hold entities to be displayed in the UI. Note that their materials and quantity can be changed while in use
 */
@Singleton
public class RoomEditorItemMap {

	private final Map<ItemType, Entity> byType = new HashMap<>();

	@Inject
	public RoomEditorItemMap(ItemTypeDictionary itemTypeDictionary,
							 ItemEntityAttributesFactory itemEntityAttributesFactory,
							 ItemEntityFactory itemEntityFactory,
							 GameMaterialDictionary materialDictionary) {
		Random random = new RandomXS128();
		GameContext fakeContext = new GameContext();
		fakeContext.setRandom(random);

		for (ItemType itemType : itemTypeDictionary.getAll()) {
			Entity entity = itemEntityFactory.createByItemType(itemType, fakeContext, false);
			byType.put(itemType, entity);
		}
	}

	public Entity getByItemType(ItemType itemType) {
		return byType.get(itemType);
	}

}
