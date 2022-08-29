package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EnhancedSelectBox<T> extends SelectBox<T> {

	protected boolean isShowingList;

	public EnhancedSelectBox(Skin skin) {
		super(skin);
	}

	public boolean isShowingList() {
		return isShowingList;
	}

	@Override
	public void showList() {
		super.showList();
		this.isShowingList = true;
	}

	@Override
	public void hideList() {
		super.hideList();
		this.isShowingList = false;
	}
}
