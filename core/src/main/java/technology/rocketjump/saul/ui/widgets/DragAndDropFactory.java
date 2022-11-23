package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.ManagementSkin;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DragAndDropFactory {
	private final ManagementSkin managementSkin;

	@Inject
	public DragAndDropFactory(GuiSkinRepository skinRepository) {
		this.managementSkin = skinRepository.getManagementSkin();
	}

	public Table buildDragAndDropSkill(DragAndDrop dragAndDrop, SkillsComponent skillsComponent, int index) {
		Table column = new Table();
		Image numberIcon = new Image(managementSkin.getDrawable("icon_" + (index + 1)));
		column.add(numberIcon).row();

		java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();
		if (index < activeProfessions.size()) {
			SkillsComponent.QuantifiedSkill activeSkill = activeProfessions.get(index);
			Skill skill = activeSkill.getSkill();

			Image draggableImage = new Image(managementSkin.getDrawable(skill.getDraggableIcon()));
			dragAndDrop.addSource(new DraggableProfession(dragAndDrop, draggableImage, index));
			dragAndDrop.addTarget(new DraggableProfessionTarget(draggableImage, column, index, skillsComponent, managementSkin));

			int percent = (int) (skillsComponent.getNextLevelProgressPercent(skill) * 100);
			int skillLevel = skillsComponent.getSkillLevel(skill);
			int nextLevel = skillLevel + 1;
			ProgressBar progressBar = new ProgressBar(0, 100, 1, false, managementSkin);
			progressBar.setValue(percent);
			progressBar.setDisabled(true);
			ProgressBar.ProgressBarStyle clonedStyle = new ProgressBar.ProgressBarStyle(progressBar.getStyle());
			if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
				clonedStyle.knobBefore = ninePatchDrawable.tint(managementSkin.getColor("progress_bar_green"));
			}
			progressBar.setStyle(clonedStyle);

			Label currentLevelLabel = new Label(String.valueOf(skillLevel), managementSkin, "item_type_name_label");
			Label nextLevelLabel = new Label(String.valueOf(nextLevel), managementSkin, "item_type_name_label");
			Table progressRow = new Table();
			progressRow.add(currentLevelLabel).spaceRight(12f);
			progressRow.add(progressBar).spaceRight(12f);
			progressRow.add(nextLevelLabel);


			column.add(draggableImage).spaceTop(10f).spaceBottom(6f).row();
			column.add(progressRow);

		} else {
			//todo: fill with villager thing
		}

		return column;
	}

	class DraggableProfession extends DragAndDrop.Source {
		private Image originalImage;
		private final DragAndDrop dragAndDrop;
		private final int professionPriority;

		public DraggableProfession(DragAndDrop dragAndDrop, Image actor, int professionPriority) {
			super(actor);
			this.originalImage = actor;
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
		private final Image originalImage;
		private final Table column;
		private final int professionPriority;
		private final SkillsComponent skillsComponent;
		private final Drawable dragOverTint;

		public DraggableProfessionTarget(Image originalImage, Table column, int professionPriority, SkillsComponent skillsComponent, ManagementSkin managementSkin) {
			super(column);
			this.originalImage = originalImage;
			this.column = column;
			this.professionPriority = professionPriority;
			this.skillsComponent = skillsComponent;
			this.dragOverTint = managementSkin.getDrawable("drag_over_tint");
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

				//TODO: consider rebuilding whole column for progress bar
				Drawable toSwap = draggableProfession.originalImage.getDrawable();
				draggableProfession.originalImage.setDrawable(this.originalImage.getDrawable());
				this.originalImage.setDrawable(toSwap);

				skillsComponent.swapActiveProfessionPositions(draggableProfession.professionPriority, this.professionPriority);
			}
		}
	}
}
