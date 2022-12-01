package technology.rocketjump.saul.ui.skins;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Helpful decorator for shared utility methods and tightly coupled components to the skin file
 */
public class MenuSkin extends Skin {

	public Actor buildBackgroundBaseLayer() {
		Table table = new Table();
		table.setName("backgroundBase");
		table.add(new Image(getDrawable("menu_bg_left"))).left();
		table.add().expandX();
		table.add(new Image(getDrawable("menu_bg_right"))).right();
		return table;
	}

	public Table buildPaperLayer(Actor paperComponents, int outsidePadding, boolean thinPattern) {
		String patternName = "paper_texture_bg_pattern_large";
		if (thinPattern) {
			patternName = "paper_texture_bg_pattern_thin";
		}
		Table baseLayer = new Table();
		baseLayer.setBackground(getDrawable("paper_texture_bg_stretch"));
		baseLayer.add(new Image(getDrawable(patternName))).growY().padLeft(outsidePadding); //TODO : change from padding from outside to just pad from paperComponents out?
		baseLayer.add(paperComponents).top().expandX();
		baseLayer.add(new Image(getDrawable(patternName))).growY().padRight(outsidePadding);
		return baseLayer;
	}
}
