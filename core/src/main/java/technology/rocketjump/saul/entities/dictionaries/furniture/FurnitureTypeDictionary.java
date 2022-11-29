package technology.rocketjump.saul.entities.dictionaries.furniture;

import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.ui.views.GuiViewName;

import java.io.IOException;
import java.util.*;

@Singleton
public class FurnitureTypeDictionary {

	private final Map<String, FurnitureType> byName = new HashMap<>();
	private final Map<GuiViewName, List<FurnitureType>> byGuiView = new HashMap<>();

	public static FurnitureType NULL_TYPE = new FurnitureType();
	static {
		NULL_TYPE.setName("Null furniture type");
	}

	private final FurnitureLayoutDictionary layoutDictionary;
	private final ItemTypeDictionary itemTypeDictionary;


	@Inject
	public FurnitureTypeDictionary(FurnitureLayoutDictionary layoutDictionary,
								   ItemTypeDictionary itemTypeDictionary) throws IOException {
		this(new FileHandle("assets/definitions/types/furnitureTypes.json"), layoutDictionary, itemTypeDictionary);
	}

	public FurnitureTypeDictionary(FileHandle jsonFile, FurnitureLayoutDictionary layoutDictionary, ItemTypeDictionary itemTypeDictionary) throws IOException {
		this.layoutDictionary = layoutDictionary;
		this.itemTypeDictionary = itemTypeDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		List<FurnitureType> furnitureTypes = objectMapper.readValue(jsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureType.class));

		for (FurnitureType furnitureType : furnitureTypes) {
			add(furnitureType);
		}
		byName.put(NULL_TYPE.getName(), NULL_TYPE);
	}

	public void add(FurnitureType furnitureType) {
		initialiseFurnitureType(furnitureType);

		byName.put(furnitureType.getName(), furnitureType);
		if (furnitureType.getShowInGuiView() != null) {
			byGuiView.computeIfAbsent(furnitureType.getShowInGuiView(), a -> new ArrayList<>()).add(furnitureType);
		}
	}

	public List<FurnitureType> getForGuiView(GuiViewName guiViewName) {
		return byGuiView.getOrDefault(guiViewName, List.of());
	}


	private void initialiseFurnitureType(FurnitureType furnitureType) {
		furnitureType.setDefaultLayout(layoutDictionary.getByName(furnitureType.getDefaultLayoutName()));
		if (furnitureType.getDefaultLayout() == null) {
			throw new RuntimeException("Could not find furniture layout: " + furnitureType.getDefaultLayoutName() + " for " + furnitureType.getName());
		}

		if (furnitureType.getRequirements() != null) {
			for (List<QuantifiedItemType> quantifiedItemTypes : furnitureType.getRequirements().values()) {
				for (QuantifiedItemType quantifiedItemType : quantifiedItemTypes) {
					ItemType itemType = itemTypeDictionary.getByName(quantifiedItemType.getItemTypeName());
					if (itemType == null) {
						throw new RuntimeException("Could not find item type: " + quantifiedItemType.getItemTypeName() + " for " + quantifiedItemType + " in " + furnitureType.getName());
					} else {
						quantifiedItemType.setItemType(itemType);
					}
				}
			}
		}

	}

	public FurnitureType getByName(String name) {
		return byName.get(name);
	}

	public Collection<FurnitureType> getAll() {
		return byName.values();
	}

}
