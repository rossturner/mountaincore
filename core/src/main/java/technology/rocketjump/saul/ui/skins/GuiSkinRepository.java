package technology.rocketjump.saul.ui.skins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.VisUI;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;

@Singleton
public class GuiSkinRepository implements Telegraph {

	public static final String MAIN_GAME_SKIN_FILE_PATH = "assets/ui/skin/main-game-skin.json";
	private static final String MENU_SKIN_FILE_PATH = "assets/ui/skin/menu-skin.json";
	private static final String MANAGEMENT_SKIN_FILE_PATH = "assets/ui/skin/management-skin.json";
	private final OnDemandFontRepository onDemandFontRepository;
	private final MenuSkin menuSkin;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;

	@Inject
	public GuiSkinRepository(OnDemandFontRepository onDemandFontRepository, MessageDispatcher messageDispatcher) {
		this.onDemandFontRepository = onDemandFontRepository;

		mainGameSkin = loadSkin(MAIN_GAME_SKIN_FILE_PATH, new MainGameSkin());
		menuSkin = loadSkin(MENU_SKIN_FILE_PATH, new MenuSkin());
		managementSkin = loadSkin(MANAGEMENT_SKIN_FILE_PATH, new ManagementSkin());

		if (!VisUI.isLoaded()) {
			VisUI.load();
		}

		messageDispatcher.addListener(this, MessageType.FONTS_CHANGED);
	}

	private <T extends Skin> T loadSkin(String skinJsonPath, T skin) {
		FileHandle menuSkinFile = Gdx.files.internal(skinJsonPath);
		FileHandle menuSkinAtlasFile = menuSkinFile.sibling(menuSkinFile.nameWithoutExtension() + ".atlas");

		skin.add("header-font-32", onDemandFontRepository.getHeaderFont(32));
		skin.add("header-font-36", onDemandFontRepository.getHeaderFont(36));
		skin.add("header-font-40", onDemandFontRepository.getHeaderFont(40));
		skin.add("header-font-47", onDemandFontRepository.getHeaderFont(47));
		skin.add("header-font-50", onDemandFontRepository.getHeaderFont(50));
		skin.add("header-font-65", onDemandFontRepository.getHeaderFont(65));
		skin.add("default-font-16", onDemandFontRepository.getDefaultFont(16));
		skin.add("default-font-18", onDemandFontRepository.getDefaultFont(18));
		skin.add("default-font-19", onDemandFontRepository.getDefaultFont(19));
		skin.add("default-font-21", onDemandFontRepository.getDefaultFont(21));
		skin.add("default-font-23", onDemandFontRepository.getDefaultFont(23));
		skin.add("default-font-24", onDemandFontRepository.getDefaultFont(24));

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

	public MenuSkin getMenuSkin() {
		return menuSkin;
	}

	public MainGameSkin getMainGameSkin() {
		return mainGameSkin;
	}

	public ManagementSkin getManagementSkin() {
		return managementSkin;
	}

	public boolean fontChanged() {
		reassignFonts(mainGameSkin, MAIN_GAME_SKIN_FILE_PATH);
		reassignFonts(menuSkin, MENU_SKIN_FILE_PATH);
		reassignFonts(managementSkin, MANAGEMENT_SKIN_FILE_PATH);
		return true;
	}

	private void reassignFonts(Skin currentSkin, String pathToSkin) {
		currentSkin.add("header-font-32", onDemandFontRepository.getHeaderFont(32));
		currentSkin.add("header-font-36", onDemandFontRepository.getHeaderFont(36));
		currentSkin.add("header-font-40", onDemandFontRepository.getHeaderFont(40));
		currentSkin.add("header-font-47", onDemandFontRepository.getHeaderFont(47));
		currentSkin.add("header-font-50", onDemandFontRepository.getHeaderFont(50));
		currentSkin.add("header-font-65", onDemandFontRepository.getHeaderFont(65));
		currentSkin.add("default-font-16", onDemandFontRepository.getDefaultFont(16));
		currentSkin.add("default-font-18", onDemandFontRepository.getDefaultFont(18));
		currentSkin.add("default-font-19", onDemandFontRepository.getDefaultFont(19));
		currentSkin.add("default-font-23", onDemandFontRepository.getDefaultFont(23));
		currentSkin.add("default-font-24", onDemandFontRepository.getDefaultFont(24));


		currentSkin.load(Gdx.files.internal(pathToSkin));
	}

}
