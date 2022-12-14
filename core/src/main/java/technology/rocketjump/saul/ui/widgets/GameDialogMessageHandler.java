package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.messaging.MessageType.DIALOG_HIDDEN;
import static technology.rocketjump.saul.messaging.MessageType.DIALOG_SHOWN;

@Singleton
public class GameDialogMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;

	private Map<Long, GameDialog> displayedDialogs = new HashMap<>();

	@Inject
	public GameDialogMessageHandler(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, DIALOG_SHOWN);
		messageDispatcher.addListener(this, DIALOG_HIDDEN);
	}

	public List<GameDialog> getDisplayedDialogs() {
		return new ArrayList<>(displayedDialogs.values());
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case DIALOG_SHOWN -> {
				GameDialog gameDialog = (GameDialog) msg.extraInfo;
				displayedDialogs.put(gameDialog.getId(), gameDialog);
				return true;
			}
			case DIALOG_HIDDEN -> {
				GameDialog gameDialog = (GameDialog) msg.extraInfo;
				displayedDialogs.remove(gameDialog.getId());
				return true;
			}
			default -> {
				Logger.error("Unexpected message type {} received by {}", msg.message, this.getClass().getSimpleName());
				return false;
			}
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {

	}

	@Override
	public void clearContextRelatedState() {
		displayedDialogs.clear();
	}
}
