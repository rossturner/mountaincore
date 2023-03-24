package technology.rocketjump.mountaincore.ui.widgets;

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
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.military.SquadFormationDictionary;
import technology.rocketjump.mountaincore.military.model.formations.SquadFormation;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.fonts.OnDemandFontRepository;
import technology.rocketjump.mountaincore.ui.i18n.I18nRepo;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.LanguageType;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.skins.ManagementSkin;

import java.util.Comparator;
import java.util.function.Consumer;

@Singleton
public class WidgetFactory {
    private final MessageDispatcher messageDispatcher;
    private final I18nRepo i18nRepo;
    private final UserPreferences userPreferences;
    private final TextureAtlasRepository textureAtlasRepository;
    private final OnDemandFontRepository onDemandFontRepository;
    private final I18nTranslator i18nTranslator;
    private final SoundAssetDictionary soundAssetDictionary;
    private final SquadFormationDictionary squadFormationDictionary;
    private final ManagementSkin managementSkin;

    @Inject
    public WidgetFactory(MessageDispatcher messageDispatcher, I18nRepo i18nRepo, UserPreferences userPreferences,
                         TextureAtlasRepository textureAtlasRepository,
                         OnDemandFontRepository onDemandFontRepository, GuiSkinRepository guiSkinRepository,
                         I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary, SquadFormationDictionary squadFormationDictionary) {
        this.messageDispatcher = messageDispatcher;
        this.i18nRepo = i18nRepo;
        this.userPreferences = userPreferences;
        this.textureAtlasRepository = textureAtlasRepository;
        this.onDemandFontRepository = onDemandFontRepository;
        this.i18nTranslator = i18nTranslator;
        this.soundAssetDictionary = soundAssetDictionary;
        this.squadFormationDictionary = squadFormationDictionary;
        this.managementSkin = guiSkinRepository.getManagementSkin();
    }

    public CustomSelect<LanguageType> createLanguageSelectBox(Skin skin) {
        BitmapFont guaranteedBoldFont = onDemandFontRepository.getGuaranteedBoldFont(18);
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
                messageDispatcher.dispatchMessage(MessageType.LANGUAGE_CHANGED);
            }
        });

        addClickCursor(selectBox);
        return selectBox;
    }

    public CustomSelect<SquadFormation> createSquadFormationSelectBox(Skin skin, SquadFormation initial, Consumer<SquadFormation> listener) {
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(skin.get("select_narrow", SelectBox.SelectBoxStyle.class));

        var formationList = new List<SquadFormation>(selectBoxStyle.listStyle) {
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



        CustomSelect<SquadFormation> selectBox = new CustomSelect<>(selectBoxStyle, formationList, new CustomSelect.DrawItemProcedure<SquadFormation>() {
            @Override
            public GlyphLayout drawItem(Batch batch, BitmapFont font, SquadFormation item, float x, float y, float width) {
                return formationList.drawItem(batch, font, 0, item, x, y, width);
            }
        });
        selectBox.setAlignment(Align.center);
        Array<SquadFormation> items = WidgetBuilder.orderedArray(squadFormationDictionary.getAll());
        items.sort(Comparator.comparing(item -> item.getDescription(i18nTranslator, null, messageDispatcher).get(0).toString()));
        selectBox.setItems(items);
        selectBox.setSelected(initial);
        selectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                listener.accept(selectBox.getSelected());
            }
        });


        formationList.setAlignment(Align.center);

        addClickCursor(selectBox);
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

        addClickCursor(toggle);
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

        addClickCursor(checkbox);

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

        addClickCursor(checkbox);

        return checkbox;
    }


    private void addClickCursor(Actor selectBox) {
        selectBox.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
        selectBox.addListener(new ChangeCursorOnHover(selectBox, GameCursor.SELECT, messageDispatcher));
    }
}
