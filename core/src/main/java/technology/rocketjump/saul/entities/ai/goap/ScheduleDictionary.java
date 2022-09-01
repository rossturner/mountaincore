package technology.rocketjump.saul.entities.ai.goap;

import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.military.model.MilitaryShift;

import java.io.IOException;
import java.util.*;

@Singleton
public class ScheduleDictionary {

	private static final String MILITARY_DAY_SHIFT_SCHEDULE_NAME = "Military day shift";
	private static final String MILITARY_NIGHT_SHIFT_SCHEDULE_NAME = "Military night shift";

	private Map<String, Schedule> byName = new HashMap<>();
	private static Schedule militaryDayShiftSchedule;
	private static Schedule militaryNightShiftSchedule;
	public static final Schedule NULL_SCHEDULE = new Schedule("NULL", Map.of());

	public static Schedule getScheduleForSquadShift(MilitaryShift shift) {
		return switch (shift) {
			case DAYTIME -> militaryDayShiftSchedule;
			case NIGHTTIME -> militaryNightShiftSchedule;
		};
	}

	@Inject
	public ScheduleDictionary() throws IOException {

		FileHandle schedulesJsonFile = new FileHandle("assets/ai/schedules.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<Schedule> schedules = objectMapper.readValue(schedulesJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Schedule.class));

		for (Schedule schedule : schedules) {
			byName.put(schedule.getName(), schedule);
		}

		militaryDayShiftSchedule = byName.get(MILITARY_DAY_SHIFT_SCHEDULE_NAME);
		if (militaryDayShiftSchedule == null) {
			throw new RuntimeException("Could not find schedule with name: " + MILITARY_DAY_SHIFT_SCHEDULE_NAME);
		}
		militaryNightShiftSchedule = byName.get(MILITARY_NIGHT_SHIFT_SCHEDULE_NAME);
		if (militaryNightShiftSchedule == null) {
			throw new RuntimeException("Could not find schedule with name: " + MILITARY_NIGHT_SHIFT_SCHEDULE_NAME);
		}
	}

	public Schedule getByName(String name) {
		return byName.get(name);
	}

	public Collection<String> getAllNames() {
		return byName.keySet();
	}
}
