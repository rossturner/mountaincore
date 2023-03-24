package technology.rocketjump.mountaincore.assets.editor.widgets;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.function.Function;

public class TextBoxConventionListener extends InputListener {
    private final Function<String, String> convention;

    public TextBoxConventionListener(Function<String, String> convention) {
        this.convention = convention;
    }

    @Override
    public boolean keyTyped(InputEvent event, char character) {
        if (event.getListenerActor() instanceof VisTextField vtf) {
            String originalText = vtf.getText();
            vtf.setText(convention.apply(originalText));
            vtf.setCursorAtTextEnd();
        }
        return true;
    }
}
