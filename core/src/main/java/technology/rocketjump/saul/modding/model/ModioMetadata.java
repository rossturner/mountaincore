package technology.rocketjump.saul.modding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModioMetadata {

	private long modioId;
	private String downloadChecksum;

	public long getModioId() {
		return modioId;
	}

	public void setModioId(long modioId) {
		this.modioId = modioId;
	}

	public String getDownloadChecksum() {
		return downloadChecksum;
	}

	public void setDownloadChecksum(String downloadChecksum) {
		this.downloadChecksum = downloadChecksum;
	}
}
