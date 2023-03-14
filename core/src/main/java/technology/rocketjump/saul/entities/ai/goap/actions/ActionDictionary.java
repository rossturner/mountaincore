package technology.rocketjump.saul.entities.ai.goap.actions;

import com.google.inject.Singleton;
import technology.rocketjump.saul.misc.ReflectionsService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class ActionDictionary {

	private Map<String, Class<? extends Action>> byName = new HashMap<>();

	@Inject
	public ActionDictionary(ReflectionsService reflectionsService) {
		Set<Class<? extends Action>> actionClasses = reflectionsService.getSubTypesOf(Action.class);
		for (Class<? extends Action> actionClass : actionClasses) {
			String className = actionClass.getSimpleName().replace("Action", "");
			byName.put(className, actionClass);
		}
	}

	public Class<? extends Action> getByName(String name) {
		Class<? extends Action> actionClass = byName.get(name);
		if (actionClass == null) {
			throw new RuntimeException("Could not find entity Action with name: " + name);
		}
		return actionClass;
	}

}
