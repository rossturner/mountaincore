package technology.rocketjump.saul.assets.editor.components.navigator;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTree;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.model.EditorState;
import technology.rocketjump.saul.entities.model.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static technology.rocketjump.saul.assets.editor.components.navigator.NavigatorTreeValue.forEntityDir;
import static technology.rocketjump.saul.assets.editor.components.navigator.NavigatorTreeValue.forSubDir;

public class NavigatorPane extends VisTable {

	private final VisTree navigatorTree;
	private final EditorState editorState;

	@Inject
	public NavigatorPane(EditorState editorState) {
		navigatorTree = new VisTree();
		this.editorState = editorState;
		reloadTree();
		VisScrollPane navigatorScrollPane = new VisScrollPane(navigatorTree);

//		this.setDebug(true);
		this.background("window-bg");
		this.add(new VisLabel("Navigator")).left().row();
		this.add(navigatorScrollPane).top().row();
		this.add(new VisTable()).expandY();
	}

	private void reloadTree() {
		navigatorTree.clearChildren();

		for (EntityType entityType : EntityType.values()) {
			NavigatorTreeNode treeNode = new NavigatorTreeNode();
			treeNode.setValue(NavigatorTreeValue.forEntityType(entityType, editorState.getModDir()));

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
							NavigatorTreeNode node = new NavigatorTreeNode();
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
