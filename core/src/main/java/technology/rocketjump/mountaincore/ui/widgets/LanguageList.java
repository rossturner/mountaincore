package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.i18n.I18nRepo;
import technology.rocketjump.mountaincore.ui.i18n.LanguageType;


public class LanguageList extends List<LanguageType> {
    private int alignment = Align.left;


    public LanguageList(I18nRepo i18nRepo, UserPreferences userPreferences, Skin skin, TextureAtlasRepository textureAtlasRepository, BitmapFont unicodeFont) {
        super(new ListStyle(skin.get(ListStyle.class))); //clone the style for future mutation
        getStyle().font = unicodeFont;

        i18nRepo.init(textureAtlasRepository);
        String languageCode = userPreferences.getPreference(UserPreferences.PreferenceKey.LANGUAGE);
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
        Drawable selection = getStyle().selection;
        float middleHeight = selection.getMinHeight() - selection.getBottomHeight() - selection.getTopHeight();
        float remainder = Math.max(middleHeight - font.getLineHeight(), 0);
        float yOffset = font.getLineHeight() - (remainder / 2f);
        batch.draw(iconSprite, x, y - yOffset, iconSprite.getWidth(), iconSprite.getHeight());
        return font.draw(batch, string, x, y, 0, string.length(), width, alignment, false, "...");
    }

    @Override
    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }
}
