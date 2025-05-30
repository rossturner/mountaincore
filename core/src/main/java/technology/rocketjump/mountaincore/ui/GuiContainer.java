package technology.rocketjump.mountaincore.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.InfoWindow;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipTable;
import technology.rocketjump.mountaincore.ui.views.*;
import technology.rocketjump.mountaincore.ui.views.debug.DebugGuiView;
import technology.rocketjump.mountaincore.ui.widgets.GameDialog;
import technology.rocketjump.mountaincore.ui.widgets.GameDialogMessageHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class GuiContainer implements Telegraph, GameContextAware {

	private final InfoWindow infoWindow;
	private final Table containerTable;
	private final Table upperRightContainerTable;
	private final Table upperLeftContainerTable;
	private final Table lowerRightContainerTable;
	private final Table timeAndDateContainerTable;
	private final Table hintContainerTable;
	private final Table debugContainerTable;
	private final Table resourceOverviewContainerTable;
	private final Table notificationTable;
	private final Table minimapContainerTable;
	private final MessageDispatcher messageDispatcher;
	private final GameInteractionStateContainer interactionStateContainer;
	private final UserPreferences userPreferences;
	private final TimeDateGuiView timeDateGuiView;
	private final HintGuiView hintGuiView;
	private final ResourceOverview resourceOverview;
	private final DebugGuiView debugGuiView;
	private final NotificationGuiView notificationGuiView;
	private final MinimapGuiView minimapGuiView;
	private final ExtendViewport viewport;
	private Stage primaryStage;
	private StageAreaOnlyInputHandler primaryStageInputHandler;

	private final GuiViewRepository guiViewRepository;
	private GuiViewName currentViewName;
	private GuiView currentView;
	private float timeSinceLastUpdate = 0;
	private Set<GuiArea> hiddenGuiAreas = new HashSet<>();
	private GameContext gameContext;

	@Inject
	public GuiContainer(MessageDispatcher messageDispatcher, GameInteractionStateContainer interactionStateContainer,
						GuiViewRepository guiViewRepository, TimeDateGuiView timeDateGuiView,
						InfoWindow infoWindow, HintGuiView hintGuiView, ResourceOverview resourceOverview,
						DebugGuiView debugGuiView, NotificationGuiView notificationGuiView,
						GameDialogMessageHandler gameDialogMessageHandler,
						MinimapGuiView minimapGuiView, UserPreferences userPreferences) {
		this.infoWindow = infoWindow;
		this.hintGuiView = hintGuiView;
		this.resourceOverview = resourceOverview;
		this.debugGuiView = debugGuiView;
		this.notificationGuiView = notificationGuiView;
		this.timeDateGuiView = timeDateGuiView;
		this.minimapGuiView = minimapGuiView;
		this.interactionStateContainer = interactionStateContainer;
		this.userPreferences = userPreferences;

		Vector2 viewportDimensions = ViewportUtils.scaledViewportDimensions(userPreferences);
		viewport = new ExtendViewport(viewportDimensions.x, viewportDimensions.y);

		primaryStage = new Stage(viewport);
		primaryStageInputHandler = new StageAreaOnlyInputHandler(primaryStage, interactionStateContainer, gameDialogMessageHandler);

		containerTable = new Table();
		containerTable.pad(10f); // Table edge padding
		containerTable.left().bottom();


		this.messageDispatcher = messageDispatcher;
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_VIEW);
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_INTERACTION_MODE);
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_VIEW_MODE);
		messageDispatcher.addListener(this, MessageType.GUI_CANCEL_CURRENT_VIEW);
		messageDispatcher.addListener(this, MessageType.GUI_CANCEL_CURRENT_VIEW_OR_GO_TO_MAIN_MENU);
		messageDispatcher.addListener(this, MessageType.GUI_REMOVE_ALL_TOOLTIPS);
		messageDispatcher.addListener(this, MessageType.GUI_SCALE_CHANGED);
		messageDispatcher.addListener(this, MessageType.GUI_HIDE_AREA);
		messageDispatcher.addListener(this, MessageType.GUI_SHOW_AREA);

		this.guiViewRepository = guiViewRepository;
		switchView(GuiViewName.DEFAULT_MENU);

		timeAndDateContainerTable = new Table();
		timeAndDateContainerTable.right().top();

		hintContainerTable = new Table();
		hintContainerTable.left().top();
		debugContainerTable = new Table();
		debugContainerTable.left().top();
		resourceOverviewContainerTable = new Table();
		resourceOverviewContainerTable.left().top();

		notificationTable = new Table();
		minimapContainerTable = new Table();

		upperRightContainerTable = new Table();
		upperRightContainerTable.right().top();
		upperRightContainerTable.add(timeAndDateContainerTable).top().right().row();
		upperRightContainerTable.add(notificationTable).right();
		upperRightContainerTable.setFillParent(true);

		upperLeftContainerTable = new Table();
		upperLeftContainerTable.left().top();
		upperLeftContainerTable.add(hintContainerTable).top().left().pad(10).row();
		upperLeftContainerTable.add(debugContainerTable).top().left().pad(10).row();
		upperLeftContainerTable.setFillParent(true);

		lowerRightContainerTable = new Table();
		lowerRightContainerTable.right().bottom();
		lowerRightContainerTable.setFillParent(true);

		Table leftHandTable = new Table();
		leftHandTable.setFillParent(true);
		leftHandTable.left();
		leftHandTable.add(resourceOverviewContainerTable).padTop(35).left().growY().row();
		leftHandTable.add(containerTable).left();


		primaryStage.addActor(leftHandTable);
		primaryStage.addActor(lowerRightContainerTable);
		primaryStage.addActor(upperRightContainerTable);
		primaryStage.addActor(upperLeftContainerTable);
		minimapGuiView.populate(minimapContainerTable);
		timeDateGuiView.populate(timeAndDateContainerTable);
		timeAndDateContainerTable.row();
		notificationGuiView.populate(timeAndDateContainerTable);
		hintGuiView.populate(hintContainerTable);
		debugGuiView.populate(debugContainerTable);
		debugGuiView.update();
		resourceOverview.populate(resourceOverviewContainerTable);
		resourceOverview.update();
	}

	public void rebuildUI() {
		timeDateGuiView.setHiddenGuiAreas(hiddenGuiAreas);
		timeDateGuiView.reset(gameContext);

		lowerRightContainerTable.clearChildren();
		if (!hiddenGuiAreas.contains(GuiArea.MINIMAP)) {
			lowerRightContainerTable.add(minimapContainerTable).bottom().right().row();
		}

		switchView(currentViewName);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_SWITCH_VIEW: {
				GuiViewName targetView = (GuiViewName)msg.extraInfo;
				switchView(targetView);
				return true;
			}
			case MessageType.GUI_SWITCH_INTERACTION_MODE: {
				GameInteractionMode targetMode = (GameInteractionMode)msg.extraInfo;
				messageDispatcher.dispatchMessage(MessageType.SET_INTERACTION_MODE_CURSOR, targetMode.cursor);
				interactionStateContainer.setInteractionMode(targetMode);
				messageDispatcher.dispatchMessage(MessageType.INTERACTION_MODE_CHANGED);
				return true;
			}
			case MessageType.GUI_SWITCH_VIEW_MODE: {
				GameViewMode targetMode = (GameViewMode)msg.extraInfo;
				interactionStateContainer.setGameViewMode(targetMode);
				messageDispatcher.dispatchMessage(MessageType.GUI_VIEW_MODE_CHANGED);
				return true;
			}
			case MessageType.GUI_CANCEL_CURRENT_VIEW: {
				GuiViewName parentViewName = currentView.getParentViewName();
				if (parentViewName != null) {
					switchView(parentViewName);
				}
				return true;
			}
			case MessageType.GUI_SCALE_CHANGED: {
				Vector2 updatedDimensions = ViewportUtils.scaledViewportDimensions(userPreferences);
				viewport.setMinWorldWidth(updatedDimensions.x);
				viewport.setMinWorldHeight(updatedDimensions.y);
				onResize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				return true;
			}
			case MessageType.GUI_CANCEL_CURRENT_VIEW_OR_GO_TO_MAIN_MENU: {
				GuiViewName parentViewName = currentView.getParentViewName();
				if (currentViewName.equals(GuiViewName.DEFAULT_MENU) || currentViewName.equals(GuiViewName.SELECT_STARTING_LOCATION)) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_MENU");
				} else if (parentViewName != null) {
					switchView(parentViewName);
				} else {
					Logger.error("Don't know how to cancel current view from " + currentViewName.name());
				}
				return true;
			}
			case MessageType.GUI_REMOVE_ALL_TOOLTIPS: {
				removeAllTooltips();
				return true;
			}
			case MessageType.GUI_HIDE_AREA: {
				GuiArea toHide = (GuiArea) msg.extraInfo;
				if (!hiddenGuiAreas.contains(toHide)) {
					hiddenGuiAreas.add(toHide);
					rebuildUI();
				}
				return true;
			}
			case MessageType.GUI_SHOW_AREA: {
				GuiArea toShow = (GuiArea) msg.extraInfo;
				if (hiddenGuiAreas.contains(toShow)) {
					hiddenGuiAreas.remove(toShow);
					rebuildUI();
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public void update(float deltaTime) {
		timeDateGuiView.update();
		minimapGuiView.update();
		hintGuiView.update();
		notificationGuiView.update();
		resourceOverview.toggleVisibility(gameContext, currentViewName, hiddenGuiAreas);
		resourceOverview.update();

		primaryStage.act(deltaTime);

		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > 1f) {
			timeSinceLastUpdate = 0;
			if (currentView != null) {
				currentView.update();
			}
		}
	}

	public void render() {
		primaryStage.draw();
		infoWindow.render();
	}

	public void onResize(int width, int height) {
		primaryStage.getViewport().update(width, height, true);
		infoWindow.onResize(width, height);
	}

	public List<InputProcessor> getInputProcessors() {
		return Arrays.asList(primaryStageInputHandler);
	}

	private void switchView(GuiViewName viewName) {
		GuiView newView = guiViewRepository.getByName(viewName);
		if (newView == null) {
			Logger.error("No GuiView defined for " + viewName.name());
		} else {
			this.currentViewName = viewName;
			if (currentView != null) {
				currentView.onHide();
			}
			removeAllTooltips();
			containerTable.clear();
			if (!hiddenGuiAreas.contains(GuiArea.MAIN_GUI_VIEW)) {
				newView.onShow();
				newView.populate(containerTable);
			}
		}
		currentView = newView;
		if (GlobalSettings.DEV_MODE) {
			debugGuiView.update();
		}
	}

	private void removeAllTooltips() {
		for (Actor actor : primaryStage.getActors()) {
			if (actor instanceof TooltipTable) {
				actor.remove();
			}
		}
	}

	public void showDialog(GameDialog dialog) {
		dialog.show(primaryStage);
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		if (gameContext != null && gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
			switchView(GuiViewName.SELECT_STARTING_LOCATION);
		} else {
			switchView(GuiViewName.DEFAULT_MENU);
		}
		this.gameContext = gameContext;
		rebuildUI();
	}

	@Override
	public void clearContextRelatedState() {
		hiddenGuiAreas.clear();
	}
}
