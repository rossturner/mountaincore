package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ChangeProfessionMessage;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.skins.ManagementSkin;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class SettlerProfessionFactory {
	private final Skin menuSkin;
	private final ManagementSkin managementSkin;
	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;
	private final I18nTranslator i18nTranslator;
	private final SkillDictionary skillDictionary;
	private final TooltipFactory tooltipFactory;
	private final Drawable noneSelectedDrawableOption;
	private final Drawable noneSelectedDrawable;

	@Inject
	public SettlerProfessionFactory(GuiSkinRepository skinRepository, MessageDispatcher messageDispatcher,
	                                SoundAssetDictionary soundAssetDictionary, I18nTranslator i18nTranslator,
	                                SkillDictionary skillDictionary, TooltipFactory tooltipFactory) {
		this.menuSkin = skinRepository.getMenuSkin();
		this.managementSkin = skinRepository.getManagementSkin();
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;
		this.i18nTranslator = i18nTranslator;
		this.skillDictionary = skillDictionary;
		this.tooltipFactory = tooltipFactory;
		this.noneSelectedDrawableOption = skinRepository.getMainGameSkin().getDrawable("icon_not_equipped_no_bg");
		this.noneSelectedDrawable = skinRepository.getManagementSkin().getDrawable("icon_not_equipped");
	}

	//todo: really needs refactoring, lots of things happening
	//TODO: change this to just return a table instead - bah it passes it in to clear the children and rebuild, bit dirty
	public void addProfessionComponents(Entity settler, Table table, Consumer<Entity> onProfessionChange, float scale) {
		table.clearChildren();
		SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
		DragAndDrop dragAndDrop = new DragAndDrop();
		boolean showNonOption = true;
		for (int i = 0; i < SkillsComponent.MAX_PROFESSIONS; i++) {
			Table column = new Table();
			Image numberIcon = new Image(managementSkin.getDrawable("icon_" + (i + 1)));
			column.add(numberIcon).row();
			java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();

			final Skill skill;
			if (i < activeProfessions.size()) {
				skill = activeProfessions.get(i).getSkill();
			} else {
				skill = null;
			}
			int activeSkillIndex = i;

			if (skill != null) {
				TextureRegionDrawable drawable = (TextureRegionDrawable) managementSkin.getDrawable(skill.getDraggableIcon());
				drawable = new TextureRegionDrawable(drawable);
				drawable.setMinWidth(drawable.getMinWidth() * scale);
				drawable.setMinHeight(drawable.getMinHeight() * scale);
				Image draggableImage = new Image(drawable);

				Table progressRow = buildProgressBarRow(skillsComponent, skill, false);

				tooltipFactory.simpleTooltip(draggableImage, skill.getI18nKey(), TooltipLocationHint.ABOVE);

				Actor draggingCursorWidget = new Actor();
				draggingCursorWidget.addListener(new ChangeCursorOnHover(draggingCursorWidget, GameCursor.REORDER_HORIZONTAL, messageDispatcher));
				draggingCursorWidget.setWidth(draggableImage.getWidth() * 0.34f);
				draggingCursorWidget.setHeight(draggableImage.getHeight());
				Actor clickingCursorWidget = new Actor();
				clickingCursorWidget.addListener(new ChangeCursorOnHover(clickingCursorWidget, GameCursor.SELECT, messageDispatcher));
				clickingCursorWidget.setWidth(draggableImage.getWidth() * (1-0.34f));
				clickingCursorWidget.setHeight(draggableImage.getHeight());

				Table draggableCursors = new Table();
				draggableCursors.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
				draggableCursors.add(draggingCursorWidget);
				draggableCursors.add(clickingCursorWidget);

				Stack complexCursorStack = new Stack();
				complexCursorStack.add(draggableImage);
				complexCursorStack.add(draggableCursors);
				column.add(complexCursorStack).spaceTop(10f).spaceBottom(6f).row();
				column.add(progressRow);
				tooltipFactory.simpleTooltip(complexCursorStack, skill.getI18nKey(), TooltipLocationHint.BELOW);

				dragAndDrop.addSource(new DraggableProfession(dragAndDrop, draggingCursorWidget, draggableImage, i));
				dragAndDrop.addTarget(new DraggableProfessionTarget(column, i, skillsComponent, managementSkin, table, settler, onProfessionChange, scale));

				clickingCursorWidget.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, new ChangeProfessionDialog(i18nTranslator, menuSkin, messageDispatcher, skillDictionary, soundAssetDictionary, settler, activeSkillIndex, table, onProfessionChange, scale));
					}
				});
			} else {
				TextureRegionDrawable drawable = new TextureRegionDrawable((TextureRegionDrawable) noneSelectedDrawable);
				drawable.setMinWidth(drawable.getMinWidth() * scale);
				drawable.setMinHeight(drawable.getMinHeight() * scale);
				Image noneOptionImage = new Image(drawable);
				noneOptionImage.addListener(new ChangeCursorOnHover(noneOptionImage, GameCursor.SELECT, messageDispatcher));
				noneOptionImage.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, new ChangeProfessionDialog(i18nTranslator, menuSkin, messageDispatcher, skillDictionary, soundAssetDictionary, settler, activeSkillIndex, table, onProfessionChange, scale));
					}
				});
				column.add(noneOptionImage).spaceTop(10f).spaceBottom(6f).row();
				Table progressRow = buildProgressBarRow(skillsComponent, SkillDictionary.NULL_PROFESSION, false);
				column.add(progressRow);
				noneOptionImage.setVisible(showNonOption);

				showNonOption = false;
			}

			table.add(column).spaceRight(24 * scale).spaceLeft(24 * scale);
		}
	}

	//todo: move somewhere better
	public Table buildProgressBarRow(SkillsComponent skillsComponent, Skill skill, boolean useAltStyle) {
		final String labelStyleName;
		final String progressBarStyleName;
		if (useAltStyle) {
			labelStyleName = "progress_bar_white_label";
			progressBarStyleName = "progress_bar_darker_bg-horizontal";
		} else {
			labelStyleName = "progress_bar_label";
			progressBarStyleName = "default-horizontal";
		}


		int percent = (int) (skillsComponent.getNextLevelProgressPercent(skill) * 100);
		int skillLevel = skillsComponent.getSkillLevel(skill);
		int nextLevel = skillLevel + 1;
		ProgressBar progressBar = new ProgressBar(0, 100, 1, false, managementSkin, progressBarStyleName);
		progressBar.setValue(percent);
		progressBar.setDisabled(true);
		ProgressBar.ProgressBarStyle clonedStyle = new ProgressBar.ProgressBarStyle(progressBar.getStyle());
		if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
			clonedStyle.knobBefore = ninePatchDrawable.tint(managementSkin.getColor("progress_bar_green"));
		}
		progressBar.setStyle(clonedStyle);

		Label currentLevelLabel = new Label(String.valueOf(skillLevel), managementSkin, labelStyleName);
		Label nextLevelLabel = new Label(String.valueOf(nextLevel), managementSkin, labelStyleName);
		Table progressRow = new Table();
		progressRow.add(currentLevelLabel).spaceRight(12f);
		progressRow.add(progressBar).width(70).spaceRight(12f);
		progressRow.add(nextLevelLabel);

		if (SkillDictionary.NULL_PROFESSION.equals(skill)) {
			progressRow.setVisible(false);
		}
		return progressRow;
	}

	class DraggableProfession extends DragAndDrop.Source {
		private Image originalImage;
		private final DragAndDrop dragAndDrop;
		private final int professionPriority;

		public DraggableProfession(DragAndDrop dragAndDrop, Actor actor, Image originalImage, int professionPriority) {
			super(actor);
			this.originalImage = originalImage;
			this.dragAndDrop = dragAndDrop;
			this.professionPriority = professionPriority;
		}

		@Override
		public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
			Image dragActor = new Image(originalImage.getDrawable());
			DragAndDrop.Payload payload = new DragAndDrop.Payload();
			payload.setDragActor(dragActor);
			dragAndDrop.setDragActorPosition(dragActor.getWidth() / 2, -dragActor.getHeight() / 2);
			return payload;
		}
	}

	class DraggableProfessionTarget extends DragAndDrop.Target {
		private final Table column;
		private final int professionPriority;
		private final SkillsComponent skillsComponent;
		private final Drawable dragOverTint;
		private final Table wholeTable;
		private final Entity settler;
		private final Consumer<Entity> onProfessionChange;
		private final float scale;

		public DraggableProfessionTarget(Table column, int professionPriority, SkillsComponent skillsComponent, ManagementSkin managementSkin, Table wholeTable, Entity settler, Consumer<Entity> onProfessionChange, float scale) {
			super(column);
			this.column = column;
			this.professionPriority = professionPriority;
			this.skillsComponent = skillsComponent;
			this.dragOverTint = managementSkin.getDrawable("drag_over_tint");
			this.wholeTable = wholeTable;
			this.settler = settler;
			this.onProfessionChange = onProfessionChange;
			this.scale = scale;
		}

		@Override
		public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
			column.setBackground(dragOverTint);
			return true;
		}

		@Override
		public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
			super.reset(source, payload);
			column.setBackground((Drawable) null);
		}

		@Override
		public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
			if (source instanceof DraggableProfession draggableProfession) {
				skillsComponent.swapActiveProfessionPositions(draggableProfession.professionPriority, this.professionPriority);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, settler);
				SettlerProfessionFactory.this.addProfessionComponents(settler, wholeTable, onProfessionChange, scale);
				onProfessionChange.accept(settler);
			}
		}
	}


	class ChangeProfessionDialog extends GameDialog {
		private static final int ITEMS_PER_ROW = 6;

		public ChangeProfessionDialog(I18nTranslator i18nTranslator, Skin skin,
		                              MessageDispatcher messageDispatcher, SkillDictionary skillDictionary,
		                              SoundAssetDictionary soundAssetDictionary, Entity settler, int activeSkillIndex, Table wholeTable, Consumer<Entity> onProfessionChange, final float scale) {
			super(i18nTranslator.getTranslatedString("GUI.CHANGE_PROFESSION_LABEL"), skin, messageDispatcher, soundAssetDictionary);

			int numAdded = 0;

			List<Skill> professionsForSelection = new ArrayList<>(skillDictionary.getSelectableProfessions());
			if (!professionsForSelection.contains(SkillDictionary.NULL_PROFESSION)) {
				professionsForSelection.add(SkillDictionary.NULL_PROFESSION);
			}

			SkillsComponent skillsComponent = settler.getComponent(SkillsComponent.class);
			for (SkillsComponent.QuantifiedSkill quantifiedSkill : skillsComponent.getActiveProfessions()) {
				professionsForSelection.remove(quantifiedSkill.getSkill());
			}


			for (Skill profession : professionsForSelection) {
				Table innerTable = new Table();

				Image image = new Image(managementSkin.getDrawable(profession.getIcon()));
				image.setTouchable(Touchable.enabled);
				image.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						messageDispatcher.dispatchMessage(MessageType.CHANGE_PROFESSION, new ChangeProfessionMessage(
								settler, activeSkillIndex, profession
						));
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
						SettlerProfessionFactory.this.addProfessionComponents(settler, wholeTable, onProfessionChange, scale);
						close();
					}
				});

				innerTable.add(image).pad(10).row();
				innerTable.add(buildProgressBarRow(skillsComponent, profession, true));
				image.addListener(new ChangeCursorOnHover(image, GameCursor.SELECT, messageDispatcher));
				image.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
				tooltipFactory.simpleTooltip(image, profession.getI18nKey(), TooltipLocationHint.BELOW);
				contentTable.add(innerTable).pad(3);
				numAdded++;

				if (numAdded % ITEMS_PER_ROW == 0) {
					contentTable.row();
				}
			}

			if (activeSkillIndex > 0) {
				contentTable.add(removeProfessionOption(messageDispatcher, soundAssetDictionary, settler, activeSkillIndex, wholeTable, onProfessionChange, scale)).pad(3);
			}
		}

		private Table removeProfessionOption(MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary, Entity settler, int activeSkillIndex, Table wholeTable, Consumer<Entity> onProfessionChange, float scale) {
			Table innerTable = new Table();
			Image image = new Image(noneSelectedDrawableOption);
			image.setTouchable(Touchable.enabled);
			image.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.REMOVE_PROFESSION, new MessageType.RemoveProfessionMessage(
							settler, activeSkillIndex
					));
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
					SettlerProfessionFactory.this.addProfessionComponents(settler, wholeTable, onProfessionChange, scale);
					close();
				}
			});

			innerTable.add(image).pad(10).row();
			image.addListener(new ChangeCursorOnHover(image, GameCursor.SELECT, messageDispatcher));
			image.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			tooltipFactory.simpleTooltip(image, "ITEM.NONE_SELECTED", TooltipLocationHint.BELOW);
			return innerTable;
		}

		@Override
		public void dispose() {

		}
	}


}
