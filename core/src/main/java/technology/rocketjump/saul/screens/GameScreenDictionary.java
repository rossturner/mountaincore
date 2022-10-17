package technology.rocketjump.saul.screens;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Singleton
public class GameScreenDictionary {

	private final Map<String, GameScreen> byName = new TreeMap<>();
	private final Map<String, ManagementScreen> managementScreensByName = new TreeMap<>();

	@Inject
	public GameScreenDictionary(Injector injector) {
		Reflections reflections = new Reflections("technology.rocketjump.saul.screens");
		Set<Class<? extends GameScreen>> screenTypes = reflections.getSubTypesOf(GameScreen.class);
		for (Class<? extends GameScreen> screenType : screenTypes) {
			if (!screenType.isInterface() && !Modifier.isAbstract(screenType.getModifiers())) {
				GameScreen instance = injector.getInstance(screenType);
				byName.put(instance.getName(), instance);
				if (instance instanceof ManagementScreen) {
					managementScreensByName.put(instance.getName(), (ManagementScreen) instance);
				}
			}
		}
	}

	public GameScreen getByName(String name) {
		return byName.get(name);
	}

	public List<ManagementScreen> getManagementScreensOrderedForUI() {
		return List.of(
//				managementScreensByName.get(MilitaryManagementScreen.NAME),
//				managementScreensByName.get(TradingManagementScreen.NAME),
				managementScreensByName.get(CraftingManagementScreen.NAME),
				managementScreensByName.get(SettlerManagementScreen.NAME),
				managementScreensByName.get(ResourceManagementScreen.NAME)
		);
	}
}
