package technology.rocketjump.mountaincore.persistence;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.AssetDisposable;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.*;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.logging.CrashHandler;
import technology.rocketjump.mountaincore.mapping.minimap.MinimapPixmapGenerator;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.ThreadSafeMessageDispatcher;
import technology.rocketjump.mountaincore.messaging.async.BackgroundTaskManager;
import technology.rocketjump.mountaincore.messaging.async.BackgroundTaskResult;
import technology.rocketjump.mountaincore.messaging.async.ErrorType;
import technology.rocketjump.mountaincore.messaging.types.GameSaveMessage;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.modding.LocalModRepository;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;
import technology.rocketjump.mountaincore.persistence.model.InvaidSaveOrModsMissingException;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.screens.menus.MenuType;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.widgets.GameDialogDictionary;
import technology.rocketjump.mountaincore.ui.widgets.ModalDialog;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static technology.rocketjump.mountaincore.persistence.SavedGameStore.ARCHIVE_HEADER_ENTRY_NAME;

@Singleton
public class SavedGameMessageHandler implements Telegraph, GameContextAware, AssetDisposable {

	private final SavedGameDependentDictionaries relatedStores;
	private final MessageDispatcher messageDispatcher;
	private final UserFileManager userFileManager;
	private final BackgroundTaskManager backgroundTaskManager;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final GameContextRegister gameContextRegister;
	private final GameContextFactory gameContextFactory;
	private final LocalModRepository localModRepository;
	private final GameDialogDictionary gameDialogDictionary;
	private final ConstantsRepo constantsRepo;
	private final SavedGameStore savedGameStore;
	private final I18nTranslator i18nTranslator;
	private final SavedGameMigrationService savedGameMigrationService;
	private GameContext gameContext;

	private final ConcurrentLinkedQueue<CompletableFuture<?>> saveTasks = new ConcurrentLinkedQueue<>(); //should only ever be one

	private final AtomicBoolean disposed = new AtomicBoolean(false);//atomic booleans to ensure correctness across threads

