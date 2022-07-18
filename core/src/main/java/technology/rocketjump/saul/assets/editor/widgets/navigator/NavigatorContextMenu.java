package technology.rocketjump.saul.assets.editor.widgets.navigator;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.messaging.MessageType;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;

public class NavigatorContextMenu extends PopupMenu {

	private NavigatorTreeValue value;
	private final MessageDispatcher messageDispatcher;

	@Inject
	public NavigatorContextMenu(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}


	public void setContext(NavigatorTreeValue value) {
		this.value = value;

		this.clearChildren();

		if (!value.treeValueType.equals(NavigatorTreeValue.TreeValueType.ENTITY_DIR)) {
			MenuItem createEntityDefinition = new MenuItem("Add new " + value.entityType.name().toLowerCase() + " type");
			createEntityDefinition.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					//TODO: start the adding new asset type here
					System.out.println("Clicked createEntityDefinition at " + value.path.toAbsolutePath());
				}
			});
			this.addItem(createEntityDefinition);

			MenuItem subDirItem = new MenuItem("Create subdirectory");
			subDirItem.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					System.out.println("Clicked create subdirectory for " + value.path.toAbsolutePath());
					messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_CREATE_DIRECTORY_DIALOG, value.path);
				}
			});
			this.addItem(subDirItem);
		}

		MenuItem explorerItem = new MenuItem("Open in explorer");
		explorerItem.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					Desktop.getDesktop().open(value.path.toFile());
				} catch (IOException e) {
					Logger.error("Desktop is not supported");
				}
			}
		});
		this.addItem(explorerItem);
	}

}
