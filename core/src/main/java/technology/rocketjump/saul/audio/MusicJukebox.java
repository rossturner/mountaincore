package technology.rocketjump.saul.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.AssetDisposable;
import technology.rocketjump.saul.audio.model.JukeboxState;
import technology.rocketjump.saul.combat.CombatTracker;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.InvasionCreatureGroup;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;

import java.util.*;

import static technology.rocketjump.saul.audio.model.JukeboxState.INVASION_STINGER;
import static technology.rocketjump.saul.audio.model.JukeboxState.PEACEFUL;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.MUSIC_VOLUME;

@Singleton
public class MusicJukebox implements Telegraph, AssetDisposable, GameContextAware {

	public static final String DEFAULT_VOLUME_AS_STRING = "0.24";
	private static final float VOLUME_CHANGE_IN_SECONDS = 5f;
	private static final float DELAY_BEFORE_EXITING_COMBAT = CombatTracker.COMBAT_ROUND_DURATION * 1.5f;
	private final UserPreferences userPreferences;
	private float volume;
	private boolean stopped;
	private Deque<FileHandle> peacefulPlaylist = new ArrayDeque<>();
	private List<FileHandle> peacefulFileList = new ArrayList<>();
	private List<FileHandle> skirmishFileList = new ArrayList<>();
	private List<FileHandle> invasionFileList = new ArrayList<>();
	private List<FileHandle> invasionStingerFileList = new ArrayList<>();
	private Music peacefulTrack;
	private Music skirmishTrack;
	private Music invasionStinger;
	private Music invasionTrack;
	private JukeboxState currentState = JukeboxState.PEACEFUL;
	private float timeInCurrentState = 0f;
	private boolean shutdown;
	private final CombatTracker combatTracker;
	private InvasionCreatureGroup currentInvasion;
	private Random random = new RandomXS128();
	private boolean gamePaused;

