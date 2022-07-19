package technology.rocketjump.saul.assets.editor.widgets.entitybrowser;

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
import java.nio.file.Files;
import java.nio.file.Path;

public class EntityBrowserContextMenu extends PopupMenu {

	private final MessageDispatcher messageDispatcher;

	@Inject
	public EntityBrowserContextMenu(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public void setContext(EntityBrowserValue value) {

		this.clearChildren();

		MenuItem subDirItem = new MenuItem("Create subdirectory");
		subDirItem.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_CREATE_DIRECTORY_DIALOG, value.path);
			}
		});
		this.addItem(subDirItem);

		MenuItem createEntityDefinition = new MenuItem("Add new entity asset");
		createEntityDefinition.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_CREATE_ASSET_DIALOG, new ShowCreateAssetDialogMessage(value.entityType, value.path, value.getTypeDescriptor())); //TODO: am lazy by not constructing new message type for this
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
