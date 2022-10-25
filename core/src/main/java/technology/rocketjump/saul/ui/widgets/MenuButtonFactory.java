package technology.rocketjump.saul.ui.widgets;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.ui.actions.ButtonAction;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

@Singleton
public class MenuButtonFactory {
    private final I18nTranslator translator;
    private final FontRepository fontRepository;
    private final MessageDispatcher messageDispatcher;

    private final SoundAsset onEnterSoundAsset;
    private final SoundAsset onClickSoundAsset;

    public enum ButtonStyle {
        BTN_SCALABLE_50PT("btn_scalable_header-font-50"),
        BTN_BANNER_1_47PT("btn_banner_1_header-font-47"),
        BTN_BANNER_2_47PT("btn_banner_2_header-font-47"),
        BTN_BANNER_3_36PT("btn_banner_3_header-font-36"),
        BTN_BANNER_3_47PT("btn_banner_3_header-font-47"),
        BTN_BANNER_4_36PT("btn_banner_4_header-font-36"),
        BTN_BANNER_4_47PT("btn_banner_4_header-font-47");

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

        this.onEnterSoundAsset = soundAssetDictionary.getByName("MenuHover");
        this.onClickSoundAsset = soundAssetDictionary.getByName("MenuClick");
    }

    public MenuButtonBuilder createButton(String i18nKey, Skin skin, ButtonStyle buttonStyle) {
        return new MenuButtonBuilder(i18nKey, skin, buttonStyle);
    }

    public class MenuButtonBuilder {
        private final Container<TextButton> buttonContainer;

        private MenuButtonBuilder(String i18nKey, Skin skin, ButtonStyle buttonStyle) {
            String text = translator.getTranslatedString(i18nKey).toString();

            TextButton button = new TextButton(text, skin.get(buttonStyle.getStyleName(), TextButton.TextButtonStyle.class));
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
            button.addListener(new ChangeCursorOnHover(GameCursor.SELECT, messageDispatcher));
        }


        public MenuButtonBuilder withScaledToFitLabel(float width) {
            Label currentLabel = buttonContainer.getActor().getLabel();
            ScaledToFitLabel label = new ScaledToFitLabel(currentLabel.getText(), currentLabel.getStyle(), width);
            label.setAlignment(Align.center);
            buttonContainer.getActor().setLabel(label);
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

        public Container<TextButton> build() {
            return buttonContainer;
        }
    }
}
