package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.*;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserCallback;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserConfiguration;
import org.apache.commons.beanutils.PropertyUtils;
import technology.rocketjump.saul.assets.editor.model.ColorPickerMessage;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;
import static technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary.initialiseSpeciesColor;

public class ColorsWidget extends VisTable {

	private final Map<ColoringLayer, SpeciesColor> sourceData;
	private final VisTextButton addButton;
	private final List<ColoringLayer> applicableColoringLayers;
	private final EntityType entityType;
	private final Path filePath;
	private final NativeFileChooser fileChooser;
	private final MessageDispatcher messageDispatcher;

	public ColorsWidget(Map<ColoringLayer, SpeciesColor> sourceData, List<ColoringLayer> applicableColoringLayers,
						EntityType entityType, Path filePath, NativeFileChooser fileChooser, MessageDispatcher messageDispatcher) {
		this.sourceData = sourceData;
		this.applicableColoringLayers = applicableColoringLayers;
		this.entityType = entityType;
		this.filePath = filePath;
		this.fileChooser = fileChooser;
		this.messageDispatcher = messageDispatcher;

		addButton = new VisTextButton("Add another");
		addButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ColoringLayer next = null;
				for (ColoringLayer cl : applicableColoringLayers) {
					if (!sourceData.containsKey(cl)) {
						next = cl;
						break;
					}
				}
				SpeciesColor speciesColor = new SpeciesColor();
				speciesColor.setColorCode(HexColors.toHexString(Color.MAGENTA));
				sourceData.put(next, speciesColor);
				reload();
			}
		});

		this.reload();
	}

	private enum ColorValueType {

		swatch,
		transitionSwatch,
		colorChart,
		colorCode

	}

	private void reload() {
		this.clearChildren();

		for (Map.Entry<ColoringLayer, SpeciesColor> entry : sourceData.entrySet()) {
			VisSelectBox<ColoringLayer> layerSelect = new VisSelectBox<>();
			layerSelect.setItems(orderedArray(applicableColoringLayers));
			layerSelect.setSelected(entry.getKey());
			layerSelect.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					ColoringLayer oldLayer = entry.getKey();
					ColoringLayer newLayer = layerSelect.getSelected();

					sourceData.put(newLayer, sourceData.get(oldLayer));
					sourceData.remove(oldLayer);
					reload();
				}
			});
			this.add(layerSelect).left();

			VisSelectBox<ColorValueType> typeSelect = new VisSelectBox<>();
			typeSelect.setItems(orderedArray(List.of(ColorValueType.values())));
			if (entry.getValue().getSwatch() != null) {
				typeSelect.setSelected(ColorValueType.swatch);
			} else if (entry.getValue().getTransitionSwatch() != null) {
				typeSelect.setSelected(ColorValueType.transitionSwatch);
			} else if (entry.getValue().getColorChart() != null) {
				typeSelect.setSelected(ColorValueType.colorChart);
			} else if (entry.getValue().getColorCode() != null) {
				typeSelect.setSelected(ColorValueType.colorCode);
			}
			typeSelect.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					entry.getValue().clear();
					switch (typeSelect.getSelected()) {
						case swatch -> entry.getValue().setSwatch("FILENAME");
						case transitionSwatch -> entry.getValue().setTransitionSwatch("FILENAME");
						case colorChart -> entry.getValue().setColorChart("FILENAME");
						case colorCode -> {
							Color defaultColor = Color.MAGENTA;
							entry.getValue().setColorCode(HexColors.toHexString(defaultColor));
							entry.getValue().setSpecificColor(defaultColor);
						}
					}
					reload();
				}
			});
			this.add(typeSelect).left();

			if (typeSelect.getSelected().equals(ColorValueType.colorCode)) {
				VisTextField colorCodeField = new VisTextField(entry.getValue().getColorCode());
				VisTextField.VisTextFieldStyle colorCodeStyle = new VisTextField.VisTextFieldStyle(colorCodeField.getStyle());
				colorCodeStyle.fontColor = entry.getValue().getSpecificColor();
				colorCodeField.setStyle(colorCodeStyle);

				colorCodeField.setText(entry.getValue().getColorCode());
				colorCodeField.addListener(new ChangeListener() {
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						Color validatedColor = HexColors.get(colorCodeField.getText());
						if (validatedColor != null) {
							entry.getValue().setColorCode(colorCodeField.getText());
							initialiseSpeciesColor(entityType, entry.getValue());
							colorCodeStyle.fontColor = entry.getValue().getSpecificColor();
						}
					}
				});
				this.add(colorCodeField).expandX().fillX();

				VisTextButton colorPickerButton = new VisTextButton("Picker");
				colorPickerButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_COLOR_PICKER,
								new ColorPickerMessage(entry.getValue().getColorCode(), (color) -> {
									entry.getValue().setColorCode(HexColors.toHexString(color));
									entry.getValue().setSpecificColor(color);
									colorCodeStyle.fontColor = color;
									colorCodeField.setText(entry.getValue().getColorCode());
								}));
					}
				});
				this.add(colorPickerButton).row();

			} else {
				buildFileSelector(entry.getValue(), typeSelect.getSelected());
			}

			VisCheckBox hiddenCheckbox = new VisCheckBox("hidden");
			hiddenCheckbox.setChecked(entry.getValue().isHidden());
			hiddenCheckbox.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					entry.getValue().setHidden(hiddenCheckbox.isChecked());
				}
			});
			this.add(hiddenCheckbox).left();

			VisTextButton removeButton = new VisTextButton("Remove");
			removeButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					sourceData.remove(entry.getKey());
					reload();
				}
			});
			this.add(removeButton).colspan(3).row();

			this.addSeparator().colspan(4).row();
		}

		this.add(addButton).right().padRight(20).colspan(3).row();
	}

	private void buildFileSelector(SpeciesColor instance, ColorValueType field) {
		try {
			Object property = PropertyUtils.getProperty(instance, field.name());
			VisTextField filenameField = new VisTextField(property == null ? "" : property.toString());
			filenameField.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					try {
						PropertyUtils.setProperty(instance, field.name(), filenameField.getText());
						initialiseSpeciesColor(entityType, instance);
					} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
					}
				}
			});
			this.add(filenameField).expandX().fillX();

			VisTextButton browseButton = new VisTextButton("Browse");
			browseButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					fileChooser.chooseFile(buildConfig(filePath), new NativeFileChooserCallback() {
						@Override
						public void onFileChosen(FileHandle file) {
							String filename = file.name();
							try {
								PropertyUtils.setProperty(instance, field.name(), filenameField.getText());
								filenameField.setText(filename);
								initialiseSpeciesColor(entityType, instance);
							} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
							}
						}

						@Override
						public void onCancellation() {

						}

						@Override
						public void onError(Exception exception) {

						}
					});
				}
			});
			this.add(browseButton).left().row();

		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
		}
	}

	private NativeFileChooserConfiguration buildConfig(Path filePath) {
		NativeFileChooserConfiguration config = new NativeFileChooserConfiguration();
		config.directory = Gdx.files.local(filePath.getParent().toString());
		config.nameFilter = (dir, name) -> name.endsWith("png");
		config.title = "Select swatch file (.png)";
		return config;
	}

}
