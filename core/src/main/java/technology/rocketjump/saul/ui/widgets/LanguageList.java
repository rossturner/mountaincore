package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.LanguageType;


public class LanguageList extends List<LanguageType> {
    private int alignment = Align.left;


    public LanguageList(MessageDispatcher messageDispatcher, I18nRepo i18nRepo, UserPreferences userPreferences, Skin skin, TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository) {
        super(new ListStyle(skin.get(ListStyle.class))); //clone the style for future mutation
        getStyle().font = fontRepository.getUnicodeFont().getBitmapFont();

        i18nRepo.init(textureAtlasRepository);
        String languageCode = userPreferences.getPreference(UserPreferences.PreferenceKey.LANGUAGE, "en-gb");
        java.util.List<LanguageType> allLanguages = i18nRepo.getAllLanguages();
        this.setItems(WidgetBuilder.orderedArray(allLanguages));

        LanguageType selectedLanguage = null;
        for (LanguageType languageType : allLanguages) {
            if (languageType.getCode().equals(languageCode)) {
                selectedLanguage = languageType;
                break;
            }
        }
        if (selectedLanguage == null) {
            selectedLanguage = allLanguages.get(0);
        }

        setAlignment(Align.center);
        setSelected(selectedLanguage);
    }

    @Override
    protected GlyphLayout drawItem (Batch batch, BitmapFont font, int index, LanguageType item, float x, float y, float width) {
        String string = toString(item);
        Sprite iconSprite = item.getIconSprite();

        float itemHeight = getItemHeight();
        batch.draw(iconSprite, x + 3, y - 18, itemHeight, itemHeight);
        return font.draw(batch, string, x + itemHeight + 5, y, 0, string.length(), width - itemHeight - 5, alignment, false, "...");
    }

    @Override
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }
}
