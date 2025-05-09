package technology.rocketjump.mountaincore.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class StockpileGroupDictionary {

	private final Map<String, StockpileGroup> byName = new HashMap<>();
	private final List<StockpileGroup> all = new ArrayList<>();

	@Inject
	public StockpileGroupDictionary() throws IOException {
		this(new File("assets/definitions/stockpileGroups.json"));
	}

	private StockpileGroupDictionary(File jsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<StockpileGroup> stockpileGroups = objectMapper.readValue(FileUtils.readFileToString(jsonFile),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, StockpileGroup.class));

		for (StockpileGroup stockpileGroup : stockpileGroups) {
			byName.put(stockpileGroup.getName(), stockpileGroup);
			all.add(stockpileGroup);
		}
	}

	public StockpileGroup getByName(String stockpileGroupName) {
		return byName.get(stockpileGroupName);
	}

	public List<StockpileGroup> getAll() {
		return all;
	}

	public List<StockpileGroup> getAll(I18nTranslator i18nTranslator) {
		ArrayList<StockpileGroup> stockpileGroups = new ArrayList<>(getAll());
		stockpileGroups.sort(Comparator.comparing(stockpileGroup -> i18nTranslator.translate(stockpileGroup.getI18nKey())));
		return stockpileGroups;
	}
}
