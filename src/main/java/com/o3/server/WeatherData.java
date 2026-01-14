package com.o3.server;

import org.json.JSONObject;

public record WeatherData(String temperature, String pressure, String humidity, String totalCloudCover,
						  String radiationGlobalAccumulation) {


	public JSONObject getJSONObject() {
		JSONObject weatherJson = new JSONObject().put("temperatureInKelvins", temperature);
		if (pressure != null) {
			weatherJson.put("atmospherePressure", pressure);
		}
		if (totalCloudCover != null) {
			weatherJson.put("cloudinessPercentage", totalCloudCover);
		}
		if (humidity != null) {
			weatherJson.put("airHumidityPercentage", humidity);
		}
		if (radiationGlobalAccumulation != null) {
			weatherJson.put("backgroundLightVolume", radiationGlobalAccumulation);
		}
		return weatherJson;
	}

}
