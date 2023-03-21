package technology.rocketjump.saul.ui.widgets.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesSeed;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;

import java.util.Objects;

public class SeedButton extends Container<Stack> {

	private final Drawable backgroundSelectionDrawable;
	private final PlantSpecies plantSpecies;
	private final Label quantityLabel;
	private boolean checked;
	private Runnable onClick;

	public SeedButton(PlantSpecies plantSpecies, Entity seedEntity, Skin skin, TooltipFactory tooltipFactory, MessageDispatcher messageDispatcher,
					  EntityRenderer entityRenderer, I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary, ManagementSkin managementSkin) {
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

		Stack stack = new Stack();

		stack.add(seedButton);

		quantityLabel = new Label("", managementSkin, "entity_drawable_quantity_label");
		quantityLabel.setAlignment(Align.center);
		quantityLabel.layout();

		float xOffset = 10f;
		float yOffset = 10f;
		float extraWidth = seedButton.getPrefWidth() + xOffset - quantityLabel.getPrefWidth();
		float extraHeight = seedButton.getPrefHeight() + yOffset - quantityLabel.getPrefHeight();


		Table amountTable = new Table();
		amountTable.add(quantityLabel).left().top();
		amountTable.add(new Container<>()).expandX().width(extraWidth).row();
		amountTable.add(new Container<>()).colspan(2).height(extraHeight).expandY();
		stack.add(amountTable);

		this.size(itemBackground.getMinWidth(), itemBackground.getMinHeight());

		this.setChecked(false);
		this.setActor(stack);
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

	public void updateQuantityLabel(SettlementItemTracker settlementItemTracker) {
		PlantSpeciesSeed seed = plantSpecies.getSeed();
		GameMaterial seedMaterial = seed.getSeedMaterial();
		ItemType seedItemType = seed.getSeedItemType();

		int quantity = settlementItemTracker
				.getAll(false)
				.stream()
				.mapToInt(entity -> {
					if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
						if (Objects.equals(seedItemType, attributes.getItemType()) && Objects.equals(seedMaterial, attributes.getPrimaryMaterial())) {
							return attributes.getQuantity();
						}
					}
					return 0;
				})
				.sum();
		quantityLabel.setText(quantity);
	}
}
