package technology.rocketjump.saul.ui.skins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.VisUI;
import technology.rocketjump.saul.ui.fonts.FontRepository;

@Singleton
public class GuiSkinRepository {

	private final FontRepository fontRepository;
	private final Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private final Skin menuSkin;
	private final Skin mainGameSkin;

	@Inject
	public GuiSkinRepository(FontRepository fontRepository) {
		this.fontRepository = fontRepository;

		fontChanged();

		menuSkin = loadSkin("assets/ui/skin/menu-skin.json");
		mainGameSkin = loadSkin("assets/ui/skin/main-game-skin.json");

		if (!VisUI.isLoaded()) {
			VisUI.load();
		}
	}

	private Skin loadSkin(String skinJsonPath) {
		FileHandle menuSkinFile = Gdx.files.internal(skinJsonPath);
		FileHandle menuSkinAtlasFile = menuSkinFile.sibling(menuSkinFile.nameWithoutExtension() + ".atlas");

		Skin skin = new Skin();
		skin.add("placeholder-font", fontRepository.getDefaultFontForUI().getBitmapFont(), BitmapFont.class);
		skin.add("placeholder-header-font", fontRepository.getDefaultFontForUI().getBitmapFont(), BitmapFont.class);
		skin.addRegions(new TextureAtlas(menuSkinAtlasFile));
		skin.load(menuSkinFile);
		return skin;
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

	public void fontChanged() {
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
	}

}
