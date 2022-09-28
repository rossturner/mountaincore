package technology.rocketjump.saul.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.LookupFurnitureMessage;

@Singleton
public class FurnitureEntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private GameContext gameContext;

	@Inject
	public FurnitureEntityMessageHandler(MessageDispatcher messageDispatcher, FurnitureTypeDictionary furnitureTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.furnitureTypeDictionary = furnitureTypeDictionary;

		messageDispatcher.addListener(this, MessageType.LOOKUP_FURNITURE_TYPE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.LOOKUP_FURNITURE_TYPE -> {
				handle((LookupFurnitureMessage)msg.extraInfo);
				return true;
			}
			default ->
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName() + ", " + msg);
		}
	}

	private void handle(LookupFurnitureMessage lookupFurnitureMessage) {
		lookupFurnitureMessage.callback.accept(furnitureTypeDictionary.getByName(lookupFurnitureMessage.furnitureTypeName));
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
