package technology.rocketjump.saul.ui.widgets.text;

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DecoratedStringLabelFactory {

	@Inject
	public DecoratedStringLabelFactory() {

	}

	public DecoratedStringLabel create(DecoratedString decoratedString, String labelStyle, Skin skin) {
		DecoratedStringLabel result = new DecoratedStringLabel();

		HorizontalGroup currentRow = new HorizontalGroup();
		for (DecoratedStringToken token : decoratedString.getTokens()) {
			switch (token.type) {
				case TEXT -> {
					Label label = new Label(token.value, skin.get(labelStyle, Label.LabelStyle.class));
					currentRow.addActor(label);
				}
				case DRAWABLE -> {
					Drawable drawable;
					try {
						drawable = skin.getDrawable(token.value);
					} catch (GdxRuntimeException e) {
						// To handle when drawable not found
						drawable = skin.getDrawable("placeholder");
					}
					currentRow.addActor(new Image(drawable));
				}
				case LINEBREAK -> {
					result.add(currentRow).left().row();
					currentRow = new HorizontalGroup();
				}
			}
		}

		result.add(currentRow).left().row();
		return result;
	}

}
