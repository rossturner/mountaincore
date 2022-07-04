package technology.rocketjump.saul.assets.editor.components;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTree;
import technology.rocketjump.saul.entities.model.EntityType;

public class NavigatorPane extends VisTable {

	private final VisTree navigatorTree;

	@Inject
	public NavigatorPane() {
		navigatorTree = new VisTree();
		for (EntityType entityType : EntityType.values()) {
			TreeNode entityTypeNode = new TreeNode(new VisLabel(entityType.name()));
			navigatorTree.add(entityTypeNode);
		}
		VisScrollPane navigatorScrollPane = new VisScrollPane(navigatorTree);

		this.setDebug(true);
		this.background("window-bg");
		this.add(new VisLabel("Navigator")).left().row();
		this.add(navigatorScrollPane).top().row();
		this.add(new VisTable()).expandY();
	}
	
}
