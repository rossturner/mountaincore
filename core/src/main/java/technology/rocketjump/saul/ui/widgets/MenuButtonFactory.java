package technology.rocketjump.saul.ui.widgets;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.ui.actions.ButtonAction;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class MenuButtonFactory implements DisplaysText {
    private final I18nTranslator translator;
    private final FontRepository fontRepository;
    private final MessageDispatcher messageDispatcher;
    private final SoundAssetDictionary soundAssetDictionary;

    private final SoundAsset onEnterSoundAsset;
    private final SoundAsset onClickSoundAsset;

    private final List<MenuButtonBuilder> buttonBuilders = new ArrayList<>();

    public enum ButtonStyle {
        DEFAULT("default"),
        BTN_BANNER_1("btn_banner_1"),
        BTN_BANNER_2("btn_banner_2"),
        BTN_BANNER_3("btn_banner_3"),
        BTN_BANNER_4("btn_banner_4");

        private final String styleName;

        ButtonStyle(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }


    @Inject
    public MenuButtonFactory(I18nTranslator translator, FontRepository fontRepository,
                             MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
        this.translator = translator;
        this.fontRepository = fontRepository;
        this.messageDispatcher = messageDispatcher;
        this.soundAssetDictionary = soundAssetDictionary;

        this.onEnterSoundAsset = soundAssetDictionary.getByName("MenuHover");
        this.onClickSoundAsset = soundAssetDictionary.getByName("MenuClick");
    }

    public MenuButtonBuilder createButton(String i18nKey, Skin skin, ButtonStyle buttonStyle) {
        MenuButtonBuilder menuButtonBuilder = new MenuButtonBuilder(i18nKey, skin, buttonStyle);
        buttonBuilders.add(menuButtonBuilder);
        return menuButtonBuilder;
    }

    @Override
    public void rebuildUI() {
        for (MenuButtonBuilder builder : buttonBuilders) {
            TextButton textButton = builder.buttonContainer.getActor();
            I18nText translatedString = translator.getTranslatedString(builder.i18nKey);

            if (builder.useHeaderFont) {
                builder.withHeaderFont(builder.fontPointSize);
            } else {
                builder.withDefaultFont(builder.fontPointSize);
            }
            textButton.setText(translatedString.toString());
            builder.scaleFont(textButton.getWidth(), textButton.getLabel());
        }
    }

    public class MenuButtonBuilder {
        private final String i18nKey;
        private final Container<TextButton> buttonContainer;
        private boolean useHeaderFont = false;
        private int fontPointSize = FontRepository.DEFAULT_FONT_SIZE;

        private MenuButtonBuilder(String i18nKey, Skin skin, ButtonStyle buttonStyle) {
            this.i18nKey = i18nKey;
            String text = translator.getTranslatedString(i18nKey).toString();

            TextButton.TextButtonStyle cloned = new TextButton.TextButtonStyle(skin.get(buttonStyle.getStyleName(), TextButton.TextButtonStyle.class));

            TextButton button = new TextButton(text, cloned);
            buttonContainer = new Container<>(button);
            buttonContainer.setTransform(true);
            buttonContainer.setOrigin(button.getPrefWidth() / 2, button.getPrefHeight() / 2);

            button.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    super.enter(event, x, y, pointer, fromActor);
                    messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onEnterSoundAsset));
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onClickSoundAsset));
                }
            });
        }


        public MenuButtonBuilder withEssentialWidth(float width) {
            Label currentLabel = buttonContainer.getActor().getLabel();
            buttonContainer.setWidth(width);
            buttonContainer.getActor().setWidth(width);
            scaleFont(width, currentLabel);
            return this;
        }

        private void scaleFont(float width, Label currentLabel) {
            currentLabel.setFontScale(1);
            currentLabel.layout();
            float glyphWidth = currentLabel.getGlyphLayout().width;

            float targetWidth = width * 0.92f;

            if (glyphWidth >= targetWidth) {
                float increase = (glyphWidth - targetWidth) / glyphWidth;
                currentLabel.setFontScale(1 - increase);
            }
        }

        public MenuButtonBuilder withHeaderFont(int fontPointSize) {
            this.useHeaderFont = true;
            this.fontPointSize = fontPointSize;
            TextButton button = buttonContainer.getActor();
            TextButton.TextButtonStyle style = button.getStyle();
            style.font = fontRepository.getHeaderFont(fontPointSize).getBitmapFont();
            button.setStyle(style);
            return this;
        }

        public MenuButtonBuilder withDefaultFont(int fontPointSize) {
            this.useHeaderFont = false;
            this.fontPointSize = fontPointSize;
            TextButton button = buttonContainer.getActor();
            TextButton.TextButtonStyle style = button.getStyle();
            style.font = fontRepository.getDefaultFont(fontPointSize).getBitmapFont();
            button.setStyle(style);
            return this;
        }

        public MenuButtonBuilder withScaleUpOnHoverBy(float scaleBy) {
            buttonContainer.getActor().addListener(new EnlargeOnHoverListener(buttonContainer, scaleBy));
            return this;
        }

        public MenuButtonBuilder withAction(ButtonAction action) {
            buttonContainer.getActor().addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    action.onClick();
                }
            });
            return this;
        }

        public MenuButtonBuilder withScaleBy(float scaleBy) {
            buttonContainer.scaleBy(scaleBy);
            return this;
        }

        public Container<TextButton> build() {
            return buttonContainer;
        }

    }
}
