package technology.rocketjump.saul.ui.widgets;

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
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.LanguageType;


public class LanguageList extends List<LanguageType> {
    private int alignment = Align.left;


    public LanguageList(I18nRepo i18nRepo, UserPreferences userPreferences, Skin skin, TextureAtlasRepository textureAtlasRepository, BitmapFont unicodeFont) {
        super(new ListStyle(skin.get(ListStyle.class))); //clone the style for future mutation
        getStyle().font = unicodeFont;

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
        batch.draw(iconSprite, x + 12, y - 40, itemHeight, itemHeight);
        return font.draw(batch, string, x, y, 0, string.length(), width, alignment, false, "...");
    }

    @Override
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }
}
