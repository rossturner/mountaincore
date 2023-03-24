package technology.rocketjump.mountaincore.assets;

import com.badlogic.gdx.files.FileHandle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.assets.model.OverlapType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FloorTypeDictionaryTest {

	private technology.rocketjump.mountaincore.assets.FloorTypeDictionary FloorTypeDictionary;
	@Mock
	private OverlapTypeDictionary mockOverlapDictionary;
	@Mock
	private CraftingTypeDictionary mockCraftingTypeDictionary;
	@Mock
	private ItemTypeDictionary mockItemTypeDictionary;

	@Before
	public void setUp() throws Exception {
		when(mockOverlapDictionary.getByName(anyString())).thenReturn(new OverlapType("fake"));
		FloorTypeDictionary = new FloorTypeDictionary(new FileHandle(new File("assets/definitions/types/floorTypes.json")),
				mockOverlapDictionary, mockCraftingTypeDictionary, mockItemTypeDictionary);
	}

	@Test
	public void get_returnsFloorForMatchingMaterial() {
		FloorType result = FloorTypeDictionary.getByFloorTypeName("grass");

		assertThat(result.getFloorTypeName()).isEqualTo("grass");
		assertThat(result.getMaterialType()).isEqualTo(GameMaterialType.EARTH);
		assertThat(result.getNumSprites()).isEqualTo(5);
		assertThat(result.getLayer()).isEqualTo(10);
	}

	@Test
	public void get_returnsNull_whenNoFloorForMaterial() {
		assertThat(FloorTypeDictionary.getByFloorTypeName("brick")).isNull();
	}

}