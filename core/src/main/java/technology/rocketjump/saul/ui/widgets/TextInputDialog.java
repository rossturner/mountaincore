package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.i18n.I18nText;

import java.util.function.Consumer;

public class TextInputDialog extends GameDialog {

	public final TextField inputBox;

	public TextInputDialog(I18nText titleText, String inputPlaceholder,
						   I18nText buttonText, Skin uiSkin, Consumer<String> onButtonClick,
						   MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary, String textButtonStyleName) {
		super(titleText, uiSkin, messageDispatcher, soundAssetDictionary);

		TextField.TextFieldStyle textFieldStyle = uiSkin.get("input-dialog-text", TextField.TextFieldStyle.class);
		inputBox = new TextField(inputPlaceholder, textFieldStyle) {
			@Override
			protected InputListener createInputListener () {
				return new TextFieldClickListener(){
					@Override
					public boolean keyUp(InputEvent event, int keycode) {
						if (keycode == Input.Keys.ENTER) {
							((Button)dialog.getButtonTable().getCells().get(0).getActor()).getClickListener().clicked(event, 0, 0);
							return true;
						} else {
							return super.keyUp(event, keycode);
						}
					};
				};
			}
		};
		inputBox.setAlignment(Align.center);
		contentTable.add(inputBox).width(910).height(96).center().row();
		withButton(buttonText, (Runnable) () -> {
			onButtonClick.accept(inputBox.getText());
		}, uiSkin.get(textButtonStyleName, TextButton.TextButtonStyle.class));
	}

	@Override
	public void show(Stage stage) {
		super.show(stage);
		stage.setKeyboardFocus(inputBox);
		inputBox.selectAll();
	}


	@Override
	public void dispose() {
		
	}

}
