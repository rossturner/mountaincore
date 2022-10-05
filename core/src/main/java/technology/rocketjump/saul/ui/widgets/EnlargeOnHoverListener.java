package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class EnlargeOnHoverListener extends ClickListener {
    private final Container<TextButton> container;
    private final float originalWidth;
    private final float originalHeight;
    private final float originalScaleX;
    private final float originalScaleY;
    private final float scaleUpBy;

    public EnlargeOnHoverListener(Container<TextButton> container, float scaleUpBy) {
        this.container = container;
        this.originalWidth = container.getPrefWidth();
        this.originalHeight = container.getPrefHeight();
        this.originalScaleX = container.getScaleX();
        this.originalScaleY = container.getScaleY();
        this.scaleUpBy = scaleUpBy;
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        if (!isOver()) {
            container.clearActions();
            container.addAction(Actions.sequence(Actions.sizeTo(originalWidth, originalHeight), Actions.scaleTo(originalScaleX, originalScaleY), Actions.scaleBy(scaleUpBy, scaleUpBy, 0.3f)));
        }
        super.enter(event, x, y, pointer, fromActor);
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        if (isOver()) {
            container.clearActions();
            container.addAction(Actions.sequence(Actions.sizeTo(originalWidth, originalHeight), Actions.scaleTo(originalScaleX, originalScaleY, 0.2f)));
        }
        super.exit(event, x, y, pointer, toActor);
    }
}
