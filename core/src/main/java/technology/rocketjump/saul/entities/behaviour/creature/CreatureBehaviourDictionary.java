package technology.rocketjump.saul.entities.behaviour.creature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.misc.ReflectionsService;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class CreatureBehaviourDictionary {

	private final Map<String, Class<? extends BehaviourComponent>> byName = new HashMap<>();

	@Inject
	public CreatureBehaviourDictionary(ReflectionsService reflectionsService) {
		Set<Class<? extends BehaviourComponent>> behaviourClasses = reflectionsService.getSubTypesOf(BehaviourComponent.class);
		for (Class<? extends BehaviourComponent> behaviourClass : behaviourClasses) {
			if (!Modifier.isAbstract(behaviourClass.getModifiers())) {
				byName.put(toBehaviourName(behaviourClass), behaviourClass);
			}
		}
	}

	public static String toBehaviourName(Class<? extends BehaviourComponent> behaviourClass) {
		return behaviourClass.getSimpleName().substring(0, behaviourClass.getSimpleName().indexOf("Behaviour"));
	}

	public Class<? extends BehaviourComponent> getByName(String name) {
		return byName.get(name);
	}

	public Collection<String> getAllNames() {
		return byName.keySet();
	}
}
