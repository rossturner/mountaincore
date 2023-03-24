package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.*;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserCallback;
import net.spookygames.gdx.nativefilechooser.NativeFileChooserConfiguration;
import org.apache.commons.lang3.text.WordUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.editor.NormalMapGenerator;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.model.ShowImportFileDialogMessage;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.model.*;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.persistence.FileUtils;
import technology.rocketjump.mountaincore.rendering.RenderMode;
import technology.rocketjump.mountaincore.rendering.entities.AnimationStudio;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;

@Singleton
public class SpriteDescriptorsPane extends VisTable {
    public static final String TRUE_COLOUR = "(none/true color)";
    private final NativeFileChooser fileChooser;
    private final MessageDispatcher messageDispatcher;
    private final NormalMapGenerator normalMapGenerator;
    private final EditorStateProvider editorStateProvider;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final SoundAssetDictionary soundAssetDictionary;
    private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
    private final AnimationStudio animationStudio;

    @Inject
    public SpriteDescriptorsPane(NativeFileChooser fileChooser, MessageDispatcher messageDispatcher,
                                 NormalMapGenerator normalMapGenerator, EditorStateProvider editorStateProvider,
                                 EntityAssetTypeDictionary entityAssetTypeDictionary, AnimationStudio animationStudio,
                                 SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
        this.fileChooser = fileChooser;
        this.messageDispatcher = messageDispatcher;
        this.normalMapGenerator = normalMapGenerator;
        this.editorStateProvider = editorStateProvider;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.animationStudio = animationStudio;
        this.soundAssetDictionary = soundAssetDictionary;
        this.particleEffectTypeDictionary = particleEffectTypeDictionary;
    }

    public void showSpriteDescriptorControls(EntityAsset entityAsset, EntityType entityType, List<EntityAssetOrientation> orientations, List<ColoringLayer> coloringLayers) {
        this.clearChildren();

        for (EntityAssetOrientation orientation : orientations) {
            final SpriteDescriptor spriteDescriptor;
            boolean hasOrientation = entityAsset.getSpriteDescriptors().containsKey(orientation);
            if (hasOrientation) {
                spriteDescriptor = entityAsset.getSpriteDescriptors().get(orientation);
            } else {
                spriteDescriptor = new SpriteDescriptor();
            }
            VisTable orientationTable = new VisTable();
            orientationTable.setFillParent(true);
            CollapsibleWidget collapsibleOrientation = new CollapsibleWidget(orientationTable);
            collapsibleOrientation.setCollapsed(!hasOrientation);

            VisCheckBox orientationCheckbox = WidgetBuilder.checkBox(orientation.name(), hasOrientation, checked -> {
                entityAsset.getSpriteDescriptors().put(orientation, spriteDescriptor);
                collapsibleOrientation.setCollapsed(false);
            },
            unchecked -> {
                entityAsset.getSpriteDescriptors().remove(orientation);
                collapsibleOrientation.setCollapsed(true);
            });

            this.addSeparator().row();
            this.add(orientationCheckbox).padBottom(15).colspan(2).left().row();

            Path currentDescriptorsPath = FileUtils.getDirectory(Paths.get(editorStateProvider.getState().getAssetSelection().getDescriptorsPath()));

            InputValidator filenameExists = new InputValidator() {
                @Override
                public boolean validateInput(String filename) {
                    FileHandle fileHandle = new FileHandle(currentDescriptorsPath.resolve(filename).toFile());
                    return fileHandle.exists() && !fileHandle.isDirectory();
                }
            };


            VisTextButton browseButton = new VisTextButton("Browse");
            VisTextField filenameField = WidgetBuilder.textField(spriteDescriptor.getFilename(), filename -> {
                if (filenameExists.validateInput(filename)) {
                    Path imageFile = currentDescriptorsPath.resolve(filename);
                    FileHandle fileHandle = new FileHandle(imageFile.toFile());
                    try {
                        displaySprite(fileHandle, spriteDescriptor);
                        spriteDescriptor.setFilename(filename);
                    } catch (GdxRuntimeException gdxRuntimeException) {
                        //TODO: tell user the image file does not work
                        FileUtils.delete(imageFile);
                    }
                }
            }, filenameExists);

            browseButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    fileChooser.chooseFile(buildConfig(), new NativeFileChooserCallback() {
                        @Override
                        public void onFileChosen(FileHandle selectedFile) {
                            Path modDirectory = editorStateProvider.getState().getModDirPath();
                            Path selectedImageDirectory = FileUtils.getDirectory(Path.of(selectedFile.path()));

                            Consumer<FileHandle> callback = fileHandle -> {
                                String filename = fileHandle.name();
                                spriteDescriptor.setFilename(filename);
                                filenameField.setText(filename);
                                displaySprite(fileHandle, spriteDescriptor);
                            };

                            if (selectedImageDirectory.startsWith(modDirectory.toAbsolutePath())) {
                                // Image already in mod directory
                                callback.accept(selectedFile);
                            } else {
                                StringJoiner suggestedFileNameBuilder = new StringJoiner("_", "", ".png");
                                suggestedFileNameBuilder.add(entityAsset.getUniqueName()).add(orientation.name());
                                String suggestedFilename = WordUtils.capitalizeFully(suggestedFileNameBuilder.toString(), '_', '-');
                                Path destinationPath = FileUtils.getDirectory(Paths.get(editorStateProvider.getState().getAssetSelection().getDescriptorsPath()));
                                FileHandle destination = new FileHandle(destinationPath.toFile());
                                messageDispatcher.dispatchMessage(MessageType.EDITOR_SHOW_IMPORT_FILE_DIALOG, new ShowImportFileDialogMessage(selectedFile, destination, suggestedFilename, callback));
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
            filenameField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    spriteDescriptor.setFilename(filenameField.getText());
                    Path currentDescriptorsPath = FileUtils.getDirectory(Paths.get(editorStateProvider.getState().getAssetSelection().getDescriptorsPath()));
                    FileHandle fileHandle = new FileHandle(currentDescriptorsPath.resolve(filenameField.getText()).toFile());
                    if (fileHandle.exists() && !fileHandle.isDirectory()) {
                        displaySprite(fileHandle, spriteDescriptor);
                    }

                }
            });
            VisTable rowTable = new VisTable();
            rowTable.add(filenameField).left().expandX().fillX().padRight(5);
            rowTable.add(browseButton);
            orientationTable.add(rowTable).left().expandX().fillX().colspan(2).row();

            VisCheckBox animatedCheckbox = new VisCheckBox("is Animated");
            animatedCheckbox.setChecked(spriteDescriptor.getIsAnimated());
            animatedCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    spriteDescriptor.setIsAnimated(animatedCheckbox.isChecked());
                }
            });
            orientationTable.add(animatedCheckbox).left().colspan(2).row();

