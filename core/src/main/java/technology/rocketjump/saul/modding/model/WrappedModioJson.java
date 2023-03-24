package technology.rocketjump.saul.modding.model;

import com.alibaba.fastjson.JSONObject;

public class WrappedModioJson {

	private final JSONObject jsonData;

	public WrappedModioJson(JSONObject jsonData) {
		this.jsonData = jsonData;
	}

	public Long getModioId() {
		return jsonData.getLongValue("id");
	}

	public String getModfileHash() {
		return jsonData.getJSONObject("modfile").getJSONObject("filehash").getString("md5");
	}

	public String getFilename() {
		return jsonData.getJSONObject("modfile").getString("filename");
	}

	public String getBinaryUrl() {
		return jsonData.getJSONObject("modfile").getJSONObject("download").getString("binary_url");
	}
}
