package technology.rocketjump.saul.entities.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.saul.entities.model.physical.creature.status.StatusEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class StatusEffectDictionary {

	private Map<String, Class<? extends StatusEffect>> simpleNameMap = new HashMap<>();

	@Inject
	public StatusEffectDictionary() {
		Reflections reflections = new Reflections("technology.rocketjump.saul", new SubTypesScanner());
		Set<Class<? extends StatusEffect>> effectClasses = reflections.getSubTypesOf(StatusEffect.class);

		for (Class<? extends StatusEffect> effectClass : effectClasses) {
			if (simpleNameMap.containsKey(effectClass.getSimpleName())) {
				throw new RuntimeException("Duplicate " + StatusEffect.class.getSimpleName() + " class name: " + effectClass.getSimpleName());
			} else {
				simpleNameMap.put(effectClass.getSimpleName(), effectClass);
			}
		}
	}

	public Class<? extends StatusEffect> getByName(String className) {
		return simpleNameMap.get(className);
	}
}