            VisLabel coloringLayerLabel = new VisLabel("Coloring layer:");
            VisSelectBox<String> coloringLayerSelect = new VisSelectBox<>();
            Collection<String> applicableLayers = getApplicableColoringLayers(coloringLayers);
            if (spriteDescriptor.getColoringLayer() != null && !applicableLayers.contains(spriteDescriptor.getColoringLayer().name())) {
                Logger.error(spriteDescriptor.getColoringLayer() + " is not included in applicable coloring layers for " + entityType);
            }
            coloringLayerSelect.setItems(orderedArray(applicableLayers, TRUE_COLOUR));
            coloringLayerSelect.setSelected(spriteDescriptor.getColoringLayer() == null ? TRUE_COLOUR : spriteDescriptor.getColoringLayer().name());
            coloringLayerSelect.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (coloringLayerSelect.getSelected().equals(TRUE_COLOUR)) {
                        spriteDescriptor.setColoringLayer(null);
                    } else {
                        spriteDescriptor.setColoringLayer(ColoringLayer.valueOf(coloringLayerSelect.getSelected()));
                    }
                }
            });
            orientationTable.add(coloringLayerLabel).left();
            orientationTable.add(coloringLayerSelect).left().row();

            orientationTable.add(new VisLabel("Scale:")).left();
            VisTextField scaleField = new VisTextField(String.valueOf(spriteDescriptor.getScale()));
            scaleField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        Float value = Float.valueOf(scaleField.getText());
                        if (value != null) {
                            spriteDescriptor.setScale(value);
                        }
                    } catch (NumberFormatException e) {

                    }
                }
            });
            orientationTable.add(scaleField).left().row();

            VisCheckBox flipXCheckbox = new VisCheckBox("Flip X");
            flipXCheckbox.setChecked(spriteDescriptor.isFlipX());
            flipXCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    spriteDescriptor.setFlipX(flipXCheckbox.isChecked());
                    for (RenderMode renderMode : RenderMode.values()) {
                        Sprite sprite = spriteDescriptor.getSprite(renderMode);
                        if (sprite != null) {
                            sprite.setFlip(spriteDescriptor.isFlipX(), spriteDescriptor.isFlipY());
                        }
                    }
                }
            });
            orientationTable.add(flipXCheckbox).left();
            VisCheckBox flipYCheckbox = new VisCheckBox("Flip Y");
            flipYCheckbox.setChecked(spriteDescriptor.isFlipY());
            flipYCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    spriteDescriptor.setFlipY(flipYCheckbox.isChecked());
                    for (RenderMode renderMode : RenderMode.values()) {
                        Sprite sprite = spriteDescriptor.getSprite(renderMode);
                        if (sprite != null) {
                            sprite.setFlip(spriteDescriptor.isFlipX(), spriteDescriptor.isFlipY());
                        }
                    }
                }
            });
            orientationTable.add(flipYCheckbox).left().row();


            orientationTable.add(new OffsetPixelsWidget(spriteDescriptor.getOffsetPixels())).left().colspan(2).row();


            addChildAssetsWidgets("Child assets (click to show)", spriteDescriptor.getChildAssets(), orientationTable, entityAssetTypeDictionary.getByEntityType(entityType));
            addChildAssetsWidgets("Attachment points (click to show)", spriteDescriptor.getAttachmentPoints(), orientationTable, entityAssetTypeDictionary.getByEntityType(entityType));
            addChildAssetsWidgets("Parent entity assets (click to show)", spriteDescriptor.getParentEntityAssets(), orientationTable, entityAssetTypeDictionary.getAll());

            orientationTable.add(new AnimationsWidget(animationStudio, spriteDescriptor, soundAssetDictionary, particleEffectTypeDictionary)).left().colspan(2).row();

            this.add(collapsibleOrientation).expandX().fillX().row();
        }
    }


    private void displaySprite(FileHandle fileHandle, SpriteDescriptor spriteDescriptor) {
        Texture texture = new Texture(fileHandle);
        Sprite sprite = new Sprite(texture);
        sprite.setFlip(spriteDescriptor.isFlipX(), spriteDescriptor.isFlipY());
        spriteDescriptor.setSprite(RenderMode.DIFFUSE, sprite); //TODO: Assumes Diffuse is selected

        Path generatedNormalFile = normalMapGenerator.generate(fileHandle.file().toPath());
        Texture normalTexture = new Texture(new LwjglFileHandle(generatedNormalFile.toAbsolutePath().toFile(), Files.FileType.Absolute));
        Sprite normalSprite = new Sprite(normalTexture);
        sprite.setFlip(spriteDescriptor.isFlipX(), spriteDescriptor.isFlipY());
        spriteDescriptor.setSprite(RenderMode.NORMALS, normalSprite);

        messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, editorStateProvider.getState().getCurrentEntity());
    }

    private void addChildAssetsWidgets(String labelText, List<EntityChildAssetDescriptor> childAssets, VisTable orientationTable, Collection<EntityAssetType> applicableTypes) {
        VisLabel label = new VisLabel(labelText);
        ChildAssetsWidget childAssetsWidget = new ChildAssetsWidget(childAssets, applicableTypes, animationStudio.getAvailableAnimationNames());
        CollapsibleWidget collapsibleChildAssets = new CollapsibleWidget(childAssetsWidget);
        collapsibleChildAssets.setCollapsed(childAssets.isEmpty());
        label.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                collapsibleChildAssets.setCollapsed(!collapsibleChildAssets.isCollapsed());
            }
        });
        orientationTable.add(label).left().colspan(2).row();
        orientationTable.add(collapsibleChildAssets).padLeft(20).left().expandX().fillX().colspan(2).row();
    }

    private NativeFileChooserConfiguration buildConfig() {
        NativeFileChooserConfiguration config = new NativeFileChooserConfiguration();
        config.nameFilter = (dir, name) -> name.endsWith("png");
        config.title = "Select sprite file (.png)";
        return config;
    }


    private List<String> getApplicableColoringLayers(List<ColoringLayer> coloringLayers) {
        return coloringLayers.stream().map(Enum::name).collect(Collectors.toList());
    }

}
