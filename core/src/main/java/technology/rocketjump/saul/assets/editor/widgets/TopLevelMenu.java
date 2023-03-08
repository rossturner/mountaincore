package technology.rocketjump.saul.assets.editor.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserCallback;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserConfiguration;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.modding.ModParser;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;

public class TopLevelMenu extends MenuBar implements Telegraph {

	private final EditorStateProvider editorStateProvider;
	private final MessageDispatcher messageDispatcher;
	private final NativeFileChooser fileChooser;
	private final Menu devModMenu;

	@Inject
	public TopLevelMenu(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher, NativeFileChooser fileChooser) {
		this.editorStateProvider = editorStateProvider;
		this.messageDispatcher = messageDispatcher;
		this.fileChooser = fileChooser;

		Menu fileMenu = new Menu("Mods");
		MenuItem newMod = new MenuItem("New");
		newMod.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				createNewMod();
			}
		});
		MenuItem openMod = new MenuItem("Open");
		openMod.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				openModDirectory();
			}
		});
		fileMenu.addItem(newMod);
		fileMenu.addItem(openMod);
		this.addMenu(fileMenu);

//		todo: nice to have, recents submenu
//		Menu recentModsSubMenu = new Menu("");
//		MenuItem recentMods = new MenuItem("Recent Mods");
//		recentMods.setSubMenu(recentModsSubMenu);
//		fileMenu.addItem(recentMods);

		Menu preferences = new Menu("Preferences");
		preferences.addItem(new AutoSaveCheckbox());
		this.addMenu(preferences);

		devModMenu = new Menu("Dev Mode");
		updateDevMenu();

		Menu selectedMod = new Menu(editorStateProvider.getState().getModDir());
		selectedMod.setTouchable(Touchable.disabled);
		selectedMod.openButton.setTouchable(Touchable.disabled);
		this.addMenu(selectedMod);

		messageDispatcher.addListener(this, MessageType.DEV_MODE_CHANGED);
	}

	private void updateDevMenu() {
		if (GlobalSettings.DEV_MODE) {
			this.insertMenu(2, devModMenu);
		} else {
			this.removeMenu(devModMenu);
		}
	}

	private void createNewMod() {
		messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_CREATE_MOD_DIALOG);
	}

	private void openModDirectory() {
		fileChooser.chooseFile(modDirectoryFileChooserConfig(), new NativeFileChooserCallback() {
			@Override
			public void onFileChosen(FileHandle file) {
				//set mod dir and reload
				if (ModParser.MOD_INFO_FILENAME.equals(file.name())) {
					messageDispatcher.dispatchMessage(MessageType.EDITOR_OPEN_MOD, file);
				} else {
					openModDirectory();
				}
			}

			@Override
			public void onCancellation() {

			}

			@Override
			public void onError(Exception exception) {

			}
		});
	}

	private NativeFileChooserConfiguration modDirectoryFileChooserConfig() {
		NativeFileChooserConfiguration config = new NativeFileChooserConfiguration();
		config.title = "Open modInfo.json";
		config.mimeFilter = "application/json";
		config.nameFilter = (dir, name) -> ModParser.MOD_INFO_FILENAME.equals(name);
		return config;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == MessageType.DEV_MODE_CHANGED) {
			updateDevMenu();
			return true;
		}
		return false;
	}

	class AutoSaveCheckbox extends MenuItem {

		public AutoSaveCheckbox() {
			super("");
			//dirty code time
			this.clearChildren();
			add(WidgetBuilder.checkBox("Auto-Save", editorStateProvider.getState().isAutosave(),
			x -> editorStateProvider.getState().setAutosave(true),
			x -> editorStateProvider.getState().setAutosave(false)));
		}
	}

}
