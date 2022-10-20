package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.minimap.MinimapContainer;
import technology.rocketjump.saul.mapping.minimap.MinimapManager;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

@Singleton
public class MinimapGuiView implements GuiView, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final MinimapManager minimapManager;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final MinimapContainer minimapContainer;
	private final Texture minimapSelectionTexture;
	private final WidgetGroup minimapGroup;
	private Table table;
	private final Button resizeButton;
	private GameContext gameContext;

	@Inject
	public MinimapGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						  MinimapManager minimapManager, PrimaryCameraWrapper primaryCameraWrapper) {
		this.messageDispatcher = messageDispatcher;
		this.minimapManager = minimapManager;
		this.primaryCameraWrapper = primaryCameraWrapper;

		Skin uiSkin = guiSkinRepository.getDefault();
		table = new Table(uiSkin);

		Skin mainGameSkin = guiSkinRepository.getMainGameSkin();
		Drawable resizeButtonDrawable = mainGameSkin.getDrawable("btn_map_resize");
		resizeButton = new Button(resizeButtonDrawable);
		resizeButton.setSize(resizeButtonDrawable.getMinWidth() / 2f, resizeButtonDrawable.getMinHeight() / 2f);
		resizeButton.addListener(new ChangeCursorOnHover(GameCursor.RESIZE, messageDispatcher));

		minimapSelectionTexture = new Texture("assets/ui/minimapSelection.png");
		TextureRegionDrawable selectionDrawable = new TextureRegionDrawable(new TextureRegion(minimapSelectionTexture));
		minimapContainer = new MinimapContainer(selectionDrawable, messageDispatcher, mainGameSkin);

		minimapGroup = new WidgetGroup();
		minimapGroup.setFillParent(true);

		minimapGroup.addActor(minimapContainer);
		minimapGroup.addActor(resizeButton);

		minimapGroup.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
			}
		});
	}

	@Override
	public void populate(Table containerTable) {
		resetTable();
		containerTable.add(table).right();
	}

	@Override
	public void update() {
		if (minimapManager.getCurrentTexture() != null) {
			minimapContainer.updateTexture(minimapManager.getCurrentTexture());
		}

		OrthographicCamera camera = primaryCameraWrapper.getCamera();

		minimapContainer.setMapSize(gameContext.getAreaMap().getWidth(), gameContext.getAreaMap().getHeight());
		minimapContainer.setCameraPosition(camera.position);
		minimapContainer.setViewportSize(camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
	}

	@Override
	public GuiViewName getName() {
		// This is a special case GuiView which lives outside of the normal usage
		return null;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	private void resetTable() {
		table.clearChildren();

		resizeButton.setPosition(resizeButton.getX(), resizeButton.getY() + minimapContainer.getContainerHeight() - (resizeButton.getHeight() / 2f));
		minimapContainer.setSize(minimapContainer.getContainerWidth(), minimapContainer.getContainerHeight());
//		minimapGroup.setSize(minimapContainer.getContainerWidth(), minimapContainer.getContainerHeight());

		table.add(minimapGroup).right().expand(true, true)
				.size(minimapContainer.getContainerWidth(), minimapContainer.getContainerHeight())
				.padRight(8).padBottom(8);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		if (gameContext != null) {
			// TODO set minimapContainer size smaller when map is large
			this.gameContext = gameContext;
			minimapContainer.setContainerWidth(gameContext.getAreaMap().getWidth());
			minimapContainer.setContainerHeight(gameContext.getAreaMap().getHeight());
			resetTable();
		}
	}

	@Override
	public void clearContextRelatedState() {

	}

//	@Override
//	public boolean handleMessage(Telegram msg) {
//		switch (msg.message) {
//			case MessageType.MINIMAP_SIZE_CHANGED -> {
//				minimapFrame.reset();
//				return true;
//			}
//			default ->
//				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName());
//		}
//	}
}
