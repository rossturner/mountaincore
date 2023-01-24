package technology.rocketjump.saul.rendering.entities;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.model.AnimationScript;
import technology.rocketjump.saul.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.saul.entities.components.AnimationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.jobs.model.JobType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class AnimationStudio implements Disposable, GameContextAware {

	private final CraftingTypeDictionary craftingTypeDictionary;
	private final JobTypeDictionary jobTypeDictionary;
	private final Map<Key, AnimationController> animationControllersForEntities = new HashMap<>(); //todo: not convinced of this key type
	private final Map<SpriteDescriptor, Model> modelCache = new HashMap<>();

	@Inject
	public AnimationStudio(CraftingTypeDictionary craftingTypeDictionary, JobTypeDictionary jobTypeDictionary) {
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
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
	 * @param entity
	 * @param gameContext
	 */
	public void animate(Affine2 affine, SpriteDescriptor spriteDescriptor, Entity entity, GameContext gameContext) {
		AnimationComponent animationComponent = entity.getComponent(AnimationComponent.class);
		if (animationComponent != null && !spriteDescriptor.getAnimationScripts().isEmpty()) {
			String currentAnimation = animationComponent.getCurrentAnimation();
			AnimationController controller = getAnimationController(new Key(entity.getId(), spriteDescriptor));
			ModelInstance modelInstance = controller.target;
			if (currentAnimation == null) {
				controller.setAnimation(null);
			} else if (controller.current == null && modelInstance.getAnimation(currentAnimation) != null) {
				controller.setAnimation(currentAnimation, -1); //loop forever
			}

			if (gameContext == null || !gameContext.getGameClock().isPaused()) {
				controller.update(Gdx.graphics.getDeltaTime());
			}

			//feels bit sketchy but this assumes one node tree, mapping to the sprite descriptor
			modelInstance.transform.set(affine); //set the general location of whole model instance
			Node node = modelInstance.nodes.get(0);
			modelInstance.transform.mul(node.localTransform); //transform the model with the single animated node
			affine.set(modelInstance.transform);
		}
	}

	private AnimationController getAnimationController(Key key) {
		AnimationController controller = animationControllersForEntities.get(key);
		if (controller == null) {
			Model model = buildModel(key.spriteDescriptor);
			ModelInstance modelInstance = new ModelInstance(model); //This is per entity sprite descriptor
			controller = new AnimationController(modelInstance);
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
		float nextKeyFrameTime = animationControllersForEntities.values()
				.stream()
				.flatMap(c -> {
					if (c.current != null) {
						return getKeyFrameTimes(c.current.animation).stream().filter(t -> t < c.current.time); //only line diff, bah
					} else {
						return Stream.empty();
					}
				})
				.sorted()
				.findFirst().orElse(0f);

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
		pauseAnimations();
		return keyFrameTime;
	}

	public void rebuildAnimation(String animationToPlay) {
		HashMap<Key, AnimationController> clonedCurrentAnimationControllers = new HashMap<>(animationControllersForEntities);
		dispose();//nuke option - ensures inherited animations are copied
		for (Map.Entry<Key, AnimationController> entry : clonedCurrentAnimationControllers.entrySet()) {
			AnimationController animationController = getAnimationController(entry.getKey());
			if (animationController.target.getAnimation(animationToPlay) != null) {
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


	private Set<Float> getKeyFrameTimes(Animation animation) {
		Set<Float> keyFrameTimes = new TreeSet<>();
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
			model = modelBuilder.end();
			model.animations.addAll(parseAnimations(node, spriteDescriptor));
			modelCache.put(spriteDescriptor, model);
		}

		return model;
	}

	private Array<Animation> parseAnimations(Node node, SpriteDescriptor spriteDescriptor) {
		Array<Animation> animations = new Array<>();
		for (Map.Entry<String, AnimationScript> entry : spriteDescriptor.getAnimationScripts().entrySet()) {
			AnimationScript script = entry.getValue();

			Animation animation = new Animation();
			animation.id = entry.getKey();
			animation.duration = script.getDuration(); //seconds
			NodeAnimation nodeAnimation = new NodeAnimation();
			nodeAnimation.node = node;

			if (script.getRotations() != null) {
				nodeAnimation.rotation = new Array<>();
				for (AnimationScript.RotationFrame frame : script.getRotations()) {
					Quaternion quaternion = new Quaternion();
					quaternion.setEulerAngles(0, 0, frame.getRoll());

					nodeAnimation.rotation.add(new NodeKeyframe<>(frame.getAtTime(), quaternion));
				}
			}

			if (script.getTranslations() != null) {
				nodeAnimation.translation = new Array<>();
				for (AnimationScript.TranslationFrame frame : script.getTranslations()) {
					Vector3 vector3 = new Vector3(frame.getVector2().getX(), frame.getVector2().getY(), 0);

					nodeAnimation.translation.add(new NodeKeyframe<>(frame.getAtTime(), vector3));
				}
			}

			animation.nodeAnimations.add(nodeAnimation);

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
		private final long entityId;
		private final SpriteDescriptor spriteDescriptor;

		private Key(long entityId, SpriteDescriptor spriteDescriptor) {
			this.entityId = entityId;
			this.spriteDescriptor = spriteDescriptor;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Key key = (Key) o;
			return entityId == key.entityId && Objects.equals(spriteDescriptor, key.spriteDescriptor);
		}

		@Override
		public int hashCode() {
			return Objects.hash(entityId, spriteDescriptor);
		}
	}

}
