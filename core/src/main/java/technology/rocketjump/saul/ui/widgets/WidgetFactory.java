package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.military.SquadFormationDictionary;
import technology.rocketjump.saul.military.model.formations.SquadFormation;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.LanguageType;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.ManagementSkin;

import java.util.Comparator;
import java.util.function.Consumer;

@Singleton
public class WidgetFactory {
    private final MessageDispatcher messageDispatcher;
    private final I18nRepo i18nRepo;
    private final UserPreferences userPreferences;
    private final TextureAtlasRepository textureAtlasRepository;
    private final FontRepository fontRepository;
    private final OnDemandFontRepository onDemandFontRepository;
    private final I18nTranslator i18nTranslator;
    private final SoundAssetDictionary soundAssetDictionary;
    private final SquadFormationDictionary squadFormationDictionary;
    private final ManagementSkin managementSkin;

    @Inject
    public WidgetFactory(MessageDispatcher messageDispatcher, I18nRepo i18nRepo, UserPreferences userPreferences,
                         TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository,
                         OnDemandFontRepository onDemandFontRepository, GuiSkinRepository guiSkinRepository,
                         I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary, SquadFormationDictionary squadFormationDictionary) {
        this.messageDispatcher = messageDispatcher;
        this.i18nRepo = i18nRepo;
        this.userPreferences = userPreferences;
        this.textureAtlasRepository = textureAtlasRepository;
        this.fontRepository = fontRepository;
        this.onDemandFontRepository = onDemandFontRepository;
        this.i18nTranslator = i18nTranslator;
        this.soundAssetDictionary = soundAssetDictionary;
        this.squadFormationDictionary = squadFormationDictionary;
        this.managementSkin = guiSkinRepository.getManagementSkin();
    }

    public CustomSelect<LanguageType> createLanguageSelectBox(Skin skin) {
        BitmapFont guaranteedBoldFont = onDemandFontRepository.getGuaranteedBoldFont(18 * 2);
        LanguageList languageList = new LanguageList(i18nRepo, userPreferences, skin, textureAtlasRepository, guaranteedBoldFont);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(skin.get(SelectBox.SelectBoxStyle.class));
        selectBoxStyle.font = guaranteedBoldFont;
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
                userPreferences.setPreference(UserPreferences.PreferenceKey.LANGUAGE, selectedLanguage.getCode());
                i18nRepo.setCurrentLanguage(selectedLanguage);
                fontRepository.changeFonts(selectedLanguage);
                messageDispatcher.dispatchMessage(MessageType.LANGUAGE_CHANGED);
            }
        });

        return selectBox;
    }

    public CustomSelect<SquadFormation> createSquadFormationSelectBox(Skin skin, SquadFormation initial, Consumer<SquadFormation> listener) {
        var formationList = new List<SquadFormation>(skin) {
            @Override
            public GlyphLayout drawItem(Batch batch, BitmapFont font, int index, SquadFormation item, float x, float y, float width) {
                String string = item.getDescription(i18nTranslator, null, messageDispatcher).get(0).toString();
                Drawable iconDrawable = managementSkin.getDrawable(item.getDrawableIconName());
//                Drawable selection = getStyle().selection;
//                float middleHeight = selection.getMinHeight() - selection.getBottomHeight() - selection.getTopHeight();
//                float remainder = Math.max(middleHeight - iconDrawable.getMinHeight(), 0);
//                float yOffset = font.getLineHeight() - (remainder / 2f); //TODO: figure this out properly
                iconDrawable.draw(batch, x, y - 31, iconDrawable.getMinWidth(), iconDrawable.getMinHeight());
                return font.draw(batch, string, x, y, 0, string.length(), width, getAlignment(), false, "...");
            }
        };
        Array<SquadFormation> items = WidgetBuilder.orderedArray(squadFormationDictionary.getAll());
        items.sort(Comparator.comparing(item -> item.getDescription(i18nTranslator, null, messageDispatcher).get(0).toString()));
        formationList.setItems(items);

        formationList.setAlignment(Align.center);

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(skin.get(SelectBox.SelectBoxStyle.class));
        CustomSelect<SquadFormation> selectBox = new CustomSelect<>(selectBoxStyle, formationList, new CustomSelect.DrawItemProcedure<SquadFormation>() {
            @Override
            public GlyphLayout drawItem(Batch batch, BitmapFont font, SquadFormation item, float x, float y, float width) {
                return formationList.drawItem(batch, font, 0, item, x, y, width);
            }
        });
        selectBox.setAlignment(Align.center);
        selectBox.setSelected(initial);
        selectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                listener.accept(selectBox.getSelected());
            }
        });


        return selectBox;
    }

    public ImageTextButton createLeftLabelledToggle(String i18nKey, Skin skin, Image prefixImage) {
        ImageTextButton toggle = new ImageTextButton(i18nTranslator.translate(i18nKey), skin, "text_toggle");
        Label label = toggle.getLabel();
        Image toggleImage = toggle.getImage();
        toggle.clearChildren();
        if (prefixImage != null) {
            toggle.add(prefixImage).padRight(9f);
        }
        toggle.add(label).padRight(9f);
        toggle.add(toggleImage);
        toggle.addActorBefore(toggle.getImage(), label);

        toggle.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
        toggle.addListener(new ChangeCursorOnHover(toggle, GameCursor.SELECT, messageDispatcher));
        return toggle;
    }

    public CheckBox createLeftLabelledCheckbox(String i18nKey, Skin skin, float labelMaxWidth) {
        CheckBox checkbox = new CheckBox("", skin);
        Label realLabel = new ScaledToFitLabel(i18nTranslator.translate(i18nKey), skin, "checkbox_label", labelMaxWidth);
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
        Label realLabel = new Label(i18nTranslator.translate(i18nKey), skin, "checkbox_label_no_bg");
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
