package technology.rocketjump.saul.mapping.minimap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.ray3k.tenpatch.TenPatchDrawable;

public class MinimapFrame extends Table {

	private static final float LEFT_BORDER_WIDTH = 27f / 2f;
	private static final float RIGHT_BORDER_WIDTH = 28f / 2f;
	private static final float TOP_BORDER_HEIGHT = 27f / 2f;
	private static final float BOTTOM_BORDER_HEIGHT = 39f / 2f;
	private final MinimapImage minimapImage;
	private final Skin skin;

	public MinimapFrame(MinimapImage minimapImage, Skin skin) {
		this.minimapImage = minimapImage;
		this.skin = skin;
		this.setTouchable(Touchable.enabled);

		reset();
	}

	public void reset() {
		this.clearChildren();

		Container<Actor> topContainer = new Container<>();
		topContainer.setBackground(skin.get("map_border_top_patch", TenPatchDrawable.class));
		this.add(topContainer).height(TOP_BORDER_HEIGHT).width(minimapImage.getWidth() + LEFT_BORDER_WIDTH + RIGHT_BORDER_WIDTH).colspan(3).expandX().fillX().row();

		Container<Actor> leftContainer = new Container<>();
		leftContainer.setBackground(skin.get("map_border_left_patch", TenPatchDrawable.class));
		this.add(leftContainer).width(LEFT_BORDER_WIDTH).height(minimapImage.getHeight()).fillY();

		this.add(minimapImage).expand();

		Container<Actor> rightContainer = new Container<>();
		rightContainer.setBackground(skin.get("map_border_right_patch", TenPatchDrawable.class));
		this.add(rightContainer).width(RIGHT_BORDER_WIDTH).height(minimapImage.getHeight()).fillY().row();

		Container<Actor> bottomContainer = new Container<>();
		bottomContainer.setBackground(skin.get("map_border_bottom_patch", TenPatchDrawable.class));
		this.add(bottomContainer).height(BOTTOM_BORDER_HEIGHT).width(minimapImage.getWidth() + LEFT_BORDER_WIDTH + RIGHT_BORDER_WIDTH).colspan(3).expandX().fillX().row();
	}

}
