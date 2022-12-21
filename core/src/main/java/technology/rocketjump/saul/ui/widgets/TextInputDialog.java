package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.i18n.I18nText;

import java.util.function.Consumer;

public class TextInputDialog extends GameDialog {

	public final TextField inputBox;

	public TextInputDialog(I18nText titleText, String inputPlaceholder,
						   I18nText buttonText, Skin uiSkin, Consumer<String> onButtonClick,
						   MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
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
		inputBox.addListener(new ChangeCursorOnHover(inputBox, GameCursor.I_BEAM, messageDispatcher));
		inputBox.setAlignment(Align.center);
		contentTable.add(inputBox).width(910).height(96).center().row();
		withButton(buttonText, (Runnable) () -> {
			String text = inputBox.getText();
			if (StringUtils.isEmpty(text)) {
				text = inputPlaceholder;
			}
			onButtonClick.accept(text);
		});
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

	public void setMaxLength(int maxLength) {
		inputBox.setMaxLength(maxLength);
	}

}
