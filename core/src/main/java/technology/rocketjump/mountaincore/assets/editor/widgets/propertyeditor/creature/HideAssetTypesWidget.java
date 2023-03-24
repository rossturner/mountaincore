package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.creature;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;

import java.util.Collection;
import java.util.Map;

public class HideAssetTypesWidget extends VisTable {
	private final Map<EntityAssetType, Float> sourceData;
	private final VisTextButton addButton;
	private final Collection<EntityAssetType> applicableTypes;

	public HideAssetTypesWidget(Map<EntityAssetType, Float> sourceData, Collection<EntityAssetType> applicableTypes) {
		this.sourceData = sourceData;
		this.applicableTypes = applicableTypes;

		addButton = new VisTextButton("Add hide asset type");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				sourceData.put(pickNextType(), 0.5f);
				reload();
			}
		});

		reload();
	}


	private void reload() {
		this.clearChildren();

		for (Map.Entry<EntityAssetType, Float> entry : sourceData.entrySet()) {
			this.add(WidgetBuilder.select(entry.getKey(), applicableTypes, null, selected -> {
				float currentValue = sourceData.get(entry.getKey());
				sourceData.remove(entry.getKey());
				sourceData.put(selected, currentValue);
				reload();
			}));
			this.add(WidgetBuilder.floatSpinner(entry.getValue(), 0, 1, newValue -> {
				sourceData.put(entry.getKey(), newValue);
			}));
			this.add(WidgetBuilder.button("Remove", a -> {
				sourceData.remove(entry.getKey());
				reload();
			}));
			this.row();
		}

		if (sourceData.size() < applicableTypes.size()) {
			this.add(addButton).colspan(2).right().row();
		}
	}

	private EntityAssetType pickNextType() {
		for (EntityAssetType applicableType : applicableTypes) {
			if (!sourceData.containsKey(applicableType)) {
				return applicableType;
			}
		}
		return applicableTypes.iterator().next();
	}
}
