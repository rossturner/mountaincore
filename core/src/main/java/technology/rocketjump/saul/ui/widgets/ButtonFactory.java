package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ButtonFactory {

	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;
	private final TooltipFactory tooltipFactory;
	private final MenuSkin menuSkin;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;

	@Inject
	public ButtonFactory(MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository) {
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;
		this.tooltipFactory = tooltipFactory;
		this.menuSkin = skinRepository.getMenuSkin();
		this.mainGameSkin = skinRepository.getMainGameSkin();
		this.managementSkin = skinRepository.getManagementSkin();
	}

	/**
	 * @return ImageButton of the supplied drawable, and when checked gives thick outline
	 */
	public ImageButton checkableButton(Drawable drawable, boolean thickOutline) {
		final String styleName = thickOutline ? "thick_outline" : "default";
		ImageButton.ImageButtonStyle clonedStyle = new ImageButton.ImageButtonStyle(menuSkin.get(styleName, ImageButton.ImageButtonStyle.class));
		clonedStyle.imageUp = drawable;
		ImageButton button = new ImageButton(clonedStyle);
		attachClickCursor(button, GameCursor.SELECT);
		return button;
	}


	public Button buildDrawableButton(String drawableName, String tooltipI18nKey, Runnable onClick) {
		Button button = new Button(getDrawable(drawableName));
		attachClickCursor(button, GameCursor.SELECT);
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				onClick.run();
			}
		});
		tooltipFactory.simpleTooltip(button, tooltipI18nKey, TooltipLocationHint.ABOVE);
		return button;
	}

	private Drawable getDrawable(String drawableName) {

		try {
			return mainGameSkin.getDrawable(drawableName);
		} catch (GdxRuntimeException ignored) {

		}
		return managementSkin.getDrawable(drawableName);
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
