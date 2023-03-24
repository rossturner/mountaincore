package technology.rocketjump.mountaincore.military;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.mountaincore.military.model.formations.SquadFormation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class SquadFormationDictionary {

	private Map<String, SquadFormation> byName = new HashMap<>();

	@Inject
	public SquadFormationDictionary() {
		Reflections reflections = new Reflections(getClass().getPackageName(), new SubTypesScanner());
		Set<Class<? extends SquadFormation>> formationClasses = reflections.getSubTypesOf(SquadFormation.class);
		for (Class<? extends SquadFormation> formationClass : formationClasses) {
			if (!formationClass.isInterface() && !Modifier.isAbstract(formationClass.getModifiers())) {
				try {
					SquadFormation squadFormation = formationClass.getConstructor().newInstance();
					if (byName.containsKey(squadFormation.getFormationName())) {
						Logger.error("Duplicate formation with name " + squadFormation.getFormationName());
					} else {
						byName.put(squadFormation.getFormationName(), squadFormation);
					}
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					Logger.error(e, "Error with reflection of class " + formationClass.getSimpleName() + " in " + getClass().getSimpleName());
				}
			}
		}
	}

	public SquadFormation getByName(String name) {
		return byName.get(name);
	}

	public Collection<SquadFormation> getAll() {
		return byName.values();
	}

}