	@Inject
	public SavedGameMessageHandler(SavedGameDependentDictionaries savedGameDependentDictionaries,
								   MessageDispatcher messageDispatcher, UserFileManager userFileManager,
								   BackgroundTaskManager backgroundTaskManager, PrimaryCameraWrapper primaryCameraWrapper,
								   GameContextRegister gameContextRegister, GameContextFactory gameContextFactory,
								   LocalModRepository localModRepository, GameDialogDictionary gameDialogDictionary,
								   ConstantsRepo constantsRepo, SavedGameStore savedGameStore, I18nTranslator i18nTranslator,
								   SavedGameMigrationService savedGameMigrationService) {
		this.relatedStores = savedGameDependentDictionaries;
		this.messageDispatcher = messageDispatcher;
		this.userFileManager = userFileManager;
		this.backgroundTaskManager = backgroundTaskManager;
		this.primaryCameraWrapper = primaryCameraWrapper;
		this.gameContextRegister = gameContextRegister;
		this.gameContextFactory = gameContextFactory;
		this.localModRepository = localModRepository;
		this.gameDialogDictionary = gameDialogDictionary;
		this.constantsRepo = constantsRepo;
		this.savedGameStore = savedGameStore;
		this.i18nTranslator = i18nTranslator;
		this.savedGameMigrationService = savedGameMigrationService;

		messageDispatcher.addListener(this, MessageType.REQUEST_SAVE);
		messageDispatcher.addListener(this, MessageType.PERFORM_LOAD);
		messageDispatcher.addListener(this, MessageType.PERFORM_SAVE);
		messageDispatcher.addListener(this, MessageType.TRIGGER_QUICKLOAD);
		messageDispatcher.addListener(this, MessageType.DAY_ELAPSED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REQUEST_SAVE: {
				triggerSaveProcess();
				return true;
			}
			case MessageType.DAY_ELAPSED: {
				// TODO configurable autosaving such as per-day, per-season, per-year, off
				triggerSaveProcess();
				return true;
			}
			case MessageType.PERFORM_SAVE: {
				GameSaveMessage message = (GameSaveMessage) msg.extraInfo;
				if (gameContext != null && !gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
					try {
						save(gameContext.getSettlementState().getSettlementName(), message.asynchronous);
					} catch (Exception e) {
						messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.WHILE_SAVING);
						CrashHandler.logCrash(e);
					}
				}
				return true;
			}
			case MessageType.PERFORM_LOAD: {
				SavedGameInfo savedGameInfo = (SavedGameInfo) msg.extraInfo;
				try {
					load(savedGameInfo);
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
				} catch (FileNotFoundException e) {
					// Mostly ignoring file not found errors
					Logger.warn(e.getMessage());
				} catch (InvaidSaveOrModsMissingException e) {
					ModalDialog dialog = gameDialogDictionary.createModsMissingSaveExceptionDialog(e.missingModNames);
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
					Logger.warn(e);
				} catch (InvalidSaveException e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.INVALID_SAVE_FILE);
					Logger.warn(e);
				} catch (Exception e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.WHILE_LOADING);
					CrashHandler.logCrash(e);
				}
				messageDispatcher.dispatchMessage(0.05f, MessageType.HIDE_AUTOSAVE_PROMPT);
				return true;
			}
			case MessageType.TRIGGER_QUICKLOAD: {
				PersistenceCallback callback = (PersistenceCallback) msg.extraInfo;
				Optional<SavedGameInfo> latest = savedGameStore.getLatest();
				boolean loadSuccess = false;
				try {
					if (latest.isPresent()) {
						load(latest.get());
						messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
						loadSuccess = true;
					}
				} catch (FileNotFoundException e) {
					// Mostly ignoring file not found errors
					Logger.warn(e.getMessage());
				} catch (InvaidSaveOrModsMissingException e) {
					ModalDialog dialog = gameDialogDictionary.createModsMissingSaveExceptionDialog(e.missingModNames);
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
					Logger.warn(e);
				} catch (InvalidSaveException e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.INVALID_SAVE_FILE);
					Logger.warn(e);
				} catch (Exception e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.WHILE_LOADING);
					CrashHandler.logCrash(e);
				} finally {
					if (callback != null) {
						callback.gameLoadAttempt(loadSuccess);
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void triggerSaveProcess() {
		messageDispatcher.dispatchMessage(MessageType.SHOW_AUTOSAVE_PROMPT);
		messageDispatcher.dispatchMessage(0.01f, MessageType.PERFORM_SAVE, new GameSaveMessage(true));
		messageDispatcher.dispatchMessage(0.05f, MessageType.HIDE_AUTOSAVE_PROMPT);
	}

	private boolean isSaving() {
		return !(saveTasks.stream().allMatch(CompletableFuture::isDone));
	}

	public synchronized void save(String settlementName, boolean asynchronous) throws Exception {
		if (!asynchronous && isSaving()) {
			// We are trying to quit while a background save is already in progress
			for (CompletableFuture<?> saveTask : saveTasks) {
				saveTask.join(); //wait for save tasks to complete
			}

			return;
		}

		if (isSaving()) { //save in progress, return immediately
			return;
		}

		if (disposed.get()) {
			return;
		}

		String saveFileName = toAlphanumeric(settlementName);
		backgroundTaskManager.waitForOutstandingTasks();

		SavedGameStateHolder stateHolder = new SavedGameStateHolder();

		for (GameMaterial dynamicMaterial : gameContext.getDynamicallyCreatedMaterialsByCombinedId().values()) {
			dynamicMaterial.writeTo(stateHolder);
		}
		for (Job job : gameContext.getJobs().values()) {
			job.writeTo(stateHolder);
		}
		for (Entity entity : gameContext.getEntities().values()) {
			entity.writeTo(stateHolder);
			stateHolder.entityIdsToLoad.add(entity.getId());
		}
		for (Construction construction : gameContext.getConstructions().values()) {
			construction.writeTo(stateHolder);
		}
		for (Room room : gameContext.getRooms().values()) {
			room.writeTo(stateHolder);
		}
		for (Squad squad : gameContext.getSquads().values()) {
			squad.writeTo(stateHolder);
		}
		TiledMap map = gameContext.getAreaMap();
		stateHolder.mapJson.put("seed", map.getSeed());
		stateHolder.mapJson.put("width", map.getWidth());
		stateHolder.mapJson.put("height", map.getHeight());
		stateHolder.mapJson.put("embarkPoint", JSONUtils.toJSON(map.getEmbarkPoint()));
		stateHolder.mapJson.put("numRegions", map.getNumRegions());
		stateHolder.mapJson.put("defaultFloor", map.getDefaultFloor().getFloorTypeName());
		stateHolder.mapJson.put("defaultFloorMaterial", map.getDefaultFloorMaterial().getMaterialName());

		for (int y = 0; y < map.getHeight(); y++) {
			for (int x = 0; x < map.getWidth(); x++) {
				map.getTile(x, y).writeTo(stateHolder);
			}
		}
		for (int y = 0; y <= map.getHeight(); y++) {
			for (int x = 0; x <= map.getWidth(); x++) {
				map.getVertex(x, y).writeTo(stateHolder);
			}
		}

		gameContext.getMapEnvironment().writeTo(stateHolder.mapEnvironmentJson, stateHolder);
		gameContext.getSettlementState().writeTo(stateHolder);
		((ThreadSafeMessageDispatcher) messageDispatcher).writeTo(stateHolder);
		gameContext.getGameClock().writeTo(stateHolder);
		stateHolder.setSequentialIdPointer(SequentialIdGenerator.lastId());
		primaryCameraWrapper.writeTo(stateHolder);
		stateHolder.setActiveMods(localModRepository.getActiveMods());

		JSONObject fileContents = stateHolder.toCombinedJson();

		Supplier<BackgroundTaskResult> writeToDisk = () -> {
			SavedGameInfo justSavedInfo = null;
			try {
				JSONObject headerJson = produceHeaderFrom(fileContents);

				File saveFile = userFileManager.getOrCreateSaveFile(saveFileName);
				File tempMainFile = userFileManager.getOrCreateSaveFile("body.temp");
				File tempHeaderFile = userFileManager.getOrCreateSaveFile("header.temp");
				File tempMinimapTextureFile = userFileManager.getOrCreateSaveFile(SavedGameStore.MINIMAP_ENTRY_NAME);

				writeJsonToFile(fileContents, tempMainFile);
				writeJsonToFile(headerJson, tempHeaderFile);

				Pixmap minimapPixmap = MinimapPixmapGenerator.generateFrom(gameContext.getAreaMap());
				PixmapIO.writePNG(new FileHandle(tempMinimapTextureFile), minimapPixmap);

				OutputStream archiveStream = new FileOutputStream(saveFile);
				ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);

				addArchiveEntry(tempHeaderFile, ARCHIVE_HEADER_ENTRY_NAME, archive);
				addArchiveEntry(tempMainFile, saveFileName + ".json", archive);
				addArchiveEntry(tempMinimapTextureFile, SavedGameStore.MINIMAP_ENTRY_NAME, archive);

				archive.finish();
				IOUtils.closeQuietly(archiveStream);

				tempMainFile.delete();
				tempHeaderFile.delete();
				tempMinimapTextureFile.delete();
				justSavedInfo = new SavedGameInfo(saveFile, headerJson, i18nTranslator, minimapPixmap);//do not dispose minimapPixmap, it will be handled elsewhere
				return BackgroundTaskResult.success();
			} catch (Exception e) {
				CrashHandler.logCrash(e);
				return BackgroundTaskResult.error(ErrorType.WHILE_SAVING);
			} finally {
				messageDispatcher.dispatchMessage(MessageType.SAVE_COMPLETED, justSavedInfo);
			}
		};

		final CompletableFuture<BackgroundTaskResult> saveFuture = backgroundTaskManager.runTask(writeToDisk);
		saveTasks.add(saveFuture);
		final CompletableFuture<?> onComplete = saveFuture.thenRun(() -> saveTasks.remove(saveFuture));

		if (!asynchronous) {
			onComplete.join();
		}
	}

	private String toAlphanumeric(String settlementName) {
		StringBuilder result = new StringBuilder();
		for (char c : settlementName.toCharArray()) {
			if (Character.isLetterOrDigit(c)) {
				result.append(c);
			}
		}
		return result.toString();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void writeJsonToFile(JSONObject json, File targetFile) throws IOException {
		FileUtils.deleteQuietly(targetFile);
		targetFile.createNewFile();
		BufferedWriter tempFileWriter = new BufferedWriter(new FileWriter(targetFile));
		try {
			JSON.writeJSONString(tempFileWriter, json,
					SerializerFeature.DisableCircularReferenceDetect);
		} finally {
			IOUtils.closeQuietly(tempFileWriter);
		}
	}

	private JSONObject produceHeaderFrom(JSONObject mainJsonContent) {
		JSONObject headerJson = new JSONObject(true);
		headerJson.put("name", mainJsonContent.getJSONObject("settlementState").getString("settlementName"));
		headerJson.put("version", mainJsonContent.getString("version"));
		headerJson.put("mods", mainJsonContent.getJSONObject("mods"));
		headerJson.put("clock", mainJsonContent.getJSONObject("clock"));
		headerJson.put("peacefulMode", mainJsonContent.getJSONObject("settlementState").getBooleanValue("peacefulMode"));
		JSONObject map = mainJsonContent.getJSONObject("map");
		if (map != null) {
			headerJson.put("seed", map.getLong("seed"));
		}
		return headerJson;
	}

	private void addArchiveEntry(File sourceFile, String entryName, ArchiveOutputStream archive) throws IOException {
		ZipArchiveEntry headerEntry = new ZipArchiveEntry(entryName);
		archive.putArchiveEntry(headerEntry);
		BufferedInputStream headerInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
		IOUtils.copy(headerInputStream, archive);
		IOUtils.closeQuietly(headerInputStream);
		archive.closeArchiveEntry();
	}


	public void load(SavedGameInfo savedGameInfo) throws IOException, InvalidSaveException, ArchiveException {
		if (isSaving()) {
			return;
		}
		File saveFile = userFileManager.getSaveFile(savedGameInfo);
		if (saveFile == null) {
			throw new FileNotFoundException("Save file does not exist: " + savedGameInfo.file.getName() + ".save");
		}

		String jsonString;
		if (savedGameInfo.isCompressed()) {
			InputStream archiveStream = new FileInputStream(saveFile);
			ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, archiveStream);
			ArchiveEntry archiveEntry = archive.getNextEntry();
			StringWriter stringWriter = new StringWriter();
			while (archiveEntry != null && archiveEntry.getName().equals(ARCHIVE_HEADER_ENTRY_NAME)) {
				archiveEntry = archive.getNextEntry();
			}

			if (archiveEntry == null) {
				throw new IOException("Could not find main entry in " + saveFile.getName());
			}

			IOUtils.copy(archive, stringWriter);
			jsonString = stringWriter.toString();
			IOUtils.closeQuietly(stringWriter);
			IOUtils.closeQuietly(archive);
			IOUtils.closeQuietly(archiveStream);
		} else {
			jsonString = FileUtils.readFileToString(savedGameInfo.file);
		}

		JSONObject storedJson = JSON.parseObject(jsonString);
		storedJson = savedGameMigrationService.migrate(savedGameInfo, storedJson);
		SavedGameStateHolder stateHolder = new SavedGameStateHolder(storedJson);
		try {
			stateHolder.jsonToObjects(relatedStores);
		} catch (InvalidSaveException e) {
			List<String> missingModNames = getMissingModNames(stateHolder);
			if (!missingModNames.isEmpty()) {
				throw new InvaidSaveOrModsMissingException(missingModNames, e.getMessage());
			} else {
				throw e;
			}
		}

		GameContext gameContext = gameContextFactory.create(stateHolder);
		// Need constant before initialising entities
		gameContext.setConstantsRepo(constantsRepo);
		for (Entity entity : stateHolder.entities.values()) {
			entity.init(messageDispatcher, gameContext);
		}
		for (CreatureGroup creatureGroup : stateHolder.creatureGroups.values()) {
			creatureGroup.init(gameContext);
		}
		TiledMap map = gameContext.getAreaMap();
		for (int y = 0; y < map.getHeight(); y++) {
			for (int x = 0; x < map.getWidth(); x++) {
				MapTile mapTile = map.getTile(x, y);
				for (Long entityId : mapTile.getEntityIds()) {
					Entity entity = stateHolder.entities.get(entityId);
					mapTile.addEntity(entity);
				}
			}
		}


		((ThreadSafeMessageDispatcher) messageDispatcher).readFrom(null, stateHolder, relatedStores);

		gameContextRegister.setNewContext(gameContext);
		primaryCameraWrapper.readFrom(stateHolder.cameraJson, stateHolder, relatedStores);

		List<String> missingMods = getMissingModNames(stateHolder);
		if (!missingMods.isEmpty()) {
			ModalDialog modsMissingDialog = gameDialogDictionary.createModsMissingDialog(missingMods);
			messageDispatcher.dispatchMessage(0.1f, MessageType.SHOW_DIALOG, modsMissingDialog);
		}
	}

	private List<String> getMissingModNames(SavedGameStateHolder savedGameStateHolder) {
		List<ParsedMod> currentlyActiveMods = localModRepository.getActiveMods();
		List<String> currentlyActiveModNames = currentlyActiveMods.stream().map(mod -> mod.getInfo().getName()).collect(Collectors.toList());
		Set<String> saveFileModNames = savedGameStateHolder.activeModNamesToVersions.keySet();

		return saveFileModNames.stream()
				.filter(saveModName -> !currentlyActiveModNames.contains(saveModName))
				.collect(Collectors.toList());

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.gameContext = null;
	}

	@Override
	public void dispose() {
		disposed.set(true);
	}
}
