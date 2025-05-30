package technology.rocketjump.mountaincore.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import technology.rocketjump.mountaincore.crafting.CraftingRecipeDictionary;
import technology.rocketjump.mountaincore.crafting.model.CraftingRecipe;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.tags.CraftingStationBehaviourTag;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.rooms.constructions.FurnitureConstruction;
import technology.rocketjump.mountaincore.settlement.SettlementState;
import technology.rocketjump.mountaincore.settlement.production.CraftingQuota;
import technology.rocketjump.mountaincore.ui.GameInteractionStateContainer;
import technology.rocketjump.mountaincore.ui.Selectable;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.skins.ManagementSkin;
import technology.rocketjump.mountaincore.ui.skins.MenuSkin;
import technology.rocketjump.mountaincore.ui.views.RoomEditorItemMap;
import technology.rocketjump.mountaincore.ui.widgets.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.*;

import static technology.rocketjump.mountaincore.screens.ManagementScreenName.CRAFTING;

@Singleton
public class CraftingManagementScreen extends AbstractGameScreen implements GameContextAware, DisplaysText {

	public static final float QUANTITY_CIRCLE_OVERLAP = 32f;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final EntityRenderer entityRenderer;
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final MenuSkin menuSkin;
	private final ManagementSkin managementSkin;
	private final ScrollPane scrollPane;
	private final Image fullScreenOverlay;
	private final LabelFactory labelFactory;
	private final ButtonFactory buttonFactory;
	private final RoomEditorItemMap roomEditorItemMap;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final WidgetFactory widgetFactory;

	private Stack stack;
	private Label titleLabel;
	private GameContext gameContext;

