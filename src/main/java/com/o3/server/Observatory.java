package com.o3.server;

import org.json.JSONObject;

public record Observatory(String name, String latitude, String longitude) {

	public JSONObject getJSONObject() {
		return new JSONObject()
			.put("observatoryName", name)
			.put("latitude", latitude)
			.put("longitude", longitude);
	}
}
