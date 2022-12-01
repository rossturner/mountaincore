package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.i18n.I18nText;

public abstract class GameDialog implements Disposable {

	private final Skin skin;
	protected final MessageDispatcher messageDispatcher;
	protected final SoundAssetDictionary soundAssetDictionary;
	protected Dialog dialog;
	protected Image fullScreenOverlay;
	protected boolean addedCursorChangeListeners;

	protected final Table layoutTable = new Table();
	protected final Table contentTable = new Table();

	public GameDialog(I18nText titleText, Skin skin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		this(titleText, skin, messageDispatcher, skin.get(Window.WindowStyle.class), soundAssetDictionary);
	}
	public GameDialog(I18nText titleText, Skin skin, MessageDispatcher messageDispatcher,
					  Window.WindowStyle windowStyle, SoundAssetDictionary soundAssetDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;
		dialog = new Dialog("", skin) {
			public void result(Object obj) {
				if (obj instanceof Runnable) {
					((Runnable)obj).run();
				}
				if (fullScreenOverlay != null) {
					fullScreenOverlay.remove();
				}
				dispose();
			}
		};
		dialog.setStyle(windowStyle);
		dialog.getButtonTable().defaults().padLeft(25).padRight(25);
		this.skin = skin;

		fullScreenOverlay = new Image(skin, "default-rect");
		fullScreenOverlay.setFillParent(true);
		fullScreenOverlay.setColor(0, 0, 0, 0.6f);

		Button exitButton = new Button(skin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				close();
			}
		});
		exitButton.addListener(new ChangeCursorOnHover(exitButton, GameCursor.SELECT, messageDispatcher));
		exitButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));

		layoutTable.defaults().pad(10);
		layoutTable.add(exitButton).align(Align.topLeft).expandX().padBottom(0).row();
		if (titleText != null) {
			Label titleLabel = new Label(titleText.toString(), skin.get("dialog_title", Label.LabelStyle.class));
			layoutTable.add(titleLabel).center().top().padTop(0).row();
		}
		layoutTable.add(contentTable).grow().center().row();

		dialog.getContentTable().add(layoutTable).grow();
	}

	public void show(Stage stage) {
		if (!addedCursorChangeListeners) {
			for (Actor child : dialog.getButtonTable().getChildren()) {
				child.addListener(new ChangeCursorOnHover(child, GameCursor.SELECT, messageDispatcher));
				child.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			}
			addedCursorChangeListeners = true;
		}

		if (fullScreenOverlay != null) {
			stage.addActor(fullScreenOverlay);
		}
		dialog.show(stage);
	}

	public void close() {
		if (fullScreenOverlay != null) {
			fullScreenOverlay.remove();
		}
		dialog.hide();
		dispose();
	}

	public Table getContentTable() {
		return contentTable;
	}

	public GameDialog withText(I18nText descriptionText) {
		Label label = new Label(descriptionText.toString(), skin.get("dialog_title", Label.LabelStyle.class));
		contentTable.add(label).padRight(180f).padLeft(180f).row();
		return this;
	}

	public GameDialog withButton(I18nText buttonText) {
		return withButton(buttonText, null);
	}

	public GameDialog withButton(I18nText buttonText, Runnable runnable) {
		Button button = new Button(skin.getDrawable("btn_01"));

		Label label = new Label(buttonText.toString(), skin.get("notification_button", Label.LabelStyle.class));
		label.setAlignment(Align.center);
		button.add(label).padLeft(40).padRight(40);

		dialog.button(button, runnable);
		return this;
	}

}
