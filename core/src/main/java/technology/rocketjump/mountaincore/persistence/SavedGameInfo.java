package technology.rocketjump.mountaincore.persistence;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SavedGameInfo {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

	public final GameClock gameClock;
	public final File file;
	public final String settlementName;
	public final String version;
	public final Instant lastModifiedTime;
	public final String formattedFileModifiedTime;
	public final String formattedGameTime;
	public final boolean peacefulMode;
	private boolean isCompressed = true;

	public SavedGameInfo(File saveFile, JSONObject headerJson, I18nTranslator i18nTranslator) throws InvalidSaveException, IOException {
		this.gameClock = new GameClock();
		this.gameClock.readFrom(headerJson.getJSONObject("clock"), null, null);

		this.file = saveFile;
		this.settlementName = headerJson.getString("name");
		this.version = headerJson.getString("version");
		this.peacefulMode = headerJson.getBooleanValue("peacefulMode");
		this.lastModifiedTime = Files.getLastModifiedTime(file.toPath()).toInstant();
		this.formattedFileModifiedTime = DATE_TIME_FORMATTER.format(lastModifiedTime);
		this.formattedGameTime = i18nTranslator.getDateTimeString(gameClock).toString();
	}

	public boolean isCompressed() {
		return isCompressed;
	}

	public void setCompressed(boolean compressed) {
		isCompressed = compressed;
	}
}
