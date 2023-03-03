package technology.rocketjump.saul.ui.widgets.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;

public class SeedButton extends Container<Button> {

	private final Drawable backgroundSelectionDrawable;
	private final PlantSpecies plantSpecies;
	private boolean checked;
	private Runnable onClick;

	public SeedButton(PlantSpecies plantSpecies, Entity seedEntity, Skin skin, TooltipFactory tooltipFactory, MessageDispatcher messageDispatcher,
					  EntityRenderer entityRenderer, I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary) {
		this.plantSpecies = plantSpecies;

		this.backgroundSelectionDrawable = skin.getDrawable("asset_selection_bg_cropped");
		this.pad(18);

		Drawable itemBackground = skin.getDrawable("asset_bg");
		Button seedButton = new Button(new EntityDrawable(
				seedEntity, entityRenderer, true, messageDispatcher
		).withBackground(itemBackground));

		seedButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		seedButton.addListener(new ChangeCursorOnHover(seedButton, GameCursor.SELECT, messageDispatcher));
		seedButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				setChecked(!isChecked());
				if (onClick != null) {
					onClick.run();
				}
			}
		});
		tooltipFactory.simpleTooltip(seedButton, i18nTranslator.getDescription(seedEntity).toString(), TooltipLocationHint.ABOVE);


		this.size(itemBackground.getMinWidth(), itemBackground.getMinHeight());

		this.setChecked(false);
		this.setActor(seedButton);
	}

	public PlantSpecies getPlantSpecies() {
		return plantSpecies;
	}

	public boolean isChecked() {
		return this.checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
		if (checked) {
			this.setBackground(backgroundSelectionDrawable);
		} else {
			this.setBackground(null);
		}
	}

	public void onClick(Runnable callback) {
		this.onClick = callback;
	}
}
