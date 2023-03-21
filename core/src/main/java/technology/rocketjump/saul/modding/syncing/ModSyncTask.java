package technology.rocketjump.saul.modding.syncing;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.async.BackgroundTaskResult;
import technology.rocketjump.saul.modding.LocalModRepository;
import technology.rocketjump.saul.modding.authentication.ModioAuthManager;

import java.util.concurrent.Callable;

public class ModSyncTask implements Callable<BackgroundTaskResult> {

	private boolean localUpdateOnly; // when not authenticated with mod.io and we should only update local files
	private final LocalModRepository localModRepository;
	private final MessageDispatcher messageDispatcher;
	private final ModioAuthManager modioAuthManager;

	@Inject
	public ModSyncTask(LocalModRepository localModRepository, MessageDispatcher messageDispatcher, ModioAuthManager modioAuthManager) {
		this.localModRepository = localModRepository;
		this.messageDispatcher = messageDispatcher;
		this.modioAuthManager = modioAuthManager;
	}

	@Override
	public BackgroundTaskResult call() throws Exception {
		try {
			localModRepository.updateLocalModListing();
			if (!modioAuthManager.isUserAuthenticated()) {
				// Not logged in to mod.io, so we can only update local files
				return BackgroundTaskResult.success();
			}

			return BackgroundTaskResult.success();
		} finally {
			messageDispatcher.dispatchMessage(MessageType.MOD_SYNC_COMPLETED);
		}
	}

	public void setLocalUpdateOnly(boolean localUpdateOnly) {
		this.localUpdateOnly = localUpdateOnly;
	}

}