	@Inject
	public CraftingManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
									I18nTranslator i18nTranslator,
									EntityRenderer entityRenderer, CraftingRecipeDictionary craftingRecipeDictionary,
									LabelFactory labelFactory, ButtonFactory buttonFactory, RoomEditorItemMap roomEditorItemMap,
									GameInteractionStateContainer gameInteractionStateContainer, UserPreferences userPreferences,
									CraftingTypeDictionary craftingTypeDictionary, WidgetFactory widgetFactory) {
		super(userPreferences, messageDispatcher);
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.entityRenderer = entityRenderer;
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.labelFactory = labelFactory;
		this.buttonFactory = buttonFactory;
		this.roomEditorItemMap = roomEditorItemMap;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.widgetFactory = widgetFactory;

		scrollPane = new EnhancedScrollPane(null, menuSkin);

		fullScreenOverlay = new Image(menuSkin, "default-rect");
		fullScreenOverlay.setFillParent(true);
		fullScreenOverlay.setColor(0, 0, 0, 0.6f);
		fullScreenOverlay.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				stack.removeActor(fullScreenOverlay);
			}
		});
	}

	@Override
	public String getName() {
		return CRAFTING.name();
	}

	//Screen implementation
	@Override
	public void show() {
		clearContextRelatedState();

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new ManagementScreenInputHandler(messageDispatcher) {
			@Override
			protected void closeScreen() {
				super.closeScreen();
				returnToSelectable();
			}
		});
		Gdx.input.setInputProcessor(inputMultiplexer);
		rebuildUI();
		stage.setKeyboardFocus(null);

		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	public void dispose() { }

	//Game Context implementation
	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}

	//Called from Screen.show() and elsewhere when language changes
	@Override
	public void rebuildUI() {
		stack = new Stack();
		stack.setFillParent(true);
		stack.add(menuSkin.buildBackgroundBaseLayer());
		stack.add(menuSkin.buildPaperLayer(buildPaperComponents(), 257, false, false));
		stack.add(buildExitTable());

		stage.addActor(stack);
	}

	private Actor buildExitTable() {
		Table table = new Table();
		Button exitButton = new Button(menuSkin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
				returnToSelectable();
			}
		});
		buttonFactory.attachClickCursor(exitButton, GameCursor.SELECT);
		table.add(exitButton).expandX().align(Align.topRight).padRight(257 + menuSkin.getDrawable("paper_texture_bg_pattern_large").getMinWidth() + 5f).padTop(5f).row();
		table.add().grow();
		return table;
	}

	private void returnToSelectable() {
		if (gameInteractionStateContainer.getSelectable() != null) {
			messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, gameInteractionStateContainer.getSelectable());
		}
	}

	private Table buildPaperComponents() {
		titleLabel = labelFactory.titleRibbon("GUI.CRAFTING_MANAGEMENT.TITLE");

		Label itemHeaderLabel = new Label(i18nTranslator.translate("GUI.CRAFTING_MANAGEMENT.TABLE.ITEM"), managementSkin, "military_subtitle_ribbon");
		itemHeaderLabel.setAlignment(Align.center);
		Label craftedWithHeaderLabel = new Label(i18nTranslator.translate("GUI.CRAFTING_MANAGEMENT.TABLE.CRAFTED_WITH"), managementSkin, "military_subtitle_ribbon");
		craftedWithHeaderLabel.setAlignment(Align.center);
		Label managementHeaderLabel = new Label(i18nTranslator.translate("GUI.CRAFTING_MANAGEMENT.TABLE.MANAGEMENT"), managementSkin, "military_subtitle_ribbon");
		managementHeaderLabel.setAlignment(Align.center);

		rebuildCraftingComponents();
		scrollPane.setFadeScrollBars(false);
		scrollPane.setForceScroll(false, false);
		scrollPane.setScrollBarPositions(true, true);

		Table headerRow = new Table();
		headerRow.add(itemHeaderLabel).left().padLeft(16).spaceTop(50).spaceBottom(40);
		headerRow.add(craftedWithHeaderLabel).expandX().spaceTop(50).spaceBottom(40);
		headerRow.add(managementHeaderLabel).right().padRight(16).spaceTop(50).spaceBottom(40).row();

		Table mainTable = new Table();
		mainTable.add(headerRow).growX().left().padBottom(40).row();
		mainTable.add(scrollPane).grow();

		Table table = new Table();
		table.add(titleLabel).padTop(54f).row();
		table.add(mainTable).spaceTop(48f).padLeft(35).padRight(35).grow();
		return table;
	}

	private void rebuildCraftingComponents() {
		if (getCraftingType() != null) {
			final String craftingLabelText;
			if (getCraftingType().getProfessionRequired() != null) {
				craftingLabelText = i18nTranslator.getTranslatedWordWithReplacements("GUI.CRAFTING_MANAGEMENT.SUBTITLE",
						Map.of("craftingType", i18nTranslator.getTranslatedString(getCraftingType().getI18nKey()),
							"profession", i18nTranslator.getTranslatedString(getCraftingType().getProfessionRequired().getI18nKey()))).toString();

			} else {
				craftingLabelText = i18nTranslator.translate(getCraftingType().getI18nKey());
			}
			titleLabel.setText(craftingLabelText);
			scrollPane.setActor(buildItemsTable());
		}
	}

	private CraftingType getCraftingType() {
		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null) {
			Entity entity = selectable.getEntity();
			Construction construction = selectable.getConstruction();
			if (entity != null && entity.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour) {
				return craftingStationBehaviour.getCraftingType();
			}
			if (construction instanceof FurnitureConstruction furnitureConstruction) {
				PhysicalEntityComponent physicalEntityComponent = furnitureConstruction.getFurnitureEntityToBePlaced().getPhysicalEntityComponent();
				if (physicalEntityComponent.getAttributes() instanceof FurnitureEntityAttributes attributes) {
					Optional<Tag> optionalTag = attributes.getFurnitureType().getProcessedTags()
							.stream()
							.filter(pt -> pt.getClass().isAssignableFrom(CraftingStationBehaviourTag.class))
							.findAny();
					return optionalTag.map(tag -> {
						CraftingStationBehaviourTag craftingTag = (CraftingStationBehaviourTag) tag;
						return craftingTag.getCraftingType(craftingTypeDictionary);
					}).orElse(null);
				}
			}
		}
		return null;
	}

	private Table buildItemsTable() {
		Table itemsTable = new Table();
		itemsTable.defaults().padBottom(40);
		itemsTable.top().left();

		List<CraftingRecipe> craftingRecipes = craftingRecipeDictionary.getByCraftingType(getCraftingType());

		Comparator<CraftingRecipe> sortBy = Comparator.comparing(cr -> i18nTranslator.translate(cr.getOutput().getItemType().getI18nKey()));

		for (CraftingRecipe craftingRecipe : craftingRecipes.stream().sorted(sortBy).toList()) {

			QuantifiedItemTypeWithMaterial output = craftingRecipe.getOutput();


			Table inputsColumn = new Table();
			Iterator<QuantifiedItemTypeWithMaterial> inputIterator = craftingRecipe.getInput().iterator();
			while (inputIterator.hasNext()) {
				QuantifiedItemTypeWithMaterial input = inputIterator.next();
				inputsColumn.add(entityColumn(input));
				if (inputIterator.hasNext()) {
					inputsColumn.add(new Image(managementSkin.getDrawable("asset_add"))).padLeft(189).padRight(189);
				}
			}

			SettlementState settlementState = gameContext.getSettlementState();
			CraftingQuota quota = settlementState.getCraftingQuota(output.getItemType(), output.getMaterial());

			Table productionManagementColumn = new Table();
			productionManagementColumn.defaults().right();
			ImageTextButton limitToggle = widgetFactory.createLeftLabelledToggle("GUI.CRAFTING_MANAGEMENT.TABLE.MANAGEMENT.LIMIT", managementSkin, null);
			productionManagementColumn.add(limitToggle).row();

			Table quantityTable = new Table();
			Label quantityTitleLabel = new Label(i18nTranslator.translate("GUI.CRAFTING_MANAGEMENT.TABLE.MANAGEMENT.QUANTITY"), managementSkin, "default-font-23-label");
			Label quantityLabel = new Label(Integer.toString(quota.getQuantity()), managementSkin, "default-font-23-label");
			quantityLabel.setAlignment(Align.center);
			Button decrementButton = buttonFactory.buildDrawableButton("btn_remove_tile", "GUI.CRAFTING_MANAGEMENT.TABLE.MANAGEMENT.QUANTITY.DECREMENT", () -> { });
			Button incrementButton = buttonFactory.buildDrawableButton("btn_add_tile", "GUI.CRAFTING_MANAGEMENT.TABLE.MANAGEMENT.QUANTITY.INCREMENT", () -> { });

			quantityTable.add(quantityTitleLabel).padRight(9f);
			quantityTable.add(decrementButton).padRight(9f);
			quantityTable.add(quantityLabel).width(100).padRight(9f);
			quantityTable.add(incrementButton).padRight(9f);

			buttonFactory.attachClickCursor(limitToggle, GameCursor.SELECT);

			limitToggle.setChecked(quota.isLimited());
			if (!quota.isLimited()) {
				buttonFactory.disable(quantityTitleLabel);
				buttonFactory.disable(decrementButton);
				buttonFactory.disable(quantityLabel);
				buttonFactory.disable(incrementButton);
			} else {
				assignClickListeners(quota, incrementButton, decrementButton, quantityLabel);
			}

			limitToggle.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (limitToggle.isChecked()) {
						buttonFactory.enable(quantityTitleLabel);
						buttonFactory.enable(decrementButton);
						buttonFactory.enable(quantityLabel);
						buttonFactory.enable(incrementButton);

						CraftingQuota newQuota = settlementState.newCraftingQuota(output.getItemType(), output.getMaterial(), 0);
						assignClickListeners(newQuota, incrementButton, decrementButton, quantityLabel);

					} else {
						settlementState.removeCraftingQuota(output.getItemType(), output.getMaterial());
						quantityLabel.setText("0");
						buttonFactory.disable(quantityTitleLabel);
						buttonFactory.disable(decrementButton);
						buttonFactory.disable(quantityLabel);
						buttonFactory.disable(incrementButton);
					}
				}
			});

			productionManagementColumn.add(quantityTable);

			itemsTable.add(dividerLine()).width(2842).row();
			Table row = new Table();
			row.add(entityColumn(output)).padRight(154);
			row.add(new Image(managementSkin.getDrawable("asset_equals"))).padRight(154);
			row.add(inputsColumn).padRight(154);
			row.add(productionManagementColumn).expandX().padRight(16).right();
			itemsTable.add(row).growX().left();

			itemsTable.row();
		}

		return itemsTable;
	}

	private void assignClickListeners(CraftingQuota newQuota, Button incrementButton, Button decrementButton, Label quantityLabel) {
		incrementButton.clearListeners();
		decrementButton.clearListeners();
		buttonFactory.attachClickCursor(incrementButton, GameCursor.SELECT);
		buttonFactory.attachClickCursor(decrementButton, GameCursor.SELECT);
		incrementButton.addListener(new InputListener() {
			private Action currentAction;
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				currentAction = numberSpinnerAction(newQuota, quantityLabel, +1, +10, +100);
				incrementButton.addAction(currentAction);
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				incrementButton.removeAction(currentAction);
				super.touchUp(event, x, y, pointer, button);
			}
		});


		decrementButton.addListener(new InputListener() {
			private Action currentAction;
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				currentAction = numberSpinnerAction(newQuota, quantityLabel, -1, -10, -100);
				decrementButton.addAction(currentAction);
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				decrementButton.removeAction(currentAction);
				super.touchUp(event, x, y, pointer, button);
			}
		});
	}

	private static SequenceAction numberSpinnerAction(CraftingQuota newQuota, Label quantityLabel, int firstStep, int secondStep, int thirdStep) {
		return Actions.sequence(
				new ModifyQuantityAction(newQuota, quantityLabel, firstStep),
				Actions.delay(0.5f),
				Actions.repeat((10 - newQuota.getQuantity() % 10) - 1,
						Actions.sequence(
								new ModifyQuantityAction(newQuota, quantityLabel, firstStep),
								Actions.delay(0.15f)
						)
				),
				Actions.repeat(9,
						Actions.sequence(
								new ModifyQuantityAction(newQuota, quantityLabel, secondStep),
								Actions.delay(0.15f)
						)
				),
				Actions.repeat(1000,
						Actions.sequence(
								new ModifyQuantityAction(newQuota, quantityLabel, thirdStep),
								Actions.delay(0.15f)
						)
				)
		);
	}

	private static class ModifyQuantityAction extends Action {
		private final CraftingQuota craftingQuota;
		private final Label quantityLabel;
		private final int modifier;

		private ModifyQuantityAction(CraftingQuota craftingQuota, Label quantityLabel, int modifier) {
			this.craftingQuota = craftingQuota;
			this.quantityLabel = quantityLabel;
			this.modifier = modifier;
		}

		@Override
		public boolean act(float delta) {
			int nextQuantity = craftingQuota.getQuantity() + modifier;
			craftingQuota.setQuantity(nextQuantity);
			quantityLabel.setText(Integer.toString(craftingQuota.getQuantity()));
			return true;
		}

	}

	private Actor dividerLine() {
		Image image = new Image(managementSkin.getDrawable("asset_line"));
		image.setScaling(Scaling.fit);

		return new Container<>(image);
	}

	private Table entityColumn(QuantifiedItemTypeWithMaterial output) {
		final I18nText targetDescription;
		if (output.isLiquid()) {
			targetDescription = i18nTranslator.getLiquidDescription(output.getMaterial(), output.getQuantity());
		} else {
			targetDescription = i18nTranslator.getItemDescription(output.getQuantity(),
					output.getMaterial(),
					output.getItemType(), null);
		}
		Label entityLabel = new Label(targetDescription.toString(), managementSkin, "item_type_name_label");
		entityLabel.setWrap(true);
		entityLabel.setAlignment(Align.center);

		Table exampleEntityColumn = new Table();

		if (output.isLiquid()) {
			exampleEntityColumn.add(buildLiquidButton(output.getMaterial(), output.getQuantity())).size(205).row();
		} else {
			Entity exampleEntity = roomEditorItemMap.get(output.getItemType(), gameContext, output.getMaterial());
			exampleEntityColumn.add(buildEntityButton(exampleEntity, output.getQuantity())).size(205).row();
		}

		exampleEntityColumn.add(entityLabel).width(240);
		return exampleEntityColumn;
	}

	private Actor buildEntityButton(Entity exampleEntity, int quantity) {
		Stack entityStack = new Stack();

		Drawable btnResourceItemBg = managementSkin.bgForExampleEntity(exampleEntity.getId());
		Button itemTypeButton = new Button(new EntityDrawable(
				exampleEntity, entityRenderer, true, messageDispatcher
		).withBackground(btnResourceItemBg));

		Label amountLabel = new Label(String.valueOf(quantity), managementSkin, "entity_drawable_quantity_label");
		amountLabel.setAlignment(Align.center);

		Table amountTable = new Table();
		amountTable.add(amountLabel).left().top();
		amountTable.add(new Container<>()).width(btnResourceItemBg.getMinWidth()- QUANTITY_CIRCLE_OVERLAP).expandX().row();
		amountTable.add(new Container<>()).colspan(2).height(btnResourceItemBg.getMinHeight()- QUANTITY_CIRCLE_OVERLAP).expandY();

		entityStack.add(itemTypeButton);
		if (quantity > 1) {
			entityStack.add(amountTable);
		}

		return entityStack;
	}

	private Actor buildLiquidButton(GameMaterial material, int quantity) {
		Stack entityStack = new Stack();

		Drawable btnResourceItemBg = managementSkin.bgForExampleEntity(1);
		Drawable icon = managementSkin.newDrawable("icon_water_white", material.getColor());

		ImageButton imageButton = new ImageButton(icon);
		imageButton.getStyle().up = btnResourceItemBg;

		Label amountLabel = new Label(String.valueOf(quantity), managementSkin, "entity_drawable_quantity_label");
		amountLabel.setAlignment(Align.center);

		Table amountTable = new Table();
		amountTable.add(amountLabel).left().top();
		amountTable.add(new Container<>()).width(btnResourceItemBg.getMinWidth()- QUANTITY_CIRCLE_OVERLAP).expandX().row();
		amountTable.add(new Container<>()).colspan(2).height(btnResourceItemBg.getMinHeight()- QUANTITY_CIRCLE_OVERLAP).expandY();

		entityStack.add(imageButton);
		if (quantity > 1) {
			entityStack.add(amountTable);
		}

		return entityStack;
	}
}
