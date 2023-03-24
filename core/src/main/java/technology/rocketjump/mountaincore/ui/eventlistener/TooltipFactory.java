package technology.rocketjump.mountaincore.ui.eventlistener;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWordClass;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

@Singleton
public class TooltipFactory {

	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;

	@Inject
	public TooltipFactory(GuiSkinRepository guiSkinRepository, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.messageDispatcher = messageDispatcher;
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

		if (locationHint.equals(TooltipLocationHint.BELOW)) {
			tooltipTable.add(new Image(skin.getDrawable("hover_state_label_arrow_up"))).size(76f, 40f).center().row();
		}

		Container<Label> labelContainer = new Container<>();
		labelContainer.setBackground(skin.get("hover_state_label_patch", TenPatchDrawable.class));

		String text = i18nText.toString();
		Label label = new Label(text, skin.get("tooltip-text", Label.LabelStyle.class));
		labelContainer.center();
		labelContainer.setActor(label);

		tooltipTable.add(labelContainer).center().row();

		if (locationHint.equals(TooltipLocationHint.ABOVE)) {
			tooltipTable.add(new Image(skin.getDrawable("hover_state_label_arrow_down"))).size(76f, 40f).center().row();
		}

		parentActor.addListener(new TooltipHoverListener(parentActor, tooltipTable, () -> labelContainer.getHeight() / 2f, locationHint, messageDispatcher));

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

	public void withTooltipText(Actor parentActor, String i18nKey, TooltipBackground tooltipBackground) {
		Table tooltipTable = new Table();
		tooltipTable.defaults().padBottom(30);

		String headerText = i18nTranslator.getTranslatedString(i18nKey).toString();
		// TODO use different text style for light background
		tooltipTable.add(new Label(headerText, skin.get("complex-tooltip-header", Label.LabelStyle.class))).center().row();

		String itemDescriptionText = i18nTranslator.getTranslatedString(i18nKey, I18nWordClass.TOOLTIP).toString();
		Label descriptionLabel = new Label(itemDescriptionText, skin);
		descriptionLabel.setWrap(true);
		tooltipTable.add(descriptionLabel).width(700).center().row();

		complexTooltip(parentActor, tooltipTable, tooltipBackground);
	}

	public void complexTooltip(Actor parentActor, Actor tooltipContents, TooltipBackground background) {
		TooltipTable tooltipTable = new TooltipTable();

		Container<Actor> contentContainer = new Container<>();
		contentContainer.setBackground(skin.get(background.tenPatchName, TenPatchDrawable.class));
		contentContainer.setActor(tooltipContents);
		contentContainer.pad(40).padBottom(80);

		tooltipTable.add(contentContainer).center().row();

		parentActor.addListener(new TooltipHoverListener(parentActor, tooltipTable, () -> (contentContainer.getHeight() / 2f) - 20f, TooltipLocationHint.ABOVE, messageDispatcher));

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
		private final MessageDispatcher messageDispatcher;

		public TooltipHoverListener(Actor parentActor, TooltipTable tooltipTable, NonThrowingCallable<Float> yOffsetCallback, TooltipLocationHint locationHint, MessageDispatcher messageDispatcher) {
			this.parentActor = parentActor;
			this.tooltipTable = tooltipTable;
			this.yOffsetCallback = yOffsetCallback;
			this.locationHint = locationHint;
			this.messageDispatcher = messageDispatcher;
		}

		@Override
		public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
			if (pointer != -1) {
				return;
			}
			// Remove other tooltips first
			if (tooltipTable.getStage() == null) {
				messageDispatcher.dispatchMessage(MessageType.GUI_REMOVE_ALL_TOOLTIPS);
			}
			// add to stage first or table size will be 0 (rarrrgghhh)
			parentActor.getStage().addActor(tooltipTable);
			// layout after adding to stage or else subsequent displaying of actor will be positioned differently (FFS)
			tooltipTable.layout();
			// Position here is lower-left corner of parent actor
			Vector2 position = parentActor.localToStageCoordinates(new Vector2(0, 0));

			if (locationHint.equals(TooltipLocationHint.ABOVE)) {
				position.add(parentActor.getWidth() / 2f, parentActor.getHeight());
				position.add(0, yOffsetCallback.call()); // This has to be run here or the width/height values will not be set!
			} else if (locationHint.equals(TooltipLocationHint.BELOW)) {
				position.add(parentActor.getWidth() / 2f, 0);
				position.sub(0, yOffsetCallback.call());
			}

			float tooltipWidth = tooltipTable.getMinWidth();
			float screenEdgePadding = 5f;
			if (tooltipWidth / 2.0 > position.x) {
				if (tooltipTable.getCells().size > 1) { //assumes two cells, one for arrow
					float arrowX = parentActor.localToStageCoordinates(new Vector2(0, 0)).x +  screenEdgePadding;
					if (locationHint == TooltipLocationHint.ABOVE) {
						tooltipTable.getCells().get(1).align(Align.left).padLeft(arrowX);
					} else {
						tooltipTable.getCells().get(0).align(Align.left).padLeft(arrowX);
					}
					tooltipTable.invalidate();
				}

				position.x = (tooltipWidth / 2.0f) + screenEdgePadding;
			} else if ((tooltipWidth / 2.0) + position.x > parentActor.getStage().getWidth()) {
				if (tooltipTable.getCells().size > 1) { //assumes two cells, one for arrow
					//TODO: this calc needs more work
					float arrowX =  parentActor.getStage().getWidth() - parentActor.localToStageCoordinates(new Vector2(0, 0)).x + screenEdgePadding - parentActor.getWidth();
					if (locationHint == TooltipLocationHint.ABOVE) {
						tooltipTable.getCells().get(1).align(Align.right).padRight(arrowX);
					} else {
						tooltipTable.getCells().get(0).align(Align.right).padRight(arrowX);
					}
					tooltipTable.invalidate();
				}
				position.x =   parentActor.getStage().getWidth() - ((tooltipWidth / 2.0f) - screenEdgePadding);
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
