package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;

import java.nio.file.Files;
import java.nio.file.Path;

public class NotificationDialog extends GameDialog {

	private Texture texture;

	public NotificationDialog(I18nText titleText, I18nText descriptionText, String imageFilename, Skin uiSkin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		super(null, uiSkin, messageDispatcher, soundAssetDictionary);

		contentTable.clearChildren();

		Table leftColumn = new Table();
		Table rightColumn = new Table();

		Label titleLabel = new Label(titleText.toString(), uiSkin.get("notification_title", Label.LabelStyle.class));
		Label descriptionLabel = new Label(descriptionText.toString(), uiSkin.get("notification_body", Label.LabelStyle.class));
		descriptionLabel.setWrap(true);

		leftColumn.add(titleLabel).center().padBottom(85).row();
		leftColumn.add(descriptionLabel).center().padLeft(85).width(900).row();

		if (imageFilename != null) {
			String filePath = "assets/ui/notifications/" + imageFilename;
			if (Files.exists(Path.of(filePath))) {
				texture = new Texture(filePath);
				Image image = new Image(texture);
				rightColumn.add(image).center();
			} else {
				Logger.warn("Could not find image " + filePath + " for notification " + titleText.toString());
			}
		}

		contentTable.add(leftColumn).top();
		contentTable.add(rightColumn).top().row();
	}

	@Override
	public void dispose() {
		if (texture != null) {
			texture.dispose();
		}
	}

}
