package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor;

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import technology.rocketjump.mountaincore.assets.entities.model.AnimationScript;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.mountaincore.assets.entities.model.StorableVector2;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;
import technology.rocketjump.mountaincore.rendering.entities.AnimationStudio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AnimationsWidget extends VisTable {

	private static final float DURATION_STEP = 0.1f;
	private static final int DURATION_SCALE = 3;
	private static final int DURATION_WIDGET_WIDTH = 80;
	private final AnimationStudio animationStudio;
	private final SpriteDescriptor spriteDescriptor;
	private final List<String> soundAssetNames;
	private final List<String> particleEffectTypeNames;

	public AnimationsWidget(AnimationStudio animationStudio, SpriteDescriptor spriteDescriptor,
							SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.animationStudio = animationStudio;
		this.spriteDescriptor = spriteDescriptor;
		soundAssetNames = soundAssetDictionary.getAll().stream().map(SoundAsset::getName).sorted().toList();
		particleEffectTypeNames = particleEffectTypeDictionary.getAll().stream().map(ParticleEffectType::getName).sorted().toList();
		reload();
	}

	public void reload() {
		clear();
		defaults().left().padLeft(0);
		add(new VisLabel("Animations")).left().colspan(2).row();
		defaults().padLeft(20);

		Map<String, AnimationScript> scripts = spriteDescriptor.getAnimationScripts();
		if (scripts != null) {
			for (Map.Entry<String, AnimationScript> scriptEntry : scripts.entrySet()) {
				String scriptName = scriptEntry.getKey();
				AnimationScript script = scriptEntry.getValue();
				add(new VisLabel(scriptName)).padRight(10);
				add(WidgetBuilder.button("X", btn -> {
					scripts.remove(scriptName);
					reload();
				})).row();
				add(new VisLabel("Duration")).padRight(10);
				add(WidgetBuilder.floatSpinner(script.getDuration(), 0, Float.MAX_VALUE, d -> {
					script.setDuration(d);
					animationStudio.rebuildAnimation(scriptName);
				}, DURATION_STEP, DURATION_SCALE)).width(DURATION_WIDGET_WIDTH).row();


				add(new VisLabel("Rotations")).padRight(10);
				Table rotationFramesTable = new Table();
				add(WidgetBuilder.button("Add", textButton -> {
					if (script.getRotations() == null) {
						script.setRotations(new ArrayList<>());
					}
					AnimationScript.RotationFrame frame = new AnimationScript.RotationFrame();
					script.getRotations().add(frame);
					this.reload();
				})).row();
				add(rotationFramesTable).colspan(2).row();
				if (script.getRotations() != null) {
					rotationFramesTable.clear();
					for (AnimationScript.RotationFrame rotation : script.getRotations()) {
						rotationFramesTable.add(rotationWidget(rotation, script, scriptName)).row();
					}
				}

				add(new VisLabel("Translations")).padRight(10);
				Table translationsFrameTable = new Table();
				add(WidgetBuilder.button("Add", textButton -> {
					if (script.getTranslations() == null) {
						script.setTranslations(new ArrayList<>());
					}
					AnimationScript.TranslationFrame frame = new AnimationScript.TranslationFrame();
					frame.setVector2(new StorableVector2());
					script.getTranslations().add(frame);

					this.reload();
				})).row();
				add(translationsFrameTable).colspan(2).row();
				if (script.getTranslations() != null) {
					translationsFrameTable.clear();
					for (AnimationScript.TranslationFrame translation : script.getTranslations()) {
						translationsFrameTable.add(translationWidget(translation, script, scriptName)).row();
					}
				}

				add(new VisLabel("Scalings")).padRight(10);
				Table scalingsFrameTable = new Table();
				add(WidgetBuilder.button("Add", textButton -> {
					if (script.getScalings() == null) {
						script.setScalings(new ArrayList<>());
					}
					AnimationScript.ScalingFrame frame = new AnimationScript.ScalingFrame();
					frame.setVector2(new StorableVector2());
					frame.getVector2().x = 1.0f;
					frame.getVector2().y = 1.0f;
					script.getScalings().add(frame);

					this.reload();
				})).row();
				add(scalingsFrameTable).colspan(2).row();
				if (script.getScalings() != null) {
					scalingsFrameTable.clear();
					for (AnimationScript.ScalingFrame scaling : script.getScalings()) {
						scalingsFrameTable.add(scalingWidget(scaling, script, scriptName)).row();
					}
				}

				if (script.getRotations() != null || script.getTranslations() != null) {
					//TODO: duplication
					add(new VisLabel("Sound Cues")).padRight(10);
					Table soundCueFrameTable = new Table();
					add(WidgetBuilder.button("Add", textButton -> {
						if (script.getSoundCues() == null) {
							script.setSoundCues(new ArrayList<>());
						}
						AnimationScript.SoundCueFrame frame = new AnimationScript.SoundCueFrame();
						frame.setSoundAssetName(soundAssetNames.get(0)); //ensure sound asset always there
						script.getSoundCues().add(frame);

						this.reload();
					})).row();
					add(soundCueFrameTable).colspan(2).row();
					if (script.getSoundCues() != null) {
						soundCueFrameTable.clear();
						for (AnimationScript.SoundCueFrame soundCueFrame : script.getSoundCues()) {
							soundCueFrameTable.add(soundWidget(soundCueFrame, script, scriptName)).row();
						}
					}


					add(new VisLabel("Particle Effects")).padRight(10);
					Table particleEffectCueFrameTable = new Table();
					add(WidgetBuilder.button("Add", textButton -> {
						if (script.getParticleEffectCues() == null) {
							script.setParticleEffectCues(new ArrayList<>());
						}
						AnimationScript.ParticleEffectCueFrame frame = new AnimationScript.ParticleEffectCueFrame();
						frame.setParticleEffectName(particleEffectTypeNames.get(0)); //ensure particle effect is always there
						script.getParticleEffectCues().add(frame);

						this.reload();
					})).row();
					add(particleEffectCueFrameTable).colspan(2).row();
					if (script.getParticleEffectCues() != null) {
						particleEffectCueFrameTable.clear();
						for (AnimationScript.ParticleEffectCueFrame particleEffectCueFrame : script.getParticleEffectCues()) {
							particleEffectCueFrameTable.add(particleEffect(particleEffectCueFrame, script, scriptName)).row();
						}
					}
				}
			}
		}

		row();

		HashSet<String> available = new HashSet<>(animationStudio.getAvailableAnimationNames());
		available.removeAll(scripts.keySet());

		HorizontalGroup newAnimationGroup = new HorizontalGroup();
		VisLabel newAnimationLabel = new VisLabel("New Animation");
		VisSelectBox<String> animationNameSelect = WidgetBuilder.select(null, available, null, x -> {});
		VisTextButton newAnimationButton = WidgetBuilder.button("Add", b -> {
				scripts.put(animationNameSelect.getSelected(), new AnimationScript());
				reload();
		});

		newAnimationGroup.space(10);
		newAnimationGroup.addActor(newAnimationLabel);
		newAnimationGroup.addActor(animationNameSelect);
		newAnimationGroup.addActor(newAnimationButton);


		if (!available.isEmpty()) {
			add(newAnimationGroup).colspan(2).row();
		}
	}

	private Table translationWidget(AnimationScript.TranslationFrame translationFrame, AnimationScript script, String scriptName) {
		Table t = new Table();
		t.add(new VisLabel("At Time")).padRight(10).padLeft(20);
		t.add(WidgetBuilder.floatSpinner(translationFrame.getAtTime(), 0, Float.MAX_VALUE, d -> {
			translationFrame.setAtTime(d);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(translationFrame.getAtTime());
		}, DURATION_STEP, DURATION_SCALE)).width(DURATION_WIDGET_WIDTH).padRight(50);

		t.add(new VisLabel("X")).padRight(10);

		t.add(WidgetBuilder.floatSpinner(translationFrame.getVector2().getX(), -Float.MAX_VALUE, Float.MAX_VALUE, i1 -> {
			translationFrame.getVector2().x = i1;
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(translationFrame.getAtTime());
		}, 0.001f, 5)).width(DURATION_WIDGET_WIDTH).padRight(50);

		t.add(new VisLabel("Y")).padRight(10);
		t.add(WidgetBuilder.floatSpinner(translationFrame.getVector2().getY(), -Float.MAX_VALUE, Float.MAX_VALUE, i -> {
			translationFrame.getVector2().y  = i;
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(translationFrame.getAtTime());
		}, 0.001f, 5)).width(DURATION_WIDGET_WIDTH).padRight(50);

		t.add(WidgetBuilder.button("X", textButton -> {
			script.getTranslations().remove(translationFrame);
			if (script.getTranslations().isEmpty()) {
				script.setTranslations(null);
			}
			animationStudio.rebuildAnimation(scriptName);
			reload();
		}));

		return t;
	}

	private Table scalingWidget(AnimationScript.ScalingFrame scalingFrame, AnimationScript script, String scriptName) {
		Table t = new Table();
		t.add(new VisLabel("At Time")).padRight(10).padLeft(20);
		t.add(WidgetBuilder.floatSpinner(scalingFrame.getAtTime(), 0, Float.MAX_VALUE, d -> {
			scalingFrame.setAtTime(d);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(scalingFrame.getAtTime());
		}, DURATION_STEP, DURATION_SCALE)).width(DURATION_WIDGET_WIDTH).padRight(50);

		t.add(new VisLabel("X")).padRight(10);

		t.add(WidgetBuilder.floatSpinner(scalingFrame.getVector2().getX(), -Float.MAX_VALUE, Float.MAX_VALUE, i1 -> {
			scalingFrame.getVector2().x = i1;
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(scalingFrame.getAtTime());
		}, 0.05f, 3)).width(DURATION_WIDGET_WIDTH).padRight(50);

		t.add(new VisLabel("Y")).padRight(10);
		t.add(WidgetBuilder.floatSpinner(scalingFrame.getVector2().getY(), -Float.MAX_VALUE, Float.MAX_VALUE, i -> {
			scalingFrame.getVector2().y  = i;
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(scalingFrame.getAtTime());
		}, 0.05f, 3)).width(DURATION_WIDGET_WIDTH).padRight(50);

		t.add(WidgetBuilder.button("X", textButton -> {
			script.getScalings().remove(scalingFrame);
			if (script.getScalings().isEmpty()) {
				script.setScalings(null);
			}
			animationStudio.rebuildAnimation(scriptName);
			reload();
		}));

		return t;
	}


	private Table rotationWidget(AnimationScript.RotationFrame rotation, AnimationScript script, String scriptName) {
		Table t = new Table();
		t.add(new VisLabel("At Time")).padRight(10).padLeft(20);
		t.add(WidgetBuilder.floatSpinner(rotation.getAtTime(), 0, Float.MAX_VALUE, d -> {
			rotation.setAtTime(d);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(rotation.getAtTime());
		}, DURATION_STEP, DURATION_SCALE)).width(DURATION_WIDGET_WIDTH).padRight(50);
		t.add(new VisLabel("Roll")).padRight(10);
		t.add(WidgetBuilder.intSpinner(rotation.getRoll(), -360, 360, i -> {
			rotation.setRoll(i);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(rotation.getAtTime());
		})).padRight(50);

		t.add(WidgetBuilder.button("X", textButton -> {
			script.getRotations().remove(rotation);
			if (script.getRotations().isEmpty()) {
				script.setRotations(null);
			}
			animationStudio.rebuildAnimation(scriptName);
			reload();
		}));
		return t;
	}

	private Table soundWidget(AnimationScript.SoundCueFrame soundCue, AnimationScript script, String scriptName) {
		Table t = new Table();
		t.add(new VisLabel("At Time")).padRight(10).padLeft(20);
		t.add(WidgetBuilder.floatSpinner(soundCue.getAtTime(), 0, Float.MAX_VALUE, d -> {
			soundCue.setAtTime(d);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(soundCue.getAtTime());
		}, DURATION_STEP, DURATION_SCALE)).width(DURATION_WIDGET_WIDTH).padRight(50);
		t.add(new VisLabel("Sound Asset")).padRight(10);
		t.add(WidgetBuilder.select(soundCue.getSoundAssetName(), soundAssetNames, null, sa -> {
			soundCue.setSoundAssetName(sa);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(soundCue.getAtTime());
		}));

		t.add(WidgetBuilder.button("X", textButton -> {
			script.getSoundCues().remove(soundCue);
			if (script.getSoundCues().isEmpty()) {
				script.setSoundCues(null);
			}
			animationStudio.rebuildAnimation(scriptName);
			reload();
		}));
		return t;
	}

	private Table particleEffect(AnimationScript.ParticleEffectCueFrame particleEffectCueFrame, AnimationScript script, String scriptName) {
		Table t = new Table();
		t.add(new VisLabel("At Time")).padRight(10).padLeft(20);
		t.add(WidgetBuilder.floatSpinner(particleEffectCueFrame.getAtTime(), 0, Float.MAX_VALUE, d -> {
			particleEffectCueFrame.setAtTime(d);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(particleEffectCueFrame.getAtTime());
		}, DURATION_STEP, DURATION_SCALE)).width(DURATION_WIDGET_WIDTH).padRight(50);
		t.add(new VisLabel("Particle Effect")).padRight(10);
		t.add(WidgetBuilder.select(particleEffectCueFrame.getParticleEffectName(), particleEffectTypeNames, null, pe -> {
			particleEffectCueFrame.setParticleEffectName(pe);
			animationStudio.rebuildAnimation(scriptName);
			animationStudio.jumpToKeyFrameTime(particleEffectCueFrame.getAtTime());
		}));

		t.add(WidgetBuilder.button("X", textButton -> {
			script.getParticleEffectCues().remove(particleEffectCueFrame);
			if (script.getParticleEffectCues().isEmpty()) {
				script.setParticleEffectCues(null);
			}
			animationStudio.rebuildAnimation(scriptName);
			reload();
		}));
		return t;
	}

}
