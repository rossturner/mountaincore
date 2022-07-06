package technology.rocketjump.saul.assets.editor.components.entitybrowser;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.*;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.messaging.MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityBrowserPane extends VisTable {

	private final VisTree assetTree;
	private final EditorStateProvider editorStateProvider;
	private final VisLabel titleLabel;
	private final ObjectMapper objectMapper;
	private MessageDispatcher messageDispatcher;

	@Inject
	public EntityBrowserPane(EditorStateProvider editorStateProvider, ObjectMapper objectMapper,
							 MessageDispatcher messageDispatcher) {
		this.objectMapper = objectMapper;
		this.messageDispatcher = messageDispatcher;
		assetTree = new VisTree();
		this.editorStateProvider = editorStateProvider;
		VisScrollPane scrollPane = new VisScrollPane(assetTree);

//		this.setDebug(true);
		this.background("window-bg");
		titleLabel = new VisLabel("Entity Browser");
		this.add(titleLabel).left().row();
		this.add(scrollPane).top().row();


		VisTextButton cancelButton = new VisTextButton("Cancel");
		cancelButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, null);
			}
		});

		VisTable controlsTable = new VisTable();
		controlsTable.add(new VisTextButton("Add new asset")).pad(5).colspan(2).row();
		controlsTable.add(new VisTextButton("Save changes")).pad(5);
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
		assetTree.clearChildren();

		try {
			EntityBrowserTreeNode typeDescriptorNode = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
			typeDescriptorNode.setValue(EntityBrowserValue.forTypeDescriptor(selection.getEntityType(), Paths.get(selection.getBasePath())));
			assetTree.add(typeDescriptorNode);

			List<EntityBrowserValue> descriptors = new ArrayList<>();
			loadDescriptors(Path.of(selection.getBasePath()), selection.getEntityType(), descriptors);
			descriptors.sort(Comparator.comparing(a -> a.label));
			descriptors.stream().map(value -> {
				EntityBrowserTreeNode node = new EntityBrowserTreeNode(messageDispatcher, editorStateProvider);
				node.setValue(value);
				return node;
			}).forEach(assetTree::add);

		} catch (IOException e) {
			Logger.error("Error while loading " + selection.getTypeName(), e);
		}
	}

	private void loadDescriptors(Path directory, EntityType entityType, List<EntityBrowserValue> descriptors) throws IOException {
		Path descriptorsFile = directory.resolve("descriptors.json");

		if (Files.exists(descriptorsFile)) {
			List<? extends EntityAsset> assetList = objectMapper.readValue(Files.readString(descriptorsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, entityType.entityAssetClass));
			for (EntityAsset entityAsset : assetList) {
				descriptors.add(EntityBrowserValue.forAsset(entityType, descriptorsFile, entityAsset));
			}
		}

		try (Stream<Path> paths = Files.list(directory)) {
			List<Path> subdirectories = paths.filter(Files::isDirectory)
					.collect(Collectors.toList());
			for (Path subDirectory : subdirectories) {
				loadDescriptors(subDirectory, entityType, descriptors);
			}
		}
	}

}
