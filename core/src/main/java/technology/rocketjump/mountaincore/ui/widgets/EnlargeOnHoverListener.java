package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class EnlargeOnHoverListener extends ClickListener {
    private final Container<TextButton> container;
    private float originalScaleX;
    private float originalScaleY;
    private final float scaleUpBy;

    public EnlargeOnHoverListener(Container<TextButton> container, float scaleUpBy) {
        this.container = container;
        this.scaleUpBy = scaleUpBy;
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        if (!isOver()) {
            this.originalScaleX = container.getScaleX();
            this.originalScaleY = container.getScaleY();
            container.clearActions();
            container.addAction(Actions.scaleBy(scaleUpBy, scaleUpBy));
        }
        super.enter(event, x, y, pointer, fromActor);
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        if (isOver()) {
            container.clearActions();
            container.addAction(Actions.scaleTo(originalScaleX, originalScaleY));
        }
        super.exit(event, x, y, pointer, toActor);
    }
}
