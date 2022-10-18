package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.fonts.GameFont;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class IconButtonFactory implements DisplaysText {

	private final I18nTranslator translator;
	private final TextureAtlas textureAtlas;
	private final MessageDispatcher messageDispatcher;
	private final NinePatch buttonNinePatch;
	private final GameFont defaultFont;

	private final Color backgroundColor = HexColors.get("#545E61");

	private final Set<IconButton> allInstances = new HashSet<>();

	private final Set<IconOnlyButton> iconOnlyButtons = new HashSet<>();
	private final SoundAsset onEnterSoundAsset;
	private final SoundAsset onClickSoundAsset;
	private final FontRepository fontRepository;

	@Inject
	public IconButtonFactory(I18nTranslator translator, FontRepository fontRepository, SoundAssetDictionary soundAssetDictionary,
							 TextureAtlasRepository textureAtlasRepository, MessageDispatcher messageDispatcher) {
		this.fontRepository = fontRepository;
		this.translator = translator;
		this.textureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		this.messageDispatcher = messageDispatcher;

		this.buttonNinePatch = textureAtlas.createPatch("button");
		this.defaultFont = fontRepository.getDefaultFontForUI();

		this.onEnterSoundAsset = soundAssetDictionary.getByName("MenuHover");
		this.onClickSoundAsset = soundAssetDictionary.getByName("MenuClick");
	}

	public IconButton create(String i18nLabelKey, String iconName, Color buttonColor, ButtonStyle style) {
		IconButton iconButton = new IconButton(defaultFont, i18nLabelKey, style);
		if (i18nLabelKey != null) {
			iconButton.setLabelText(translator.getTranslatedString(i18nLabelKey), messageDispatcher);
		}
		iconButton.setBackgroundColor(backgroundColor);
		iconButton.setForegroundColor(buttonColor);
		if (iconName != null) {
			iconButton.setIconSprite(this.textureAtlas.createSprite(iconName));
		}
		iconButton.setButtonNinepatch(buttonNinePatch);
		iconButton.setOnEnter(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onEnterSoundAsset));
		});
		iconButton.setOnClickSoundAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onClickSoundAsset));
		});

		allInstances.add(iconButton);
		return iconButton;
	}

	public IconOnlyButton create(String iconName) {
		IconOnlyButton iconOnlyButton = new IconOnlyButton(null);
		iconOnlyButton.setIconSprite(this.textureAtlas.createSprite(iconName));
		return iconOnlyButton;
	}

	public void remove(IconButton iconButton) {
		allInstances.remove(iconButton);
	}

	@Override
	public void rebuildUI() {
		for (IconButton button : allInstances) {
			if (button.getI18nKey() != null) {
				button.setFont(fontRepository.getDefaultFontForUI());
				button.setLabelText(translator.getTranslatedString(button.getI18nKey()), messageDispatcher);
			}
		}
	}

}
