package technology.rocketjump.saul.assets.editor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class EntityEditorPersistence {

	private final EditorStateProvider editorStateProvider;
	private final CompleteAssetDictionary assetDictionary;
	private final RaceDictionary raceDictionary;
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final MechanismTypeDictionary mechanismTypeDictionary;
	private final ObjectWriter objectWriter;

	@Inject
	public EntityEditorPersistence(EditorStateProvider editorStateProvider, CompleteAssetDictionary assetDictionary,
								   RaceDictionary raceDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
								   ItemTypeDictionary itemTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
								   MechanismTypeDictionary mechanismTypeDictionary) {
		this.editorStateProvider = editorStateProvider;
		this.assetDictionary = assetDictionary;
		this.raceDictionary = raceDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.mechanismTypeDictionary = mechanismTypeDictionary;

		this.objectWriter = new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
				.writerWithDefaultPrettyPrinter();

	}

	public void saveChanges(Map<String, Path> descriptorPathsByAssetName) throws IOException {
		EditorEntitySelection entitySelection = editorStateProvider.getState().getEntitySelection();
		Logger.info("Saving type descriptor and asset descriptors for " + entitySelection.getTypeName());

		Map<Path, List<String>> assetsNamesByDescriptorPath = new LinkedHashMap<>();
		descriptorPathsByAssetName.forEach((key, value) -> assetsNamesByDescriptorPath.computeIfAbsent(value, a -> new ArrayList<>()).add(key));

		for (Map.Entry<Path, List<String>> entry : assetsNamesByDescriptorPath.entrySet()) {
			Path descriptorPath = entry.getKey();

			List<EntityAsset> assetList = new ArrayList<>();
			entry.getValue().forEach(assetName -> assetList.add(assetDictionary.getByUniqueName(assetName)));

			Files.writeString(descriptorPath, objectWriter.writeValueAsString(assetList));
		}

		Object typeDescriptorInstance = getTypeDescriptorInstance(entitySelection.getTypeName(), entitySelection.getEntityType());
		Path typeDescriptorPath = Path.of(entitySelection.getBasePath(), entitySelection.getEntityType().descriptorFilename);
		Files.writeString(typeDescriptorPath, objectWriter.writeValueAsString(typeDescriptorInstance));

	}

	private Object getTypeDescriptorInstance(String typeName, EntityType entityType) {
		return switch (entityType) {
			case CREATURE -> raceDictionary.getByName(typeName);
			case PLANT -> plantSpeciesDictionary.getByName(typeName);
			case ITEM -> itemTypeDictionary.getByName(typeName);
			case FURNITURE -> furnitureTypeDictionary.getByName(typeName);
			case MECHANISM -> mechanismTypeDictionary.getByName(typeName);
			default -> "Not yet implemented";
		};
	}
}
