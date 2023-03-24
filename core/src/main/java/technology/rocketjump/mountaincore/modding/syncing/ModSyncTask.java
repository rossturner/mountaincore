package technology.rocketjump.mountaincore.modding.syncing;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import okhttp3.Response;
import org.apache.commons.compress.archivers.ArchiveException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.async.BackgroundTaskResult;
import technology.rocketjump.mountaincore.modding.LocalModRepository;
import technology.rocketjump.mountaincore.modding.authentication.ModioAuthManager;
import technology.rocketjump.mountaincore.modding.authentication.ModioRequestAdapter;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;
import technology.rocketjump.mountaincore.modding.model.WrappedModioJson;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class ModSyncTask implements Callable<BackgroundTaskResult> {

	private final LocalModRepository localModRepository;
	private final MessageDispatcher messageDispatcher;
	private final ModioAuthManager modioAuthManager;
	private final ModioRequestAdapter modioRequestAdapter;

	@Inject
	public ModSyncTask(LocalModRepository localModRepository, MessageDispatcher messageDispatcher, ModioAuthManager modioAuthManager,
					   ModioRequestAdapter modioRequestAdapter) {
		this.localModRepository = localModRepository;
		this.messageDispatcher = messageDispatcher;
		this.modioAuthManager = modioAuthManager;
		this.modioRequestAdapter = modioRequestAdapter;
	}

	@Override
	public BackgroundTaskResult call() {
		try {
			localModRepository.updateLocalModListing();
			if (!modioAuthManager.isUserAuthenticated()) {
				// Not logged in to mod.io, so we can only update local files
				return BackgroundTaskResult.success();
			}

			List<WrappedModioJson> subscribedMods = modioRequestAdapter.getSubscribedMods(modioAuthManager.getAccessToken());
			Set<Long> subscribedModIds = new HashSet<>();

			for (WrappedModioJson mod : subscribedMods) {
				subscribedModIds.add(mod.getModioId());

				ParsedMod existingMod = localModRepository.getByModioId(mod.getModioId());
				if (existingMod == null) {
					download(mod);
				} else {
					String downloadChecksum = existingMod.getModioMeta().get().getDownloadChecksum();
					if (!downloadChecksum.equals(mod.getModfileHash())) {
						localModRepository.delete(existingMod, false);
						download(mod);
					}
				}
			}

			for (ParsedMod localMod : localModRepository.getAll()) {
				if (localMod.getModioMeta().isPresent()) {
					Long modioId = localMod.getModioMeta().get().getModioId();
					if (!subscribedModIds.contains(modioId)) {
						localModRepository.delete(localMod, true);
					}
				}
			}

			return BackgroundTaskResult.success();
		} catch (Exception e) {
			Logger.error(e, "Error syncing mods");
			// Not showing errors to user
			return BackgroundTaskResult.success();
		} finally {
			messageDispatcher.dispatchMessage(MessageType.MOD_SYNC_COMPLETED);
		}
	}

	private void download(WrappedModioJson mod) throws IOException, ArchiveException {
		try (Response downloadResponse = modioRequestAdapter.downloadFile(mod.getBinaryUrl())) {
			if (downloadResponse.isSuccessful()) {
				try (InputStream downloadStream = downloadResponse.body().byteStream()) {
					localModRepository.writeDownload(downloadStream, mod);
				}
			} else {
				Logger.error("Error downloading mod: " + downloadResponse.message());
			}
		}
	}

}
