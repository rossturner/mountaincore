package technology.rocketjump.mountaincore.guice;

import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.FloorTypeDictionary;
import technology.rocketjump.mountaincore.assets.OverlapTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;

import java.io.IOException;


@Singleton
public class FloorDictionaryProvider implements Provider<FloorTypeDictionary> {

	@Inject
	private OverlapTypeDictionary overlapTypeDictionary;
	@Inject
	private CraftingTypeDictionary craftingTypeDictionary;
	@Inject
	private ItemTypeDictionary itemTypeDictionary;

	@Override
	public FloorTypeDictionary get() {
		try {
			return new FloorTypeDictionary(Gdx.files.internal("assets/definitions/types/floorTypes.json"), overlapTypeDictionary, craftingTypeDictionary, itemTypeDictionary);
		} catch (IOException e) {
			throw new ProvisionException("Failed to create " + FloorTypeDictionary.class.getSimpleName(), e);
		}
	}
}
