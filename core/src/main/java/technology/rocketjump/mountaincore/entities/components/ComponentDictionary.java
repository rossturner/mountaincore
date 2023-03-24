package technology.rocketjump.mountaincore.entities.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.misc.ReflectionsService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class ComponentDictionary {

	private final Map<String, Class<? extends EntityComponent>> simpleNameMap = new HashMap<>();

	@Inject
	public ComponentDictionary(ReflectionsService reflectionsService) {
		Set<Class<? extends EntityComponent>> componentClasses = reflectionsService.getSubTypesOf(EntityComponent.class);

		for (Class<? extends EntityComponent> componentClass : componentClasses) {
			if (simpleNameMap.containsKey(componentClass.getSimpleName())) {
				throw new RuntimeException("Duplicate EntityComponent class name: " + componentClass.getSimpleName());
			} else {
				simpleNameMap.put(componentClass.getSimpleName(), componentClass);
			}
		}
	}

	public Class<? extends EntityComponent> getByName(String className) {
		return simpleNameMap.get(className);
	}
}
