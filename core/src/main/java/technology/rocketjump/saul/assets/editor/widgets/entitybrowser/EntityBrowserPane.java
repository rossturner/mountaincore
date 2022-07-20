package technology.rocketjump.saul.assets.editor.widgets.entitybrowser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
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
import technology.rocketjump.saul.persistence.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityBrowserPane extends VisTable implements Telegraph {

	private final VisTree assetTree;
	private final EditorStateProvider editorStateProvider;
	private final VisLabel titleLabel;
	private final MessageDispatcher messageDispatcher;
	private final CompleteAssetDictionary assetDictionary;
	private final EntityEditorPersistence entityEditorPersistence;
	private final CompleteEntityDefinitionDictionary entityDefinitionDictionary;

	private final Map<String, Path> descriptorPathsByAssetName = new TreeMap<>();

	@Inject
	public EntityBrowserPane(EditorStateProvider editorStateProvider,
							 MessageDispatcher messageDispatcher, CompleteAssetDictionary assetDictionary,
							 EntityEditorPersistence entityEditorPersistence, CompleteEntityDefinitionDictionary entityDefinitionDictionary) {
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

		messageDispatcher.addListener(this, MessageType.EDITOR_ASSET_CREATED);
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
			Object typeDescriptorInstance = entityDefinitionDictionary.get(selection.getEntityType(), selection.getTypeName());
			typeDescriptorNode.setValue(EntityBrowserValue.forTypeDescriptor(selection.getEntityType(), Paths.get(selection.getBasePath()), typeDescriptorInstance));
			assetTree.add(typeDescriptorNode);

			addNodesForDirectory(Path.of(selection.getBasePath()), selection.getEntityType(), null, typeDescriptorInstance);
		} catch (IOException e) {
			Logger.error("Error while loading " + selection.getTypeName(), e);
		}
	}

	private void addNodesForDirectory(Path directoryPath, EntityType entityType, EntityBrowserTreeNode parentNode, Object typeDescriptorInstance) throws IOException {
		try (Stream<Path> paths = Files.list(directoryPath)) {
			List<Path> subdirectories = paths.filter(Files::isDirectory)
					.collect(Collectors.toList());
			for (Path subDirectory : subdirectories) {
				EntityBrowserTreeNode subDirNode = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
				subDirNode.setValue(EntityBrowserValue.forSubDirectory(entityType, subDirectory, typeDescriptorInstance));
				if (parentNode == null) {
					assetTree.add(subDirNode);
				} else {
					parentNode.add(subDirNode);
				}
				addNodesForDirectory(subDirectory, entityType, subDirNode, typeDescriptorInstance);
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
				assetNode.setValue(EntityBrowserValue.forAsset(entityType, descriptorsFile, asset, typeDescriptorInstance));
				if (parentNode == null) {
					assetTree.add(assetNode);
				} else {
					parentNode.add(assetNode);
				}
			});
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.EDITOR_ASSET_CREATED -> newAssetCreated((EntityBrowserValue)msg.extraInfo);
			default -> Logger.error("Unexpected message type handled: " + msg.message);
		}
		return true;
	}

	private void newAssetCreated(EntityBrowserValue value) {
		Path targetDirectory = FileUtils.getDirectory(value.path);
		EntityBrowserTreeNode targetParentNode = findSubDirNode(targetDirectory);


		EntityBrowserTreeNode assetNode = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
		assetNode.setValue(value);
		descriptorPathsByAssetName.put(value.label, value.path);
		targetParentNode.add(assetNode);
	}

	private EntityBrowserTreeNode findSubDirNode(Path targetDirectory) {
		Array<EntityBrowserTreeNode> nodes = assetTree.getNodes();
		Deque<EntityBrowserTreeNode> frontier = new ArrayDeque<>();
		for (EntityBrowserTreeNode node : nodes) {
			frontier.add(node);
		}

		while (!frontier.isEmpty()) {
			EntityBrowserTreeNode node = frontier.pop();

			if (node.getValue().treeValueType.equals(EntityBrowserValue.TreeValueType.SUBDIR)) {
				if (targetDirectory.equals(node.getValue().path)) {
					return node;
				}
				for (EntityBrowserTreeNode child : node.getChildren()) {
					frontier.add(child);
				}
			}
		}

		throw new IllegalArgumentException("Could not find valid parent node for descriptor at " + targetDirectory);
	}
}
