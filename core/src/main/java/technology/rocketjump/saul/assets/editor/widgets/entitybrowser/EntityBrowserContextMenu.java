package technology.rocketjump.saul.assets.editor.widgets.entitybrowser;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import org.pmw.tinylog.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EntityBrowserContextMenu extends PopupMenu {

	private EntityBrowserValue value;

	public void setContext(EntityBrowserValue value) {
		this.value = value;

		this.clearChildren();

		MenuItem subDirItem = new MenuItem("Create subdirectory");
		subDirItem.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Clicked create subdirectory for " + value.path.toAbsolutePath());
			}
		});
		this.addItem(subDirItem);

		MenuItem createEntityDefinition = new MenuItem("Add new entity asset");
		createEntityDefinition.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				System.out.println("Clicked createEntityDefinition at " + value.path.toAbsolutePath());
			}
		});
		this.addItem(createEntityDefinition);

		MenuItem explorerItem = new MenuItem("Open in explorer");
		explorerItem.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					Path targetPath = value.path;
					if (!Files.isDirectory(targetPath)) {
						targetPath = targetPath.getParent();
					}
					Desktop.getDesktop().open(targetPath.toFile());
				} catch (IOException e) {
					Logger.error("Desktop is not supported");
				}
			}
		});
		this.addItem(explorerItem);

	}

}
