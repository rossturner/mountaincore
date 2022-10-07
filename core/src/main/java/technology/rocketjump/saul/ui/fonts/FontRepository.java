package technology.rocketjump.saul.ui.fonts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.constants.UiConstants;
import technology.rocketjump.saul.ui.i18n.I18nRepo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Singleton
public class FontRepository {

	private static final Set<Integer> DEFAULT_FONT_POINT_SIZES = Set.of(12, 14, 16, 18, 20);
	private static final Set<Integer> HEADER_FONT_POINT_SIZES = Set.of(36, 39, 47);
	public static final String UNICODE_FONT_FILENAME = "NotoSansCJKjp-Regular.otf";

	private final I18nRepo i18nRepo;
	private final UiConstants uiConstants;
	private final GameFont[] defaultGameFonts;
	private final GameFont[] headerGameFonts;

	private String currentFontName;
	private GameFont largestFont;
	private GameFont defaultUIFont;
	private final GameFont guaranteedUnicodeFont;

	@Inject
	public FontRepository(I18nRepo i18nRepo, ConstantsRepo constantsRepo) {
		this.i18nRepo = i18nRepo;
		this.uiConstants = constantsRepo.getUiConstants();
		// MODDING - Expose the font selction and sizes from small to large

		this.currentFontName = uiConstants.getDefaultFont();
		if (i18nRepo.getCurrentLanguageType().getFontName() != null) {
			this.currentFontName = i18nRepo.getCurrentLanguageType().getFontName();
		}
		defaultGameFonts = generateGameFonts(this.currentFontName, DEFAULT_FONT_POINT_SIZES);
		this.largestFont = getFont(defaultGameFonts, 20);
		this.defaultUIFont = getFont(defaultGameFonts, 16);

		headerGameFonts = generateGameFonts(uiConstants.getHeaderFont(), HEADER_FONT_POINT_SIZES);


		this.guaranteedUnicodeFont = generateFont(UNICODE_FONT_FILENAME);
	}

	private GameFont generateFont(String fontFilename) {
		FileHandle fontFile = Gdx.files.internal("assets/ui/fonts/" + fontFilename);
		if (!fontFile.exists()) {
			Logger.error(fontFile.toString() + " does not exist");
			return defaultUIFont;
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.characters = i18nRepo.getAllCharacters(FreeTypeFontGenerator.DEFAULT_CHARS);
		return new GameFont(generator.generateFont(parameter), 16);
	}

	private GameFont[] generateGameFonts(String fontFileName, Set<Integer> pointSizes) {
		FileHandle fontFile = Gdx.files.internal("assets/ui/fonts/" + fontFileName);
		if (!fontFile.exists()) {
			Logger.error(fontFile + " does not exist");
			return null;
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.genMipMaps = true;
		parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
		parameter.magFilter = Texture.TextureFilter.MipMapLinearLinear;
		parameter.characters = i18nRepo.getAllCharacters(FreeTypeFontGenerator.DEFAULT_CHARS);

		List<Integer> fontPointSizes = new java.util.ArrayList<>(pointSizes);
		Collections.sort(fontPointSizes);
		GameFont[] gameFonts = new GameFont[fontPointSizes.get(fontPointSizes.size() - 1)];

		GameFont previous = null;
		for (Integer fontPointSize : fontPointSizes) {
			parameter.size = fontPointSize;
			BitmapFont bitmapFont = generator.generateFont(parameter);
			GameFont generated = new GameFont(bitmapFont, fontPointSize);
			if (previous != null) {
				previous.setBigger(generated);
				generated.setSmaller(previous);
			}
			previous = generated;
			gameFonts[fontPointSize - 1] = generated;
		}
		generator.dispose(); // don't forget to dispose to avoid memory leaks!
		return gameFonts;
	}

	private GameFont getFont(GameFont[] gameFonts, int pointSize) {
		int pointSizeToUse = pointSize;
		GameFont gameFont = null;
		if (pointSizeToUse > gameFonts.length) {
			pointSizeToUse = gameFonts.length;
		}
		while (gameFont == null) {
			gameFont = gameFonts[pointSizeToUse - 1];
			pointSizeToUse--;
		}

		//todo: warn requesting point size is different;
		return gameFont;
	}


	public boolean changeFontName(String fontName) {
		if (fontName == null) {
			fontName = uiConstants.getDefaultFont();
		}
		if (!fontName.equals(currentFontName)) {
			this.currentFontName = fontName;
			this.defaultUIFont.dispose();
			this.largestFont.dispose();
			generateGameFonts(this.currentFontName, DEFAULT_FONT_POINT_SIZES);
			//TODO: header fonts?
			return true;
		} else {
			return false;
		}
	}

	public GameFont getDefaultFontForUI() {
		return defaultUIFont;
	}

	public GameFont getLargestFont() {
		return largestFont;
	}

	public GameFont getUnicodeFont() {
		return this.guaranteedUnicodeFont;
	}

	public GameFont getHeaderFont(int fontPointSize) {
		return getFont(headerGameFonts, fontPointSize);
	}

	public GameFont getDefaultFont(int fontPointSize) {
		return getFont(defaultGameFonts, fontPointSize);
	}
}
