package technology.rocketjump.mountaincore.persistence;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.async.BackgroundTaskManager;
import technology.rocketjump.mountaincore.messaging.async.BackgroundTaskResult;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

@Singleton
public class SavedGameStore implements Telegraph, DisplaysText, Disposable {

	public static final String ARCHIVE_HEADER_ENTRY_NAME = "header.json";
	public static final String MINIMAP_ENTRY_NAME = "minimap.png";
	private final UserFileManager userFileManager;
	private final BackgroundTaskManager backgroundTaskManager;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private boolean refreshInProgress = false;

	private final Map<String, SavedGameInfo> bySettlementName = new HashMap<>();

	@Inject
	public SavedGameStore(UserFileManager userFileManager, BackgroundTaskManager backgroundTaskManager,
						  MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator) {
		this.userFileManager = userFileManager;
		this.backgroundTaskManager = backgroundTaskManager;
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;

		messageDispatcher.addListener(this, MessageType.SAVED_GAMES_PARSED);
		messageDispatcher.addListener(this, MessageType.SAVE_COMPLETED);

		refresh();
	}

	public void refresh() {
		if (!refreshInProgress) {
			if (GlobalSettings.DEV_MODE) {
				Logger.debug("Refreshing save file list");
			}
			refreshInProgress = true;
			backgroundTaskManager.runTask((Callable<BackgroundTaskResult>) () -> {
				List<SavedGameInfo> results = new ArrayList<>();
				for (File saveFile : userFileManager.getAllSaveFiles()) {
					try {
						InputStream archiveStream = new FileInputStream(saveFile);
						ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, archiveStream);

						StringWriter headerWriter = new StringWriter();
						Pixmap minimapPixmap = null;
						boolean headerParsed = false;

						ArchiveEntry archiveEntry = archive.getNextEntry();
						while (archiveEntry != null) {
							if (ARCHIVE_HEADER_ENTRY_NAME.equals(archiveEntry.getName())) {
								IOUtils.copy(archive, headerWriter);
								headerParsed = true;
							} else if (MINIMAP_ENTRY_NAME.equals(archiveEntry.getName())) {
								byte[] rawData = IOUtils.toByteArray(archive);
								minimapPixmap = new Pixmap(rawData, 0, rawData.length);
							}
							archiveEntry = archive.getNextEntry();
						}

						if (!headerParsed) {
							Logger.error("Could not find " + ARCHIVE_HEADER_ENTRY_NAME + " in " + saveFile.getName());
							continue;
						}

						IOUtils.closeQuietly(headerWriter);
						IOUtils.closeQuietly(archive);
						IOUtils.closeQuietly(archiveStream);

						JSONObject headerJson = JSON.parseObject(headerWriter.toString());

						results.add(new SavedGameInfo(saveFile, headerJson, i18nTranslator, minimapPixmap));
					} catch (Exception e) {
						Logger.error("Error while reading " + saveFile.getAbsolutePath());
					}
				}
				for (File saveDirectory : userFileManager.getAllSaveDirectories()) {
					try {
						File headerJsonFile = saveDirectory.toPath().resolve("header.json").toFile();
						if (headerJsonFile.exists()) {
							JSONObject headerJson = JSON.parseObject(FileUtils.readFileToString(headerJsonFile));
							String settlementName = headerJson.getString("name");
							File minimapFile = saveDirectory.toPath().resolve(MINIMAP_ENTRY_NAME).toFile();
							Pixmap minimapPixmap = null;
							if (minimapFile.exists()) {
								minimapPixmap = new Pixmap(new FileHandle(minimapFile));
							}

							File bodyFile = saveDirectory.toPath().resolve(settlementName + ".json").toFile();
							if (bodyFile.exists()) {
								SavedGameInfo savedGameInfo = new SavedGameInfo(bodyFile, headerJson, i18nTranslator, minimapPixmap);
								savedGameInfo.setCompressed(false);
								results.add(savedGameInfo);
							}
						}
					} catch (Exception e) {
						Logger.error("Error while parsing save directory " + saveDirectory.getName());
					}
				}
				return BackgroundTaskResult.success(MessageType.SAVED_GAMES_PARSED, results);
			});
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SAVED_GAMES_PARSED: {
				List<SavedGameInfo> savedGames = (List<SavedGameInfo>) msg.extraInfo;
				if (GlobalSettings.DEV_MODE) {
					Logger.debug("Save file list refreshed");
				}
				this.refreshInProgress = false;
				clearSavedGames();
				for (SavedGameInfo savedGame : savedGames) {
					if (!bySettlementName.containsKey(savedGame.settlementName) ||
							bySettlementName.get(savedGame.settlementName).lastModifiedTime.isBefore(savedGame.lastModifiedTime)) {
						bySettlementName.put(savedGame.settlementName, savedGame);
					}
				}
				messageDispatcher.dispatchMessage(MessageType.SAVED_GAMES_LIST_UPDATED);
				return true;
			}
			case MessageType.SAVE_COMPLETED: {
				SavedGameInfo savedGameInfo = (SavedGameInfo) msg.extraInfo;
				if (savedGameInfo != null) {
					this.bySettlementName.put(savedGameInfo.settlementName, savedGameInfo);
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public void delete(SavedGameInfo savedGameInfo) {
		try {
			if (savedGameInfo.isCompressed()) {
				FileUtils.deleteQuietly(savedGameInfo.file);
			} else {
				FileUtils.deleteDirectory(savedGameInfo.file.getParentFile());
			}
		} catch (IOException e) {
			Logger.error("Error while deleting "  + savedGameInfo.settlementName, e);
		}
		remove(savedGameInfo);
	}


	private void clearSavedGames() {
		dispose();
		bySettlementName.clear();
	}
	private void remove(SavedGameInfo savedGameInfo) {
		savedGameInfo.dispose();
		bySettlementName.remove(savedGameInfo.settlementName);
	}

	public SavedGameInfo getByName(String settlementName) {
		return bySettlementName.get(settlementName);
	}

	public Optional<SavedGameInfo> getLatest() {
		return bySettlementName.values().stream()
				.sorted((o1, o2) -> o2.lastModifiedTime.compareTo(o1.lastModifiedTime))
				.findFirst();
	}

	public Collection<SavedGameInfo> getAll() {
		return bySettlementName.values();
	}

	public boolean hasSave() {
		return !bySettlementName.isEmpty();
	}

	@Override
	public void rebuildUI() {
		// Game clock display requires translation
		refresh();
	}

	@Override
	public void dispose() {
		this.bySettlementName.values().forEach(Disposable::dispose);
	}
}
