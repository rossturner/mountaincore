package technology.rocketjump.saul.ui.skins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.VisUI;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;

@Singleton
public class GuiSkinRepository implements Telegraph {

	public static final String MAIN_GAME_SKIN_FILE_PATH = "assets/ui/skin/main-game-skin.json";
	private static final String MENU_SKIN_FILE_PATH = "assets/ui/skin/menu-skin.json";
	private static final int FONT_SCALE = 2;
	private final FontRepository fontRepository;
	private final OnDemandFontRepository onDemandFontRepository;
	private final Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private final Skin menuSkin;
	private final Skin mainGameSkin;

	@Inject
	public GuiSkinRepository(FontRepository fontRepository, OnDemandFontRepository onDemandFontRepository, MessageDispatcher messageDispatcher) {
		this.fontRepository = fontRepository;
		this.onDemandFontRepository = onDemandFontRepository;

		mainGameSkin = loadSkin(MAIN_GAME_SKIN_FILE_PATH);
		menuSkin = loadSkin(MENU_SKIN_FILE_PATH);

		if (!VisUI.isLoaded()) {
			VisUI.load();
		}

		messageDispatcher.addListener(this, MessageType.FONTS_CHANGED);
	}

	private Skin loadSkin(String skinJsonPath) {
		FileHandle menuSkinFile = Gdx.files.internal(skinJsonPath);
		FileHandle menuSkinAtlasFile = menuSkinFile.sibling(menuSkinFile.nameWithoutExtension() + ".atlas");

		Skin skin = new Skin();
		skin.add("placeholder-font", fontRepository.getDefaultFontForUI().getBitmapFont(), BitmapFont.class);
		skin.add("placeholder-header-font", fontRepository.getDefaultFontForUI().getBitmapFont(), BitmapFont.class);

		skin.add("header-font-32", onDemandFontRepository.getHeaderFont(32 * FONT_SCALE));
		skin.add("header-font-36", onDemandFontRepository.getHeaderFont(36 * FONT_SCALE));
		skin.add("header-font-47", onDemandFontRepository.getHeaderFont(47 * FONT_SCALE));
		skin.add("header-font-50", onDemandFontRepository.getHeaderFont(50 * FONT_SCALE));
		skin.add("header-font-65", onDemandFontRepository.getHeaderFont(65 * FONT_SCALE));
		skin.add("default-font-16", onDemandFontRepository.getDefaultFont(16 * FONT_SCALE));
		skin.add("default-font-18", onDemandFontRepository.getDefaultFont(18 * FONT_SCALE));
		skin.add("default-font-19", onDemandFontRepository.getDefaultFont(19 * FONT_SCALE));
		skin.add("default-font-23", onDemandFontRepository.getDefaultFont(23 * FONT_SCALE));
		skin.add("default-font-24", onDemandFontRepository.getDefaultFont(24 * FONT_SCALE));

		skin.addRegions(new TextureAtlas(menuSkinAtlasFile));
		skin.load(menuSkinFile);
		return skin;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		return switch (msg.message) {
			case MessageType.FONTS_CHANGED -> fontChanged();
			default -> throw new IllegalArgumentException("Unexpected message type handled: " + msg.message);
		};
	}

	@Deprecated
	public Skin getDefault() {
		return uiSkin;
	}

	public Skin getMenuSkin() {
		return menuSkin;
	}

	public Skin getMainGameSkin() {
		return mainGameSkin;
	}

	public boolean fontChanged() {
		reassignFonts(mainGameSkin, MAIN_GAME_SKIN_FILE_PATH);
		reassignFonts(menuSkin, MENU_SKIN_FILE_PATH);

		// All of the following is for the now defunct UI skin
		BitmapFont bitmapFont = fontRepository.getDefaultFontForUI().getBitmapFont();
		uiSkin.add("default-font", bitmapFont);

		uiSkin.get(TextField.TextFieldStyle.class).font = bitmapFont;
		uiSkin.get(Label.LabelStyle.class).font = bitmapFont;
		uiSkin.get(CheckBox.CheckBoxStyle.class).font = bitmapFont;
		uiSkin.get(Window.WindowStyle.class).titleFont = bitmapFont;
		uiSkin.get(List.ListStyle.class).font = bitmapFont;
		uiSkin.get(SelectBox.SelectBoxStyle.class).font = bitmapFont;
		uiSkin.get(SelectBox.SelectBoxStyle.class).listStyle.font = bitmapFont;
		uiSkin.get(TextButton.TextButtonStyle.class).font = bitmapFont;

		return true;
	}

	private void reassignFonts(Skin currentSkin, String pathToSkin) {
		currentSkin.add("header-font-32", onDemandFontRepository.getHeaderFont(32 * 2));
		currentSkin.add("header-font-36", onDemandFontRepository.getHeaderFont(36 * 2));
		currentSkin.add("header-font-47", onDemandFontRepository.getHeaderFont(47 * 2));
		currentSkin.add("header-font-50", onDemandFontRepository.getHeaderFont(50 * 2));
		currentSkin.add("header-font-65", onDemandFontRepository.getHeaderFont(65 * 2));
		currentSkin.add("default-font-16", onDemandFontRepository.getDefaultFont(16 * 2));
		currentSkin.add("default-font-18", onDemandFontRepository.getDefaultFont(18 * 2));
		currentSkin.add("default-font-19", onDemandFontRepository.getDefaultFont(19 * 2));
		currentSkin.add("default-font-23", onDemandFontRepository.getDefaultFont(23 * 2));


		currentSkin.load(Gdx.files.internal(pathToSkin));
	}

}
