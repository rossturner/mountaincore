package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.messaging.MessageType;

import java.util.function.Consumer;

public abstract class AbstractAttributesPane extends VisTable {
    protected final EditorStateProvider editorStateProvider;
    protected final MessageDispatcher messageDispatcher;
    private int colCount = 0;

    public AbstractAttributesPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.messageDispatcher = messageDispatcher;
    }

    public abstract void reload();

    @Override
    public <T extends Actor> Cell<T> add(T actor) {
        if (colCount == 3) {
            row();
        }
        colCount++;
        return super.add(actor).left().padRight(10);
    }

    @Override
    public Cell row() {
        colCount = 0;
        return super.row();
    }

    protected <T> Consumer<T> update(Consumer<T> input) {
        Consumer<T> consumer = x -> {
            messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, editorStateProvider.getState().getCurrentEntity());
            editorStateProvider.stateChanged();
        };

        return input.andThen(consumer);
    }
}
