package technology.rocketjump.saul.ui.eventlistener;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import static technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint.ABOVE;
import static technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint.BELOW;

@Singleton
public class TooltipFactory {

	private final Skin skin;
	private final I18nTranslator i18nTranslator;

	@Inject
	public TooltipFactory(GuiSkinRepository guiSkinRepository, I18nTranslator i18nTranslator) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
	}

	/**
	 * Note that this should be called *after* attaching any click listeners on the parentActor
	 * so that the tooltip can be removed when the parentActor is clicked
	 */
	public void simpleTooltip(Actor parentActor, String i18nKey, TooltipLocationHint locationHint) {
		I18nText i18nText = i18nTranslator.getTranslatedString(i18nKey);
		simpleTooltip(parentActor, i18nText, locationHint);
	}

	public void simpleTooltip(Actor parentActor, I18nText i18nText, TooltipLocationHint locationHint) {
		TooltipTable tooltipTable = new TooltipTable();

		if (locationHint.equals(BELOW)) {
			tooltipTable.add(new Image(skin.getDrawable("hover_state_label_arrow_up"))).size(76f, 40f).center().row();
		}

		Container<Label> labelContainer = new Container<>();
		labelContainer.setBackground(skin.get("hover_state_label_patch", TenPatchDrawable.class));

		String text = i18nText.toString();
		Label label = new Label(text, skin.get("tooltip-text", Label.LabelStyle.class));
		labelContainer.center();
		labelContainer.setActor(label);

		tooltipTable.add(labelContainer).center().row();

		if (locationHint.equals(ABOVE)) {
			tooltipTable.add(new Image(skin.getDrawable("hover_state_label_arrow_down"))).size(76f, 40f).center().row();
		}

		parentActor.addListener(new TooltipHoverListener(parentActor, tooltipTable, () -> labelContainer.getHeight() / 2f, locationHint));

		tooltipTable.addListener(new InputListener() {
			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				checkToRemove(tooltipTable, parentActor);
			}
		});

		parentActor.addCaptureListener(event -> {
			if (event instanceof InputEvent inputEvent) {
				switch (inputEvent.getType()) {
					case touchDown:
					case touchUp:
					case touchDragged:
						tooltipTable.remove();
					default:
				}
			}
			return false;
		});
	}

	public enum TooltipBackground {

		LARGE_PATCH_DARK("asset_bg_tooltip_patch"),
		LARGE_PATCH_LIGHT("asset_bg_tooltip_patch_light");

		public final String tenPatchName;

		TooltipBackground(String tenPatchName) {
			this.tenPatchName = tenPatchName;
		}
	}

	public void complexTooltip(Actor parentActor, Actor tooltipContents, TooltipBackground background) {
		TooltipTable tooltipTable = new TooltipTable();

		Container<Actor> contentContainer = new Container<>();
		contentContainer.setBackground(skin.get(background.tenPatchName, TenPatchDrawable.class));
		contentContainer.setActor(tooltipContents);
		contentContainer.pad(40).padBottom(80);

		tooltipTable.add(contentContainer).center().row();

		parentActor.addListener(new TooltipHoverListener(parentActor, tooltipTable, () -> (contentContainer.getHeight() / 2f) - 20f, ABOVE));

		tooltipTable.addListener(new InputListener() {
			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				checkToRemove(tooltipTable, parentActor);
			}
		});

		parentActor.addCaptureListener(event -> {
			if (event instanceof InputEvent inputEvent) {
				switch (inputEvent.getType()) {
					case touchDown:
					case touchUp:
					case touchDragged:
						tooltipTable.remove();
					default:
				}
			}
			return false;
		});
	}

	private void checkToRemove(Table tooltipTable, Actor parentActor) {
		if (tooltipTable.getParent() != null) {
			Vector2 stageCoords = tooltipTable.getStage().screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
			if (!hoveringOverAny(stageCoords, tooltipTable, parentActor)) {
				tooltipTable.remove();
			}
		}
	}

	private boolean hoveringOverAny(Vector2 stageCoords, Actor... actors) {
		boolean hoveringOnAny = false;
		for (Actor actor : actors) {
			Vector2 localCoords = actor.stageToLocalCoordinates(stageCoords.cpy());
			hoveringOnAny = 0 <= localCoords.x && localCoords.x <= actor.getWidth() &&
					0 <= localCoords.y && localCoords.y <= actor.getHeight();
			if (hoveringOnAny) {
				break;
			}
		}
		return hoveringOnAny;
	}

	private class TooltipHoverListener extends InputListener {

		private final Actor parentActor;
		private final TooltipTable tooltipTable;
		private final TooltipLocationHint locationHint;
		private final NonThrowingCallable<Float> yOffsetCallback;

		public TooltipHoverListener(Actor parentActor, TooltipTable tooltipTable, NonThrowingCallable<Float> yOffsetCallback, TooltipLocationHint locationHint) {
			this.parentActor = parentActor;
			this.tooltipTable = tooltipTable;
			this.yOffsetCallback = yOffsetCallback;
			this.locationHint = locationHint;
		}

		@Override
		public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
			if (pointer != -1) {
				return;
			}
			// add to stage first or table size will be 0 (rarrrgghhh)
			parentActor.getStage().addActor(tooltipTable);
			// layout after adding to stage or else subsequent displaying of actor will be positioned differently (FFS)
			tooltipTable.layout();
			// Position here is lower-left corner of parent actor
			Vector2 position = parentActor.localToStageCoordinates(new Vector2(0, 0));

			if (locationHint.equals(ABOVE)) {
				position.add(parentActor.getWidth() / 2f, parentActor.getHeight());
				position.add(0, yOffsetCallback.call()); // This has to be run here or the width/height values will not be set!
			} else if (locationHint.equals(BELOW)) {
				position.add(parentActor.getWidth() / 2f, 0);
				position.sub(0, yOffsetCallback.call());
			}

			// setPosition() ***centers*** the actor being positioned around the point specified (internal screaming)
			tooltipTable.setPosition(position.x, position.y);
		}

		@Override
		public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
			checkToRemove(tooltipTable, parentActor);
		}

	}

	public interface NonThrowingCallable<V> {
		V call();
	}

}
