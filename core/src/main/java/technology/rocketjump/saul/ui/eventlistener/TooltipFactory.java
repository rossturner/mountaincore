package technology.rocketjump.saul.ui.eventlistener;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
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
		Table tooltipTable = new Table();

		if (locationHint.equals(BELOW)) {
			tooltipTable.add(new Image(skin.getDrawable("hover_state_label_arrow_up"))).size(76f, 40f).center().row();
		}

		Container<Label> labelContainer = new Container<>();
		labelContainer.setBackground(skin.get("hover_state_label_patch", TenPatchDrawable.class));

		String text = i18nTranslator.getTranslatedString(i18nKey).toString();
		Label label = new Label(text, skin.get("tooltip-text", Label.LabelStyle.class));
		labelContainer.center();
		labelContainer.setActor(label);

		tooltipTable.add(labelContainer).center().row();

		if (locationHint.equals(ABOVE)) {
			tooltipTable.add(new Image(skin.getDrawable("hover_state_label_arrow_down"))).size(76f, 40f).center().row();
		}

		parentActor.addListener(new InputListener() {
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				// add to stage first or table size will be 0 (rarrrgghhh)
				parentActor.getStage().addActor(tooltipTable);
				// layout after adding to stage or else subsequent displaying of actor will be positioned differently (FFS)
				tooltipTable.layout();
				// Position here is lower-left corner of parent actor
				Vector2 position = parentActor.localToStageCoordinates(new Vector2(0, 0));

				if (locationHint.equals(ABOVE)) {
					position.add(parentActor.getWidth() / 2f, parentActor.getHeight());
					position.add(0, labelContainer.getHeight() / 2f);
				} else if (locationHint.equals(BELOW)) {
					position.add(parentActor.getWidth() / 2f, 0);
					position.sub(0, labelContainer.getHeight() / 2f);
				}

				// setPosition() ***centers*** the actor being positioned around the point specified (internal screaming)
				tooltipTable.setPosition(position.x, position.y);
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				checkToRemove(tooltipTable, parentActor);
			}
		});

		tooltipTable.addListener(new InputListener() {
			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				checkToRemove(tooltipTable, parentActor);
			}
		});

		if (hasClickListener(parentActor)) {
			parentActor.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					tooltipTable.remove();
				}
			});
		}
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

	private boolean hasClickListener(Actor actor) {
		for (EventListener listener : actor.getListeners()) {
			if (listener instanceof ClickListener) {
				return true;
			}
		}
		return false;
	}

}
