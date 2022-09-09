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
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;

import java.util.*;

import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.MUSIC_VOLUME;

@Singleton
public class MusicJukebox implements Telegraph, AssetDisposable {

	public static final String DEFAULT_VOLUME_AS_STRING = "0.24";
	private static final float VOLUME_CHANGE_IN_SECONDS = 5f;
	private static final float DELAY_BEFORE_EXITING_COMBAT = CombatTracker.COMBAT_ROUND_DURATION * 1.5f;
	private final UserPreferences userPreferences;
	private float volume;
	private boolean stopped;
	private Deque<FileHandle> peacefulPlaylist = new ArrayDeque<>();
	private List<FileHandle> peacefulFileList = new ArrayList<>();
	private List<FileHandle> skirmishFileList = new ArrayList<>();
	private Music peacefulTrack;
	private Music skirmishTrack;
	private JukeboxState currentState = JukeboxState.PEACEFUL;
	private float timeInCurrentState = 0f;
	private boolean shutdown;
	private final CombatTracker combatTracker;
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
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_CHANGE_MUSIC_VOLUME: {
				Float newVolume = (Float)msg.extraInfo;
				this.volume = SoundEffectManager.GLOBAL_VOLUME_MULTIPLIER * newVolume;
				if (peacefulTrack != null) {
					peacefulTrack.setVolume(volume);
				}
				if (skirmishTrack != null) {
					skirmishTrack.setVolume(volume);
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


				if (skirmishTrack == null && (peacefulTrack == null || !peacefulTrack.isPlaying())) {
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

	@Override
	public void dispose() {
		disposeTrack(peacefulTrack);
		peacefulTrack = null;

		disposeTrack(skirmishTrack);
		skirmishTrack = null;

		this.shutdown = true;
	}

	private void disposeTrack(Music musicTrack) {
		if (musicTrack != null) {
			musicTrack.stop();
			musicTrack.dispose();
		}
	}
}
