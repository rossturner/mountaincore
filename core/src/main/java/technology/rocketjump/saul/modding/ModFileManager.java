package technology.rocketjump.saul.modding;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.messaging.MessageType;

@Singleton
public class ModFileManager implements Telegraph {

	private final MessageDispatcher messageDispatcher;
	private boolean refreshInProgress;

	public ModFileManager(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.REFRESH_MOD_FILES);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REFRESH_MOD_FILES -> {

			}
			default -> Logger.error("Unexpected message type handled: " + msg.message);
		}
		return true;
	}
}
