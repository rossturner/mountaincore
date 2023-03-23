package technology.rocketjump.saul.modding;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.async.BackgroundTaskManager;
import technology.rocketjump.saul.modding.syncing.ModSyncTask;

@Singleton
public class ModSyncMessageHandler implements Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final BackgroundTaskManager backgroundTaskManager;
	private final Provider<ModSyncTask> modSyncTaskProvider;
	private boolean syncInProgress;

	@Inject
	public ModSyncMessageHandler(MessageDispatcher messageDispatcher, BackgroundTaskManager backgroundTaskManager,
								 Provider<ModSyncTask> modSyncTaskProvider) {
		this.messageDispatcher = messageDispatcher;
		this.backgroundTaskManager = backgroundTaskManager;
		this.modSyncTaskProvider = modSyncTaskProvider;

		messageDispatcher.addListener(this, MessageType.REQUEST_SYNC_MOD_FILES);
		messageDispatcher.addListener(this, MessageType.MOD_SYNC_COMPLETED);

		// Kick off mod syncing on startup
		messageDispatcher.dispatchMessage(MessageType.REQUEST_SYNC_MOD_FILES);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REQUEST_SYNC_MOD_FILES -> {
				if (!syncInProgress) {
					syncInProgress = true;
					messageDispatcher.dispatchMessage(MessageType.MOD_SYNC_IN_PROGRESS);
					backgroundTaskManager.runTask(modSyncTaskProvider.get());
				}
			}
			case MessageType.MOD_SYNC_COMPLETED -> {
				syncInProgress = false;
			}
			default -> Logger.error("Unexpected message type handled: " + msg.message);
		}
		return true;
	}
}
