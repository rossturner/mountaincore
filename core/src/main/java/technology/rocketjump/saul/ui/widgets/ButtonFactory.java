package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ButtonFactory {

	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;
	private final MenuSkin menuSkin;

	@Inject
	public ButtonFactory(MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary, GuiSkinRepository skinRepository) {
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;
		this.menuSkin = skinRepository.getMenuSkin();
	}

	/**
	 * @return ImageButton of the supplied drawable, and when checked gives thick outline
	 */
	public ImageButton checkableButton(Drawable drawable) {
		ImageButton.ImageButtonStyle clonedStyle = new ImageButton.ImageButtonStyle(menuSkin.get("default", ImageButton.ImageButtonStyle.class));
		clonedStyle.imageUp = drawable;
		ImageButton button = new ImageButton(clonedStyle);
		attachClickCursor(button, GameCursor.SELECT);
		return button;
	}

	public void attachClickCursor(Actor button, GameCursor cursor) {
		button.addListener(new ChangeCursorOnHover(button, cursor, messageDispatcher));
		button.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
	}

	public void enable(Actor button) {
		button.getColor().a = 1.0f;
		button.setTouchable(Touchable.enabled);
	}

	public void disable(Actor button) {
		button.getColor().a = 0.5f;
		button.setTouchable(Touchable.disabled);
	}
}
