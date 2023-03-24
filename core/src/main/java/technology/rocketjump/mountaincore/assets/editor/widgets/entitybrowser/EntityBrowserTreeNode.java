package technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.messaging.MessageType;

import static technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_ASSET_DESCRIPTOR;
import static technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_TYPE_DESCRIPTOR;

public class EntityBrowserTreeNode extends Tree.Node<EntityBrowserTreeNode, EntityBrowserValue, VisLabel> {

	private final MessageDispatcher messageDispatcher;
	private final EditorStateProvider editorStateProvider;

	public EntityBrowserTreeNode(MessageDispatcher messageDispatcher, EditorStateProvider editorStateProvider) {
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
	public void setValue(EntityBrowserValue value) {
		super.setValue(value);
		VisLabel actor = new VisLabel(value.label);
		this.setActor(actor);
		actor.addListener(new ClickListener(Input.Buttons.LEFT) {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (value.treeValueType.equals(ENTITY_TYPE_DESCRIPTOR) || value.treeValueType.equals(ENTITY_ASSET_DESCRIPTOR)) {
					messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, value);
				} else {
					messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, null);
				}
			}
		});
		actor.addListener(new ClickListener(Input.Buttons.RIGHT) {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_RIGHT_CLICK,
						new EntityBrowserTreeMessage(value, actor));
			}
		});

		if (editorStateProvider.getState().getExpandedNavigatorNodes().contains(value.label)) {
			super.setExpanded(true);
		}
	}
}
