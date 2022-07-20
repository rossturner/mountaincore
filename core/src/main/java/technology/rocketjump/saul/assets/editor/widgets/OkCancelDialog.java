package technology.rocketjump.saul.assets.editor.widgets;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import java.util.ArrayList;
import java.util.List;

public abstract class OkCancelDialog {
    private final VisDialog visDialog;
    private final List<VisValidatableTextField> validatedFields = new ArrayList<>();

    public OkCancelDialog(String title) {
        this.visDialog = new VisDialog(title) {
            @Override
            protected void result(Object result) {
                if (result instanceof Boolean isOk) {
                    if (isOk) {
                        onOk();
                    }
                }
            }
        };

        VisTextButton okButton = new VisTextButton("Ok") {
            @Override
            public Touchable getTouchable() {
                if (isFormValid()) {
                    setDisabled(false);
                    return Touchable.enabled;
                } else {
                    setDisabled(true);
                    return Touchable.disabled;
                }
            }
        };

        visDialog.button(okButton, true);
        visDialog.button("Cancel", false);
        visDialog.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER && isFormValid()) {
                    onOk(); //bit ugly that can't call result from outside VisDialog
                    visDialog.hide();
                }
                return false;
            }
        });
        visDialog.key(Input.Keys.ESCAPE, false);
    }

    public <T extends Actor> Cell<T> add(T actor) {
        //TODO: maybe recursive
        if (actor instanceof Table table) {
            table.getChildren().forEach(child -> {
                if (child instanceof VisValidatableTextField f) {
                    validatedFields.add(f);
                }
            });
        }
        if (actor instanceof VisValidatableTextField f) {
            validatedFields.add(f);
        }
        return visDialog.getContentTable().add(actor);
    }

    public void row() {
        visDialog.getContentTable().row();
    }

    public abstract void onOk();

    public void show(Stage stage) {
        visDialog.show(stage);
        if (!validatedFields.isEmpty()) {
            stage.setKeyboardFocus(validatedFields.get(0));
        }
    }

    private boolean isFormValid() {
        boolean formValid = true;
        for (VisValidatableTextField validatedField : validatedFields) {
            formValid &= validatedField.isInputValid();
        }
        return formValid;
    }

}
