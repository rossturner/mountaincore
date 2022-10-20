package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.minimap.MinimapFrame;
import technology.rocketjump.saul.mapping.minimap.MinimapImage;
import technology.rocketjump.saul.mapping.minimap.MinimapManager;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

@Singleton
public class MinimapGuiView implements GuiView, GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final MinimapManager minimapManager;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final MinimapImage minimapImage;
	private final MinimapFrame minimapFrame;
	private final Texture minimapSelectionTexture;
	private Table table;
	private GameContext gameContext;

	@Inject
	public MinimapGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						  MinimapManager minimapManager, PrimaryCameraWrapper primaryCameraWrapper) {
		this.messageDispatcher = messageDispatcher;
		this.minimapManager = minimapManager;
		this.primaryCameraWrapper = primaryCameraWrapper;

		Skin uiSkin = guiSkinRepository.getDefault();
		table = new Table(uiSkin);


		minimapSelectionTexture = new Texture("assets/ui/minimapSelection.png");
		TextureRegionDrawable selectionDrawable = new TextureRegionDrawable(new TextureRegion(minimapSelectionTexture));
		minimapImage = new MinimapImage(selectionDrawable, messageDispatcher);
		minimapFrame = new MinimapFrame(minimapImage, guiSkinRepository.getMainGameSkin());

		messageDispatcher.addListener(this, MessageType.MINIMAP_SIZE_CHANGED);
	}

	@Override
	public void populate(Table containerTable) {
		resetTable();
		containerTable.add(table).right();
	}

	@Override
	public void update() {
		if (minimapManager.getCurrentTexture() != null) {
			minimapImage.updateTexture(minimapManager.getCurrentTexture());
		}

		OrthographicCamera camera = primaryCameraWrapper.getCamera();

		minimapImage.setMapSize(gameContext.getAreaMap().getWidth(), gameContext.getAreaMap().getHeight());
		minimapImage.setCameraPosition(camera.position);
		minimapImage.setViewportSize(camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
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
		table.add(minimapFrame).right();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		if (gameContext != null) {
			// TODO set minimapContainer size smaller when map is large
			this.gameContext = gameContext;
			minimapImage.setSize(gameContext.getAreaMap().getWidth(), gameContext.getAreaMap().getHeight());
		}
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MINIMAP_SIZE_CHANGED -> {
				minimapFrame.reset();
				return true;
			}
			default ->
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName());
		}
	}
}
