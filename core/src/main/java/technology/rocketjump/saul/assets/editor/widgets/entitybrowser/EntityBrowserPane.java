package technology.rocketjump.saul.assets.editor.widgets.entitybrowser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.*;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.EntityEditorPersistence;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.ClickThroughVisTree;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.CompleteEntityDefinitionDictionary;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.messaging.MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityBrowserPane extends VisTable {

	private final VisTree assetTree;
	private final EditorStateProvider editorStateProvider;
	private final VisLabel titleLabel;
	private final ObjectMapper objectMapper;
	private final MessageDispatcher messageDispatcher;
	private final CompleteAssetDictionary assetDictionary;
	private final EntityEditorPersistence entityEditorPersistence;
	private final CompleteEntityDefinitionDictionary entityDefinitionDictionary;

	private final Map<String, Path> descriptorPathsByAssetName = new TreeMap<>();

	@Inject
	public EntityBrowserPane(EditorStateProvider editorStateProvider, ObjectMapper objectMapper,
							 MessageDispatcher messageDispatcher, CompleteAssetDictionary assetDictionary,
							 EntityEditorPersistence entityEditorPersistence, CompleteEntityDefinitionDictionary entityDefinitionDictionary) {
		this.objectMapper = objectMapper;
		this.messageDispatcher = messageDispatcher;
		this.assetDictionary = assetDictionary;
		this.entityEditorPersistence = entityEditorPersistence;
		this.entityDefinitionDictionary = entityDefinitionDictionary;
		assetTree = new ClickThroughVisTree();
		this.editorStateProvider = editorStateProvider;
		VisScrollPane scrollPane = new VisScrollPane(assetTree);

//		this.setDebug(true);
		this.background("window-bg");
		titleLabel = new VisLabel("Entity Browser");
		this.add(titleLabel).left().row();
		this.add(scrollPane).top().left().row();

		VisTextButton saveButton = new VisTextButton("Save changes");
		saveButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					EntityBrowserPane.this.entityEditorPersistence.saveChanges(descriptorPathsByAssetName);
					messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, null);
				} catch (Exception e) {
					Logger.error("An error occurred while saving", e);
				}
			}
		});


		VisTextButton cancelButton = new VisTextButton("Cancel");
		cancelButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// TODO clear all state changes i.e. reload all (or just these) assets and types
				messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, null);
			}
		});

		VisTable controlsTable = new VisTable();
		controlsTable.add(saveButton).pad(5);
		controlsTable.add(cancelButton).pad(5).row();
		this.add(controlsTable).top().row();

		this.add(new VisTable()).expandY().row();
	}

	public void reload() {
		titleLabel.setText(editorStateProvider.getState().getEntitySelection().getTypeName());
		reloadTree();
	}

	private void reloadTree() {
		EditorEntitySelection selection = editorStateProvider.getState().getEntitySelection();
		descriptorPathsByAssetName.clear();
		assetTree.clearChildren();

		try {
			EntityBrowserTreeNode typeDescriptorNode = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
			typeDescriptorNode.setValue(EntityBrowserValue.forTypeDescriptor(selection.getEntityType(), Paths.get(selection.getBasePath()),
					entityDefinitionDictionary.get(selection.getEntityType(), selection.getTypeName())));
			assetTree.add(typeDescriptorNode);

			addNodesForDirectory(Path.of(selection.getBasePath()), selection.getEntityType(), null);
		} catch (IOException e) {
			Logger.error("Error while loading " + selection.getTypeName(), e);
		}
	}

	private void addNodesForDirectory(Path directoryPath, EntityType entityType, EntityBrowserTreeNode parentNode) throws IOException {
		try (Stream<Path> paths = Files.list(directoryPath)) {
			List<Path> subdirectories = paths.filter(Files::isDirectory)
					.collect(Collectors.toList());
			for (Path subDirectory : subdirectories) {
				EntityBrowserTreeNode subDirNode = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
				subDirNode.setValue(EntityBrowserValue.forSubDirectory(entityType, subDirectory));
				if (parentNode == null) {
					assetTree.add(subDirNode);
				} else {
					parentNode.add(subDirNode);
				}
				addNodesForDirectory(subDirectory, entityType, subDirNode);
			}
		}

		Path descriptorsFile = directoryPath.resolve("descriptors.json");
		if (Files.exists(descriptorsFile)) {
			JSONArray descriptorsJson = JSON.parseArray(Files.readString(descriptorsFile));
			List<EntityAsset> referencedAssets = new ArrayList<>();
			for (int cursor = 0; cursor < descriptorsJson.size(); cursor++) {
				String assetName = descriptorsJson.getJSONObject(cursor).getString("uniqueName");
				// Ensuring use of asset references from CompleteAssetDictionary so any data changes are reflected in rendering
				EntityAsset entityAsset = assetDictionary.getByUniqueName(assetName);
				if (entityAsset == null) {
					Logger.error("Could not find asset with unique name " + assetName);
				} else {
					referencedAssets.add(entityAsset);
					descriptorPathsByAssetName.put(assetName, descriptorsFile);
				}
			}
			referencedAssets.sort(Comparator.comparing(EntityAsset::getUniqueName));

			referencedAssets.forEach(asset -> {
				EntityBrowserTreeNode assetNode = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
				assetNode.setValue(EntityBrowserValue.forAsset(entityType, descriptorsFile, asset));
				if (parentNode == null) {
					assetTree.add(assetNode);
				} else {
					parentNode.add(assetNode);
				}
			});
		}
	}

}
