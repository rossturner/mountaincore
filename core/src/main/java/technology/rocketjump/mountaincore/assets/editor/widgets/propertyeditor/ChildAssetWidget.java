package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.*;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.model.EntityChildAssetDescriptor;

import java.util.Collection;
import java.util.Set;

import static technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;

public class ChildAssetWidget extends VisTable {

	public ChildAssetWidget(EntityChildAssetDescriptor childDescriptor, Collection<EntityAssetType> applicableTypes, Set<String> inheritableAnimationNames) {
		VisSelectBox<EntityAssetType> assetTypeSelectBox = new VisSelectBox<>();
		assetTypeSelectBox.setItems(orderedArray(applicableTypes));
		if (childDescriptor.getType() == null) {
			childDescriptor.setType(applicableTypes.iterator().next());
		}
		assetTypeSelectBox.setSelected(childDescriptor.getType());
		assetTypeSelectBox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				childDescriptor.setType(assetTypeSelectBox.getSelected());
			}
		});
		this.add(new VisLabel("Asset type:")).left();
		this.add(assetTypeSelectBox).expandX().fillX().row();

		this.add(new OffsetPixelsWidget(childDescriptor.getOffsetPixels())).colspan(2).left().row();

		this.add(new VisLabel("Override Render Layer (optional)")).left();
		VisTextField renderLayerField = new VisTextField(childDescriptor.getOverrideRenderLayer() == null ? "" : String.valueOf(childDescriptor.getOverrideRenderLayer()));
		renderLayerField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					if (renderLayerField.getText().equals("")) {
						childDescriptor.setOverrideRenderLayer(null);
					} else {
						Integer newValue = Integer.valueOf(renderLayerField.getText());
						if (newValue != null) {
							childDescriptor.setOverrideRenderLayer(newValue);
						}
					}
				} catch (NumberFormatException e) {

				}
			}
		});
		this.add(renderLayerField).left().row();

		this.add(new VisLabel("Specified Asset Unique Name (optional):")).left().colspan(2).row();
		VisTextField specificNameField = new VisTextField(childDescriptor.getSpecificAssetName() == null ? "" : childDescriptor.getSpecificAssetName());
		specificNameField.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String newValue = specificNameField.getText();
				if (newValue.equals("")) {
					childDescriptor.setSpecificAssetName(null);
				} else {
					childDescriptor.setSpecificAssetName(newValue);
				}
			}
		});
		this.add(specificNameField).left().expandX().fillX().colspan(2).row();

		Label inheritAnimationsLabel = WidgetBuilder.label("Inherit Animations (click to show):");
		this.add(inheritAnimationsLabel).left().row();

		VisTable inheritAnimationsCheckboxes = WidgetBuilder.checkboxes(childDescriptor.getInheritAnimations(), inheritableAnimationNames, childDescriptor.getInheritAnimations()::add, childDescriptor.getInheritAnimations()::remove);
		CollapsibleWidget inheritAnimationsCollapsible = new CollapsibleWidget(inheritAnimationsCheckboxes);
		inheritAnimationsCollapsible.setCollapsed(childDescriptor.getInheritAnimations().isEmpty());
		inheritAnimationsLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				inheritAnimationsCollapsible.setCollapsed(!inheritAnimationsCollapsible.isCollapsed());
			}
		});
		this.add(inheritAnimationsCollapsible);
		this.row();


		this.addSeparator().colspan(2).row();
	}
}