	@Inject
	public MusicJukebox(MessageDispatcher messageDispatcher, UserPreferences userPreferences, CombatTracker combatTracker) {
		this.userPreferences = userPreferences;
		this.combatTracker = combatTracker;

		String volumeString = userPreferences.getPreference(UserPreferences.PreferenceKey.MUSIC_VOLUME, DEFAULT_VOLUME_AS_STRING);
		this.volume = SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER * Float.valueOf(volumeString);
		if (this.volume < 0.01f) {
			this.stopped = true;
		}

		FileHandle peacefulMusicDir = new FileHandle("assets/music/peaceful");

		FileHandle mainMusic = null;
		for (FileHandle fileHandle : peacefulMusicDir.list()) {
			if (fileHandle.extension().equals("ogg")) {
				peacefulFileList.add(fileHandle);
				if (fileHandle.name().contains("King under the Mountain")) {
					mainMusic = fileHandle;
				}
			}
		}

		for (FileHandle fileHandle : new FileHandle("assets/music/skirmish").list()) {
			if (fileHandle.extension().equals("ogg")) {
				skirmishFileList.add(fileHandle);
			}
		}
		for (FileHandle fileHandle : new FileHandle("assets/music/invasion_stinger").list()) {
			if (fileHandle.extension().equals("ogg")) {
				invasionStingerFileList.add(fileHandle);
			}
		}
		for (FileHandle fileHandle : new FileHandle("assets/music/invasion").list()) {
			if (fileHandle.extension().equals("ogg")) {
				invasionFileList.add(fileHandle);
			}
		}

		if (mainMusic != null) {
			peacefulFileList.remove(mainMusic);
		}

		Collections.shuffle(peacefulFileList);
		if (mainMusic != null) {
			peacefulPlaylist.add(mainMusic);
		}
		peacefulPlaylist.addAll(peacefulFileList);

		messageDispatcher.addListener(this, MessageType.GUI_CHANGE_MUSIC_VOLUME);
		messageDispatcher.addListener(this, MessageType.GAME_PAUSED);
		messageDispatcher.addListener(this, MessageType.INVASION_ABOUT_TO_BEGIN);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_CHANGE_MUSIC_VOLUME: {
				Float newVolume = (Float)msg.extraInfo;
				this.volume = SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER * newVolume;
				for (Music track : Arrays.asList(peacefulTrack, skirmishTrack, invasionStinger, invasionTrack)) {
					if (track != null) {
						track.setVolume(volume);
					}
				}
				userPreferences.setPreference(MUSIC_VOLUME, String.valueOf(newVolume));

				if (this.stopped) {
					if (this.volume > 0.01f) {
						this.stopped = false;
					}
				} else {
					if (this.volume < 0.01f) {
						this.stopped = true;
					}
				}
				return true;
			}
			case MessageType.GAME_PAUSED: {
				this.gamePaused = (boolean) msg.extraInfo;
				return true;
			}
			case MessageType.INVASION_ABOUT_TO_BEGIN: {
				this.currentState = INVASION_STINGER;
				Entity invasionEntity = (Entity) msg.extraInfo;
				if (invasionEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
						creatureBehaviour.getCreatureGroup() != null && creatureBehaviour.getCreatureGroup() instanceof InvasionCreatureGroup invasionGroup) {
					this.currentInvasion = invasionGroup;
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	// Note: Not using Updatable interface as this should run on all screens
	public void update() {
		if (shutdown || stopped) {
			return;
		}

		switch (currentState) {
			case PEACEFUL -> {
				if (!combatTracker.getEntitiesInCombat().isEmpty()) {
					this.currentState = JukeboxState.SKIRMISH_COMBAT;
					update();
				}

				if (skirmishTrack != null) {
					fadeOut(skirmishTrack);
					if (skirmishTrack.getVolume() < 0.01f) {
						disposeTrack(skirmishTrack);
						skirmishTrack = null;
					}
				}

				if (invasionTrack != null) {
					fadeOut(invasionTrack);
					if (invasionTrack.getVolume() < 0.01f) {
						disposeTrack(invasionTrack);
						invasionTrack = null;
					}
				}


				if (skirmishTrack == null && invasionTrack == null && (peacefulTrack == null || !peacefulTrack.isPlaying())) {
					startNewPeacefulTrack();
				}
			}
			case SKIRMISH_COMBAT -> {
				// If still playing peaceful music, quickly fade it out
				// if peaceful music stopped, play skirmish music


				if (peacefulTrack != null) {
					fadeOut(peacefulTrack);
					if (peacefulTrack.getVolume() < 0.01f) {
						disposeTrack(peacefulTrack);
						peacefulTrack = null;
					}
				}

				if (invasionTrack != null) {
					fadeOut(invasionTrack);
					if (invasionTrack.getVolume() < 0.01f) {
						disposeTrack(invasionTrack);
						invasionTrack = null;
					}
				}

				if (peacefulTrack == null && (skirmishTrack == null || !skirmishTrack.isPlaying())) {
					if (skirmishTrack == null) {
						loadSkirmishTrack();
					}
					skirmishTrack.play();
				}

				if (combatTracker.getEntitiesInCombat().isEmpty()) {
					this.currentState = JukeboxState.EXITING_COMBAT;
					this.timeInCurrentState = 0f;
				}
			}
			case INVASION_STINGER -> {
				if (peacefulTrack != null) {
					fadeOut(peacefulTrack);
					if (peacefulTrack.getVolume() < 0.01f) {
						disposeTrack(peacefulTrack);
						peacefulTrack = null;
					}
					return;
				}
				if (skirmishTrack != null) {
					fadeOut(skirmishTrack);
					if (skirmishTrack.getVolume() < 0.01f) {
						disposeTrack(skirmishTrack);
						skirmishTrack = null;
					}
					return;
				}

				if (invasionStinger == null) {
					loadInvasionStinger();
					invasionStinger.play();
				}

				if (!invasionStinger.isPlaying()) {
					invasionStinger = null;
					this.currentState = JukeboxState.INVASION_IN_PROGRESS;
				}
			}
			case INVASION_IN_PROGRESS -> {
				if (invasionTrack == null) {
					loadInvasionTrack();
				}

				invasionTrack.play();

				if (currentInvasion.getMemberIds().isEmpty()) {
					this.currentState = JukeboxState.PEACEFUL;
				}
			}
			case EXITING_COMBAT -> {
				if (!gamePaused) {
					timeInCurrentState += Gdx.graphics.getDeltaTime();
				}

				if (!combatTracker.getEntitiesInCombat().isEmpty()) {
					this.currentState = JukeboxState.SKIRMISH_COMBAT;
				} else if (timeInCurrentState > DELAY_BEFORE_EXITING_COMBAT) {
					this.currentState = JukeboxState.PEACEFUL;
				}
			}
		}
	}

	private void fadeOut(Music track) {
		float trackVolume = track.getVolume();
		trackVolume -= (Gdx.graphics.getDeltaTime() / VOLUME_CHANGE_IN_SECONDS);
		trackVolume = Math.max(trackVolume, 0);
		track.setVolume(trackVolume);
	}

	private void startNewPeacefulTrack() {
		if (peacefulTrack != null) {
			peacefulTrack.dispose();
		}
		if (peacefulPlaylist.isEmpty()) {
			peacefulPlaylist.addAll(peacefulFileList);
		}
		peacefulTrack = Gdx.audio.newMusic(peacefulPlaylist.pop());
		peacefulTrack.setVolume(volume);
		peacefulTrack.play();
	}

	private void loadSkirmishTrack() {
		this.skirmishTrack = Gdx.audio.newMusic(skirmishFileList.get(random.nextInt(skirmishFileList.size())));
		this.skirmishTrack.setVolume(volume);
	}

	private void loadInvasionStinger() {
		this.invasionStinger = Gdx.audio.newMusic(invasionStingerFileList.get(random.nextInt(invasionStingerFileList.size())));
		this.invasionStinger.setVolume(volume);
	}

	private void loadInvasionTrack() {
		this.invasionTrack = Gdx.audio.newMusic(invasionFileList.get(random.nextInt(invasionFileList.size())));
		this.invasionTrack.setVolume(volume);
	}

	@Override
	public void dispose() {
		disposeTrack(peacefulTrack);
		peacefulTrack = null;

		disposeTrack(skirmishTrack);
		skirmishTrack = null;

		disposeTrack(invasionStinger);
		invasionStinger = null;

		disposeTrack(invasionTrack);
		invasionTrack = null;

		this.shutdown = true;
	}

	private void disposeTrack(Music musicTrack) {
		if (musicTrack != null) {
			musicTrack.stop();
			musicTrack.dispose();
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.currentState = PEACEFUL;
		if (gameContext != null && gameContext.getEntities() != null) {
			for (Entity entity : gameContext.getEntities().values()) {
				if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
					creatureBehaviour.getCreatureGroup() != null && creatureBehaviour.getCreatureGroup() instanceof InvasionCreatureGroup invasionCreatureGroup) {
					this.currentInvasion = invasionCreatureGroup;
					this.currentState = INVASION_STINGER;
					break;
				}
			}
		}
	}

	@Override
	public void clearContextRelatedState() {
		this.currentInvasion = null;
	}
}
