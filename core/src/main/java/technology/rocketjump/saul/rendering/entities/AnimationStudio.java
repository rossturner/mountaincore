package technology.rocketjump.saul.rendering.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.model.AnimationScript;
import technology.rocketjump.saul.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.components.AnimationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.jobs.model.JobTarget;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class AnimationStudio implements Disposable, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final JobTypeDictionary jobTypeDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final Map<Key, NotifyingAnimationController> animationControllersForEntities = new HashMap<>(); //todo: not convinced of this key type
	private final Map<SpriteDescriptor, Model> modelCache = new HashMap<>();

	@Inject
	public AnimationStudio(MessageDispatcher messageDispatcher, CraftingTypeDictionary craftingTypeDictionary,
	                       JobTypeDictionary jobTypeDictionary, SoundAssetDictionary soundAssetDictionary,
	                       ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
	}

	public Set<String> getAvailableAnimationNames() {
		Stream<String> craftingTypeStream = craftingTypeDictionary.getAll()
				.stream()
				.map(CraftingType::getWorkOnJobAnimation);
		Stream<String> jobTypeStream = jobTypeDictionary.getAll()
				.stream()
				.map(JobType::getWorkOnJobAnimation);

		return Stream.concat(craftingTypeStream, jobTypeStream).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	/**
	 * Mutates affine transform based on available animation for given sprite descriptor
	 *
	 * @param affine
	 * @param spriteDescriptor
	 * @param gameContext
	 */
	public void animate(Affine2 affine, SpriteDescriptor spriteDescriptor, EntityPartRenderStep renderStep, GameContext gameContext, Vector2 spriteWorldSize, LocationComponent locationComponent) {
		Entity entity = renderStep.getEntity();
		AnimationComponent animationComponent = entity.getComponent(AnimationComponent.class);
		if (animationComponent != null && !spriteDescriptor.getEffectiveAnimationScripts().isEmpty()) {
			String currentAnimation = animationComponent.getCurrentAnimation();
			NotifyingAnimationController controller = getAnimationController(new Key(entity, spriteDescriptor));
			ModelInstance modelInstance = controller.target;
			if (currentAnimation == null) {
				controller.setAnimation(null);
			} else if (controller.current == null && modelInstance.getAnimation(currentAnimation) != null) {
				controller.setAnimation(currentAnimation, -1); //loop forever
			}

			Node node = modelInstance.nodes.get(0);
			Node child = modelInstance.nodes.get(1);
			child.translation.set(-spriteWorldSize.x/2, -spriteWorldSize.y/2, 0);
			child.translation.add(renderStep.getOffsetFromEntity().x, renderStep.getOffsetFromEntity().y, 0);

			if (gameContext == null || !gameContext.getGameClock().isPaused()) {
				controller.update(Gdx.graphics.getDeltaTime());
			}

			Affine2 newAffine = new Affine2();
			newAffine.idt();
			newAffine.translate(locationComponent.getWorldOrParentPosition());

			//feels bit sketchy but this assumes one node tree, mapping to the sprite descriptor
			modelInstance.transform.set(newAffine); //set the general location of whole model instance
			modelInstance.transform.mul(node.localTransform).mul(child.globalTransform); //transform the model with the single animated node
			affine.set(modelInstance.transform);
		}
	}

	/**
	 * Inspired by Unreal's Animation Notifications
	 */
	private static class NotifyingAnimationController extends AnimationController {

		private final SpriteDescriptor spriteDescriptor;
		private final MessageDispatcher messageDispatcher;
		private final Entity entity;
		private long frameId = 0l;

		/**
		 * Construct a new AnimationController.
		 *
		 * @param target               The {@link ModelInstance} on which the animations will be performed.
		 * @param messageDispatcher
		 */
		public NotifyingAnimationController(ModelInstance target, Key key, MessageDispatcher messageDispatcher) {
			super(target);
			this.entity = key.entity;
			this.spriteDescriptor = key.spriteDescriptor;
			this.messageDispatcher = messageDispatcher;
		}

		@Override
		public void update(float delta) {
			if (paused) {
				return;
			}

			long currentFrameId = Gdx.graphics.getFrameId();
			if (currentFrameId == frameId) {
				return;
			} else {
				frameId = currentFrameId;
			}

			super.update(delta);
			if (current != null) {
				String id = current.animation.id;
				float time = current.offset + current.time;
				float previousTime = Math.max(time - delta, 0); //todo: is this safe? what about if animation has changed?
				AnimationScript script = spriteDescriptor.getEffectiveAnimationScripts().get(id);

				if (script.getSoundCues() != null) {
					List<AnimationScript.SoundCueFrame> toPlay = script.getSoundCues().stream()
							.filter(cue -> cue.getAtTime() > previousTime && cue.getAtTime() < time).toList();

					for (AnimationScript.SoundCueFrame soundCueFrame : toPlay) {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND,
								new RequestSoundMessage(soundCueFrame.getSoundAsset(), entity.getId(), entity.getLocationComponent().getWorldOrParentPosition(), null));
					}
				}

				if (script.getParticleEffectCues() != null) {
					List<AnimationScript.ParticleEffectCueFrame> toPlay = script.getParticleEffectCues().stream()
							.filter(cue -> cue.getAtTime() > previousTime && cue.getAtTime() < time).toList();

					for (AnimationScript.ParticleEffectCueFrame particleEffectCueFrame : toPlay) {
						messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST,
								new ParticleRequestMessage(
										particleEffectCueFrame.getParticleEffectType(),
										Optional.of(entity),
										Optional.of(new JobTarget.AnimationTarget(entity)),
										instance -> {})
						);

					}
				}
			}
		}
	}

	private NotifyingAnimationController getAnimationController(Key key) {
		NotifyingAnimationController controller = animationControllersForEntities.get(key);
		if (controller == null) {
			Model model = buildModel(key.spriteDescriptor);
			ModelInstance modelInstance = new ModelInstance(model); //This is per entity sprite descriptor
			controller = new NotifyingAnimationController(modelInstance, key, messageDispatcher);
			animationControllersForEntities.put(key, controller);
		}
		return controller;
	}

	//For asset editing
	public void pauseAnimations() {
		animationControllersForEntities.values().forEach(c -> c.paused = true);
	}

	public void resumeAnimations() {
		animationControllersForEntities.values().forEach(c -> c.paused = false);
	}

	public float jumpToStartForAnimations() {
		return jumpToKeyFrameTime(0f);
	}

	public float nextKeyFrame() {
		float nextKeyFrameTime = animationControllersForEntities.values()
				.stream()
				.flatMap(c -> {
					if (c.current != null) {
						return getKeyFrameTimes(c.current.animation).stream().filter(t -> t > c.current.time);
					} else {
						return Stream.empty();
					}
				})
				.sorted()
				.findFirst().orElse(0f);

		return jumpToKeyFrameTime(nextKeyFrameTime);
	}

	//TODO: lots of duplication
	public float previousKeyFrame() {
		float lastTime = animationControllersForEntities.values().stream().filter(c -> c.current != null).flatMap(c -> getKeyFrameTimes(c.current.animation).stream()).max(Float::compareTo).orElse(0f);

		float nextKeyFrameTime = animationControllersForEntities.values()
				.stream()
				.flatMap(c -> {
					if (c.current != null) {
						return getKeyFrameTimes(c.current.animation).stream().filter(t -> t < c.current.time); //only line diff, bah
					} else {
						return Stream.empty();
					}
				})
				.max(Float::compareTo).orElse(lastTime);

		return jumpToKeyFrameTime(nextKeyFrameTime);
	}

	public float jumpToEndForAnimations() {
		float nextKeyFrameTime = animationControllersForEntities.values()
				.stream()
				.flatMap(c -> {
					if (c.current != null) {
						return getKeyFrameTimes(c.current.animation).stream();
					} else {
						return Stream.empty();
					}
				})
				.max(Comparator.comparingDouble(Float::doubleValue)).orElse(0f);

		return jumpToKeyFrameTime(nextKeyFrameTime);
	}

	public float jumpToKeyFrameTime(float keyFrameTime) {
		resumeAnimations();
		animationControllersForEntities.values().forEach(c -> {
			if (c.current != null) {
				c.current.time = keyFrameTime;
				c.update(0);
			}
		});
		animationControllersForEntities.values().forEach(c -> {
			if (c.current != null) {
				c.current.time = keyFrameTime;
			}
		});
		pauseAnimations();
		return keyFrameTime;
	}

	public void rebuildAnimation(String animationToPlay) {
		HashMap<Key, AnimationController> clonedCurrentAnimationControllers = new HashMap<>(animationControllersForEntities);
		dispose();//nuke option - ensures inherited animations are copied
		for (Map.Entry<Key, AnimationController> entry : clonedCurrentAnimationControllers.entrySet()) {
			AnimationController animationController = getAnimationController(entry.getKey());
			if (animationController.target.getAnimation(animationToPlay) != null) {
				Node clonedChild = entry.getValue().target.nodes.get(1);
				Node targetChildNode = animationController.target.nodes.get(1);
				targetChildNode.translation.set(clonedChild.translation);

				animationController.setAnimation(animationToPlay, -1);
			}
		}
	}



	@Override
	public void dispose() {
		modelCache.values().forEach(Model::dispose);
		modelCache.clear();
		animationControllersForEntities.clear();
	}


	private TreeSet<Float> getKeyFrameTimes(Animation animation) {
		TreeSet<Float> keyFrameTimes = new TreeSet<>();
		for (NodeAnimation nodeAnimation : animation.nodeAnimations) {
			if (nodeAnimation.translation != null) {
				for (NodeKeyframe<?> keyframe : nodeAnimation.translation) {
					keyFrameTimes.add(keyframe.keytime);
				}
			}
			if (nodeAnimation.rotation != null) {
				for (NodeKeyframe<?> keyframe : nodeAnimation.rotation) {
					keyFrameTimes.add(keyframe.keytime);
				}
			}
			if (nodeAnimation.scaling != null) {
				for (NodeKeyframe<?> keyframe : nodeAnimation.scaling) {
					keyFrameTimes.add(keyframe.keytime);
				}
			}
		}
		return keyFrameTimes;
	}

	private Model buildModel(SpriteDescriptor spriteDescriptor) {
		Model model = modelCache.get(spriteDescriptor);
		if (model == null) {
			ModelBuilder modelBuilder = new ModelBuilder();
			modelBuilder.begin();
			Node node = modelBuilder.node();
			node.id = "animation-node-" + spriteDescriptor.hashCode(); //TODO: whats the right thing here

			Node child = modelBuilder.node();
			child.id = "animation-node-child-" + spriteDescriptor.hashCode();
			node.addChild(child);

			Array<Animation> animations = parseAnimations(spriteDescriptor, node);

			model = modelBuilder.end();
			model.animations.addAll(animations);
			modelCache.put(spriteDescriptor, model);
		}

		return model;
	}

	private Array<Animation> parseAnimations(SpriteDescriptor spriteDescriptor, Node... nodes) {
		Array<Animation> animations = new Array<>();

		for (Map.Entry<String, AnimationScript> entry : spriteDescriptor.getEffectiveAnimationScripts().entrySet()) {
			AnimationScript script = entry.getValue();

			Animation animation = new Animation();
			animation.id = entry.getKey();
			animation.duration = script.getDuration(); //seconds

			for (Node node : nodes) {

				NodeAnimation nodeAnimation = new NodeAnimation();
				nodeAnimation.node = node;

				if (script.getRotations() != null && !script.getRotations().isEmpty()) {
					nodeAnimation.rotation = new Array<>();
					script.getRotations().sort(Comparator.comparing(AnimationScript.Frame::getAtTime));
					for (AnimationScript.RotationFrame frame : script.getRotations()) {
						Quaternion quaternion = new Quaternion();
						quaternion.setEulerAngles(0, 0, frame.getRoll());

						nodeAnimation.rotation.add(new NodeKeyframe<>(frame.getAtTime(), quaternion));
					}
				}

				if (script.getTranslations() != null && !script.getTranslations().isEmpty()) {
					nodeAnimation.translation = new Array<>();
					script.getTranslations().sort(Comparator.comparing(AnimationScript.Frame::getAtTime));
					for (AnimationScript.TranslationFrame frame : script.getTranslations()) {
						Vector3 vector3 = new Vector3(frame.getVector2().getX(), frame.getVector2().getY(), 0);

						nodeAnimation.translation.add(new NodeKeyframe<>(frame.getAtTime(), vector3));
					}
				}

				if (script.getScalings() != null && !script.getScalings().isEmpty()) {
					nodeAnimation.scaling = new Array<>();
					script.getScalings().sort(Comparator.comparing(AnimationScript.Frame::getAtTime));
					for (AnimationScript.ScalingFrame frame : script.getScalings()) {
						Vector3 vector3 = new Vector3(frame.getVector2().getX(), frame.getVector2().getY(), 0f);
						nodeAnimation.scaling.add(new NodeKeyframe<>(frame.getAtTime(), vector3));
					}
				}


				if (script.getSoundCues() != null) {
					for (AnimationScript.SoundCueFrame soundCue : script.getSoundCues()) {
						SoundAsset soundAsset = soundAssetDictionary.getByName(soundCue.getSoundAssetName());
						if (soundAsset != null) {
							soundCue.setSoundAsset(soundAsset);
						}
					}
				}

				if (script.getParticleEffectCues() != null) {
					for (AnimationScript.ParticleEffectCueFrame particleEffectCue : script.getParticleEffectCues()) {
						ParticleEffectType particleEffectType = particleEffectTypeDictionary.getByName(particleEffectCue.getParticleEffectName());
						if (particleEffectType != null) {
							particleEffectCue.setParticleEffectType(particleEffectType);
						}
					}
				}

				animation.nodeAnimations.add(nodeAnimation);
			}


			animations.add(animation);
		}
		return animations;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		dispose();
	}

	@Override
	public void clearContextRelatedState() {
		dispose();
	}


	private static class Key {
		private final Entity entity;
		private final SpriteDescriptor spriteDescriptor;

		private Key(Entity entity, SpriteDescriptor spriteDescriptor) {
			this.entity = entity;
			this.spriteDescriptor = spriteDescriptor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Key key = (Key) o;
			return entity.equals(((Key) o).entity) && Objects.equals(spriteDescriptor, key.spriteDescriptor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(entity.hashCode(), spriteDescriptor);
		}
	}

}
