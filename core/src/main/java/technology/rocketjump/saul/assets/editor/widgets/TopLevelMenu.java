package technology.rocketjump.saul.assets.editor.widgets;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;

public class TopLevelMenu extends MenuBar {

	private final EditorStateProvider editorStateProvider;

	@Inject
	public TopLevelMenu(EditorStateProvider editorStateProvider) {
		this.editorStateProvider = editorStateProvider;
//		this.setMenuListener(new MenuBar.MenuBarListener() {
//			@Override
//			public void menuOpened (Menu menu) {
//				System.out.println("Opened menu: " + menu.getTitle());
//			}
//
//			@Override
//			public void menuClosed (Menu menu) {
//				System.out.println("Closed menu: " + menu.getTitle());
//			}
//		});

		Menu fileMenu = new Menu("mods/base");
		fileMenu.addItem(new MenuItem("Select other mod"));
		this.addMenu(fileMenu);

		Menu preferences = new Menu("Preferences");
		preferences.addItem(new AutoSaveCheckbox());
		this.addMenu(preferences);

	}

	class AutoSaveCheckbox extends MenuItem {

		public AutoSaveCheckbox() {
			super("");
			//dirty code time
			this.clearChildren();
			add(WidgetBuilder.checkBox("Auto-Save", editorStateProvider.getState().isAutosave(),
			x -> {
				editorStateProvider.getState().setAutosave(true);
			},
			x -> {
				editorStateProvider.getState().setAutosave(false);
			}));
		}
	}

}
