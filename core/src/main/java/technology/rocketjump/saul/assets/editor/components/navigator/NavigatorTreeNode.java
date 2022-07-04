package technology.rocketjump.saul.assets.editor.components.navigator;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.widget.VisLabel;

public class NavigatorTreeNode extends Tree.Node<NavigatorTreeNode, NavigatorTreeValue, VisLabel> {

	@Override
	public void setValue(NavigatorTreeValue value) {
		super.setValue(value);
		this.setActor(new VisLabel(value.label));
	}
}
