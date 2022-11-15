package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.screens.menus.PrivacyOptInMenu;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.LanguageType;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

@Singleton
public class WidgetFactory {
    private final MessageDispatcher messageDispatcher;
    private final I18nRepo i18nRepo;
    private final UserPreferences userPreferences;
    private final TextureAtlasRepository textureAtlasRepository;
    private final FontRepository fontRepository;
    private final OnDemandFontRepository onDemandFontRepository;
    private final GuiSkinRepository guiSkinRepository;
    private final I18nTranslator i18nTranslator;
    private final SoundAssetDictionary soundAssetDictionary;

    @Inject
    public WidgetFactory(MessageDispatcher messageDispatcher, I18nRepo i18nRepo, UserPreferences userPreferences,
                         TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository,
                         OnDemandFontRepository onDemandFontRepository, GuiSkinRepository guiSkinRepository,
                         I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary) {
        this.messageDispatcher = messageDispatcher;
        this.i18nRepo = i18nRepo;
        this.userPreferences = userPreferences;
        this.textureAtlasRepository = textureAtlasRepository;
        this.fontRepository = fontRepository;
        this.onDemandFontRepository = onDemandFontRepository;
        this.guiSkinRepository = guiSkinRepository;
        this.i18nTranslator = i18nTranslator;
        this.soundAssetDictionary = soundAssetDictionary;
    }

    public CustomSelect<LanguageType> createLanguageSelectBox(Skin skin) {
        LanguageList languageList = new LanguageList(i18nRepo, userPreferences, skin, textureAtlasRepository, onDemandFontRepository.getGuaranteedUnicodeFont());

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(skin.get(SelectBox.SelectBoxStyle.class));
        selectBoxStyle.font = onDemandFontRepository.getGuaranteedUnicodeFont();
        CustomSelect<LanguageType> selectBox = new CustomSelect<>(selectBoxStyle, languageList, new CustomSelect.DrawItemProcedure<LanguageType>() {
            @Override
            public GlyphLayout drawItem(Batch batch, BitmapFont font, LanguageType item, float x, float y, float width) {
                return languageList.drawItem(batch, font, 0, item, x, y, width);
            }
        });
        selectBox.setAlignment(Align.center);
        selectBox.setSelected(languageList.getSelected());
        selectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                LanguageType selectedLanguage = selectBox.getSelected();
                PrivacyOptInMenu.changeLanguage(selectedLanguage, userPreferences, i18nRepo, messageDispatcher, fontRepository);
//                parent.reset();
            }
        });

        return selectBox;
    }

    public CheckBox createLeftLabelledCheckbox(String i18nKey, Skin skin, float labelMaxWidth) {
        CheckBox checkbox = new CheckBox("", skin);
        Label realLabel = new ScaledToFitLabel(i18nTranslator.getTranslatedString(i18nKey).toString(), skin, "checkbox_label", labelMaxWidth);
        realLabel.setAlignment(Align.center);
        checkbox.getLabel().setStyle(skin.get("checkbox_label", Label.LabelStyle.class));
        Image image = checkbox.getImage();

        checkbox.clearChildren();
        checkbox.add(realLabel).growX().padRight(28f);
        checkbox.add(image);

        checkbox.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
        checkbox.addListener(new ChangeCursorOnHover(checkbox, GameCursor.SELECT, messageDispatcher));

        return checkbox;
    }

    public CheckBox createLeftLabelledCheckboxNoBackground(String i18nKey, Skin skin, float labelMaxWidth) {
        CheckBox checkbox = new CheckBox("", skin, "checkbox_no_bg");
        Label realLabel = new Label(i18nTranslator.getTranslatedString(i18nKey).toString(), skin, "checkbox_label_no_bg");
        realLabel.setWrap(true);
        realLabel.setAlignment(Align.center);
        checkbox.getLabel().setStyle(skin.get("checkbox_label_no_bg", Label.LabelStyle.class));
        Image image = checkbox.getImage();

        checkbox.clearChildren();
        checkbox.add(realLabel).width(labelMaxWidth).padRight(28f);
        checkbox.add(image);

        checkbox.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
        checkbox.addListener(new ChangeCursorOnHover(checkbox, GameCursor.SELECT, messageDispatcher));

        return checkbox;
    }
}
