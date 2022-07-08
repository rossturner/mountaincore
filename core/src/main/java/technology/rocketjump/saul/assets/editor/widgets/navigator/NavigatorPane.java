package technology.rocketjump.saul.assets.editor.widgets.navigator;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTree;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.ClickThroughVisTree;
import technology.rocketjump.saul.entities.model.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorTreeValue.forEntityDir;
import static technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorTreeValue.forSubDir;

public class NavigatorPane extends VisTable {

	private final VisTree navigatorTree;
	private final EditorStateProvider editorStateProvider;
	private MessageDispatcher messageDispatcher;

	@Inject
	public NavigatorPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
		navigatorTree = new ClickThroughVisTree();
		this.editorStateProvider = editorStateProvider;
		reloadTree();
		VisScrollPane navigatorScrollPane = new VisScrollPane(navigatorTree);

//		this.setDebug(true);
		this.background("window-bg");
		this.add(new VisLabel("Navigator")).left().row();
		this.add(navigatorScrollPane).top().left().row();
		this.add(new VisTable()).expandY();
	}

	private void reloadTree() {
		navigatorTree.clearChildren();

		for (EntityType entityType : EntityType.values()) {
			if (entityType.equals(EntityType.ONGOING_EFFECT)) {
				// Not adding ongoing effects to asset editor (yet)
				continue;
			}
			NavigatorTreeNode treeNode = new NavigatorTreeNode(messageDispatcher, editorStateProvider);
			treeNode.setValue(NavigatorTreeValue.forEntityType(entityType, editorStateProvider.getState().getModDirPath()));

			try {
				populateChildren(treeNode);
			} catch (IOException e) {
				Logger.error("Error parsing dir " + treeNode.getValue().path.toAbsolutePath(), e);
			}

			navigatorTree.add(treeNode);
		}
	}

	private void populateChildren(NavigatorTreeNode parentNode) throws IOException {
		try (Stream<Path> fileStream = Files.list(parentNode.getValue().path)) {
			fileStream
					.filter(Files::isDirectory)
					.forEach(childDir -> {
						try {
							NavigatorTreeNode node = new NavigatorTreeNode(messageDispatcher, editorStateProvider);
							if (hasEntityTypeDescriptor(childDir, parentNode.getValue().entityType)) {
								node.setValue(forEntityDir(parentNode.getValue().entityType, childDir));
							} else {
								node.setValue(forSubDir(parentNode.getValue().entityType, childDir));
								populateChildren(node);
							}

							parentNode.add(node);
						} catch (IOException | ClassCastException e) {
							Logger.error("Error parsing dir " + childDir.toAbsolutePath(), e);
						}
					});
		}
	}

	private boolean hasEntityTypeDescriptor(Path childDir, EntityType entityType) {
		return Files.exists(childDir.resolve(entityType.descriptorFilename));
	}

}
