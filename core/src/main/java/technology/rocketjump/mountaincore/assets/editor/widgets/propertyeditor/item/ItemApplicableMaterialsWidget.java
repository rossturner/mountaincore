package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.item;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

import java.util.List;

public class ItemApplicableMaterialsWidget extends VisTable {

	private final List<String> sourceData;
	private final VisTextButton addButton;
	private final VisTable innerTable = new VisTable();
	private final GameMaterialDictionary materialDictionary;

	public ItemApplicableMaterialsWidget(List<String> sourceData, GameMaterialDictionary materialDictionary) {
		this.sourceData = sourceData;
		this.materialDictionary = materialDictionary;

		addButton = new VisTextButton("Add another");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				addUnpickedMaterial();
				reload();
			}
		});

		CollapsibleWidget collapsible = new CollapsibleWidget(innerTable);
		collapsible.setCollapsed(sourceData.isEmpty());
		VisLabel headerLabel = new VisLabel("Applicable materials (click to show)");
		headerLabel.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				collapsible.setCollapsed(!collapsible.isCollapsed());
			}
		});

		this.add(headerLabel).left().expandX().row();
		this.add(innerTable).left().pad(4).padLeft(20).expandX().row();

		reload();
	}


	private void reload() {
		innerTable.clearChildren();
		List<String> allNames = materialDictionary.getAll().stream().map(GameMaterial::getMaterialName).toList();

		for (String currentMaterialName : sourceData) {
			innerTable.add(WidgetBuilder.select(currentMaterialName, allNames, null, newMaterial -> {
				sourceData.remove(currentMaterialName);
				sourceData.add(newMaterial);
			}));
			innerTable.add(WidgetBuilder.button("Remove", (a) -> {
				sourceData.remove(currentMaterialName);
				reload();
			}));
			innerTable.row();
		}

		innerTable.add(addButton).left().row();
	}

	private void addUnpickedMaterial() {
		for (GameMaterial gameMaterial : materialDictionary.getAll()) {
			if (!sourceData.contains(gameMaterial.getMaterialName())) {
				sourceData.add(gameMaterial.getMaterialName());
				return;
			}
		}
	}
}
