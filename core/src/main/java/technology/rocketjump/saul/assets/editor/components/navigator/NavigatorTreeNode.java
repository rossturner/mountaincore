package technology.rocketjump.saul.assets.editor.components.navigator;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.messaging.MessageType;

public class NavigatorTreeNode extends Tree.Node<NavigatorTreeNode, NavigatorTreeValue, VisLabel> {

	private final MessageDispatcher messageDispatcher;
	private final EditorStateProvider editorStateProvider;

	public NavigatorTreeNode(MessageDispatcher messageDispatcher, EditorStateProvider editorStateProvider) {
		this.messageDispatcher = messageDispatcher;
		this.editorStateProvider = editorStateProvider;
	}

	@Override
	public void setExpanded(boolean expanded) {
		super.setExpanded(expanded);

		if (expanded) {
			editorStateProvider.getState().getExpandedNavigatorNodes().add(getValue().label);
		} else {
			editorStateProvider.getState().getExpandedNavigatorNodes().remove(getValue().label);
		}
		editorStateProvider.stateChanged();
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

		if (editorStateProvider.getState().getExpandedNavigatorNodes().contains(value.label)) {
			super.setExpanded(true);
		}
	}
}
