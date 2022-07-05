package technology.rocketjump.saul.assets.editor.components.navigator;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import technology.rocketjump.saul.messaging.MessageType;

public class NavigatorTreeNode extends Tree.Node<NavigatorTreeNode, NavigatorTreeValue, VisLabel> {

	private final MessageDispatcher messageDispatcher;

	public NavigatorTreeNode(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void setValue(NavigatorTreeValue value) {
		super.setValue(value);
		VisLabel actor = new VisLabel(value.label);
		this.setActor(actor);
		actor.addListener(new ClickListener(Input.Buttons.RIGHT) {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.EDITOR_NAVIGATOR_TREE_RIGHT_CLICK,
						new NavigatorTreeMessage(value, actor));
			}
		});
	}
}
