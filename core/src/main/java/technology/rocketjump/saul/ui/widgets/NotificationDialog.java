package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.i18n.I18nText;

public class NotificationDialog extends GameDialog {

	private Texture texture;
	private Image image;

	public NotificationDialog(I18nText titleText, Skin uiSkin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		super(titleText, uiSkin, messageDispatcher, soundAssetDictionary);
	}

	@Override
	public void dispose() {
		if (texture != null) {
			texture.dispose();
		}
	}

	public void addTexture(Texture texture) {
		this.texture = texture;
		this.image = new Image(texture);
		contentTable.add(image).pad(8).row();
	}
}
