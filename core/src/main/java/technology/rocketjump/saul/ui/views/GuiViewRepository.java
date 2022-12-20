package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class GuiViewRepository implements Telegraph {

	private Map<GuiViewName, GuiView> byName = new EnumMap<>(GuiViewName.class);
	private final GameInteractionStateContainer gameInteractionStateContainer;

	@Inject
	public GuiViewRepository(Injector injector, MessageDispatcher messageDispatcher, GameInteractionStateContainer gameInteractionStateContainer) {
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		Reflections reflections = new Reflections(getClass().getPackageName(), new SubTypesScanner());
		Set<Class<? extends GuiView>> viewClasses = reflections.getSubTypesOf(GuiView.class);
		viewClasses.forEach(SaulGuiceModule::checkForSingleton);
		for (Class<? extends GuiView> viewClass : viewClasses) {
			add(injector.getInstance(viewClass));
		}

		messageDispatcher.addListener(this, MessageType.GUI_ROOM_TYPE_SELECTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_ROOM_TYPE_SELECTED -> {
				RoomType selectedRoomType = (RoomType) msg.extraInfo;
				gameInteractionStateContainer.setSelectedRoomType(selectedRoomType);
				GameInteractionMode.PLACE_ROOM.setRoomType(selectedRoomType);
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg);
		}
	}

	private void add(GuiView view) {
		if (byName.containsKey(view.getName())) {
			throw new RuntimeException("Duplicate GuiView name " + view.getName().name() + " attempting to be added to " + this.getClass().getSimpleName());
		}
		byName.put(view.getName(), view);
	}

	public GuiView getByName(GuiViewName name) {
		return byName.get(name);
	}
}
