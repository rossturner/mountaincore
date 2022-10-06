package technology.rocketjump.saul.ui.widgets;


import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.ui.actions.ButtonAction;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

@Singleton
public class MenuButtonFactory {
    private final I18nTranslator translator;
    private final FontRepository fontRepository;

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
    public MenuButtonFactory(I18nTranslator translator, FontRepository fontRepository) {
        this.translator = translator;
        this.fontRepository = fontRepository;
    }

    public MenuButtonBuilder createButton(String i18nKey, Skin skin, ButtonStyle buttonStyle) {
        return new MenuButtonBuilder(i18nKey, skin, buttonStyle);
    }

    public class MenuButtonBuilder {
        private final Container<TextButton> buttonContainer;

        private MenuButtonBuilder(String i18nKey, Skin skin, ButtonStyle buttonStyle) {
            String text = translator.getTranslatedString(i18nKey).toString();

            TextButton.TextButtonStyle cloned = new TextButton.TextButtonStyle(skin.get(buttonStyle.getStyleName(), TextButton.TextButtonStyle.class));

            TextButton button = new TextButton(text, cloned);
            buttonContainer = new Container<>(button);
            buttonContainer.setTransform(true);
            buttonContainer.setOrigin(button.getPrefWidth() / 2, button.getPrefHeight() / 2);
        }

        public MenuButtonBuilder withHeaderFont(int fontPointSize) {
            TextButton button = buttonContainer.getActor();
            TextButton.TextButtonStyle style = button.getStyle();
            style.font = fontRepository.getHeaderFont(fontPointSize).getBitmapFont();
            button.setStyle(style);
            return this;
        }

        public MenuButtonBuilder withDefaultFont(int fontPointSize) {
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
