package technology.rocketjump.saul.assets.editor.components.entitybrowser;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;

public class EntityBrowserTreeNode extends Tree.Node<EntityBrowserTreeNode, EntityBrowserValue, VisLabel> {

	private final MessageDispatcher messageDispatcher;
	private final EditorStateProvider editorStateProvider;

	public EntityBrowserTreeNode(MessageDispatcher messageDispatcher, EditorStateProvider editorStateProvider) {
		this.messageDispatcher = messageDispatcher;
		this.editorStateProvider = editorStateProvider;
	}

	@Override
	public void setValue(EntityBrowserValue value) {
		super.setValue(value);
		VisLabel actor = new VisLabel(value.label);
		this.setActor(actor);
		actor.addListener(new ClickListener(Input.Buttons.LEFT) {
			@Override
			public void clicked(InputEvent event, float x, float y) {
			}
		});
		actor.addListener(new ClickListener(Input.Buttons.RIGHT) {
			@Override
			public void clicked(InputEvent event, float x, float y) {
			}
		});
	}
}
