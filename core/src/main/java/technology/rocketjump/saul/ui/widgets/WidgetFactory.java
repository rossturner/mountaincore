package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.screens.menus.PrivacyOptInMenu;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
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

    @Inject
    public WidgetFactory(MessageDispatcher messageDispatcher, I18nRepo i18nRepo, UserPreferences userPreferences,
                         TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository,
                         OnDemandFontRepository onDemandFontRepository, GuiSkinRepository guiSkinRepository) {
        this.messageDispatcher = messageDispatcher;
        this.i18nRepo = i18nRepo;
        this.userPreferences = userPreferences;
        this.textureAtlasRepository = textureAtlasRepository;
        this.fontRepository = fontRepository;
        this.onDemandFontRepository = onDemandFontRepository;
        this.guiSkinRepository = guiSkinRepository;
    }

    public LanguageList createLanguageList(Skin skin) {
        return new LanguageList(messageDispatcher, i18nRepo, userPreferences, skin, textureAtlasRepository, fontRepository);
    }

    public CustomSelect<LanguageType> createLanguageSelectBox(Skin skin) {
        LanguageList languageList = createLanguageList(skin);
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(skin.get(SelectBox.SelectBoxStyle.class));
        selectBoxStyle.font = fontRepository.getUnicodeFont().getBitmapFont();
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
}
