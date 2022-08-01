package technology.rocketjump.saul.assets.editor.widgets;

import com.google.inject.Inject;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;

public class TopLevelMenu extends MenuBar {

	@Inject
	public TopLevelMenu() {
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
	}

}
