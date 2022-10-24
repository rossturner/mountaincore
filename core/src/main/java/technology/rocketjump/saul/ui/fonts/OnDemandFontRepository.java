package technology.rocketjump.saul.ui.fonts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.constants.UiConstants;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.LanguageType;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class OnDemandFontRepository implements Disposable {

	private final I18nRepo i18nRepo;
	private final UiConstants uiConstants;
	private final MessageDispatcher messageDispatcher;
	// These are the current names of the font files as defined by the language or global defaults
	private String defaultFontName;
	private String headerFontName;

	private Map<Integer, BitmapFont> defaultFonts = new HashMap<>();
	private Map<Integer, BitmapFont> headerFonts = new HashMap<>();

	public static final String UNICODE_FONT_FILENAME = "NotoSansCJKjp-Regular.otf";
	private final BitmapFont guaranteedUnicodeFont;

	@Inject
	public OnDemandFontRepository(I18nRepo i18nRepo, ConstantsRepo constantsRepo, MessageDispatcher messageDispatcher) {
		this.i18nRepo = i18nRepo;
		this.uiConstants = constantsRepo.getUiConstants();
		this.messageDispatcher = messageDispatcher;
		FreeTypeFontGenerator.setMaxTextureSize(8192);

		this.guaranteedUnicodeFont = generateFont(UNICODE_FONT_FILENAME, 16 * 2);
	}

	public void preLanguageUpdated() {
		LanguageType currentLanguage = i18nRepo.getCurrentLanguageType();

		String defaultFontName = currentLanguage.getFontName() != null ? currentLanguage.getFontName() : uiConstants.getDefaultFont();
		String headerFontName = currentLanguage.getHeaderFontName() != null ? currentLanguage.getHeaderFontName() : uiConstants.getHeaderFont();

		if (!defaultFontName.equals(this.defaultFontName) || !headerFontName.equals(this.headerFontName)) {
			dispose();

			this.defaultFontName = defaultFontName;
			this.headerFontName = headerFontName;

			messageDispatcher.dispatchMessage(MessageType.FONTS_CHANGED);
		}
	}

	public BitmapFont getDefaultFont(int pointSize) {
		return defaultFonts.computeIfAbsent(pointSize, a -> generateFont(defaultFontName, pointSize));
	}

	public BitmapFont getHeaderFont(int pointSize) {
		return headerFonts.computeIfAbsent(pointSize, a -> generateFont(headerFontName, pointSize));
	}

	private BitmapFont generateFont(String fontName, int pointSize) {
		FileHandle fontFile = Gdx.files.internal("assets/ui/fonts/" + fontName);
		if (!fontFile.exists()) {
			Logger.error(fontFile + " does not exist");
			return guaranteedUnicodeFont;
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = pointSize;
		parameter.genMipMaps = true;
		parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
		parameter.magFilter = Texture.TextureFilter.MipMapLinearLinear;
		parameter.characters = i18nRepo.getAllCharacters(FreeTypeFontGenerator.DEFAULT_CHARS);
		parameter.padTop = 1;
		parameter.padLeft = 1;
		parameter.padRight = 1;
		parameter.padBottom = 1;
		BitmapFont font = generator.generateFont(parameter);
		generator.dispose();
		return font;
	}

	@Override
	public void dispose() {
		for (BitmapFont font : defaultFonts.values()) {
			if (!font.equals(guaranteedUnicodeFont)) {
				font.dispose();
			}
		}
		defaultFonts.clear();
		for (BitmapFont font : headerFonts.values()) {
			if (!font.equals(guaranteedUnicodeFont)) {
				font.dispose();
			}
		}
		headerFonts.clear();
	}

	public BitmapFont getGuaranteedUnicodeFont() {
		return guaranteedUnicodeFont;
	}
}
