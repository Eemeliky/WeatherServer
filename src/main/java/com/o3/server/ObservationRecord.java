package com.o3.server;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ObservationRecord {

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
	private final Integer index;
	private final String identifier;
	private final String description;
	private final String payload;
	private final String rightAscension;
	private final String declination;
	private final String owner;
	private final ZonedDateTime timeReceived;
	private final Observatory observatory;
	private final WeatherData weatherData;
	private final String updateReason;
	private final ZonedDateTime updateTime;

	/**
	 * Constructor for creating an observation record with current time as time stamp. Uses ISO 8601 date format.
	 **/
	public ObservationRecord(String Identifier, String Description, String Payload,
							 String RightAscension, String Declination,
							 String owner, Observatory observatory, WeatherData wData,
							 String updateReason) {
		ZonedDateTime currentTime = Util.getCurrentTime();
		this.index = null;
		this.identifier = Identifier;
		this.description = Description;
		this.payload = Payload;
		this.rightAscension = RightAscension;
		this.declination = Declination;
		this.owner = owner;
		this.timeReceived = currentTime;
		this.observatory = observatory;
		this.weatherData = wData;
		this.updateReason = Objects.requireNonNullElse(updateReason, "N/A");
		this.updateTime = currentTime;
	}

	/**
	 * Constructor for creating an observation record with already existing time stamp.
	 **/
	public ObservationRecord(Integer index, String identifier, String description, String payload,
							 String rightAscension, String declination, String owner,
							 long timeReceived, Observatory observatory, WeatherData wData,
							 String updateReason, long updateTime) {
		this.index = index;
		this.identifier = identifier;
		this.description = description;
		this.payload = payload;
		this.rightAscension = rightAscension;
		this.declination = declination;
		this.owner = owner;
		this.timeReceived = Util.timeLongToZoned(timeReceived);
		this.observatory = observatory;
		this.weatherData = wData;
		this.updateReason = updateReason;
		this.updateTime = Util.timeLongToZoned(updateTime);
	}

	public JSONObject getJSONObject() {
		JSONObject jsonObject = new JSONObject()
			.put("id", index)
			.put("recordIdentifier", identifier)
			.put("recordDescription", description)
			.put("recordPayload", payload)
			.put("recordRightAscension", rightAscension)
			.put("recordDeclination", declination)
			.put("recordOwner", owner)
			.put("recordTimeReceived", timeReceived.format(formatter));
		if (!timeReceived.equals(updateTime)) {
			jsonObject
				.put("updateReason", updateReason)
				.put("modified", updateTime.format(formatter));
		}
		if (this.hasObservatory()) {
			JSONArray tmp = new JSONArray();
			tmp.put(observatory.getJSONObject());
			jsonObject.put("observatory", tmp);
		}
		if (this.hasWeatherData()) {
			JSONArray tmp = new JSONArray();
			tmp.put(weatherData.getJSONObject());
			jsonObject.put("observatoryWeather", tmp);
		}
		return jsonObject;
	}

	public boolean hasWeatherData() {
		return weatherData != null;
	}

	public boolean hasObservatory() {
		return observatory != null;
	}

	public WeatherData getWeatherData() {
		return weatherData;
	}

	public Observatory getObservatory() {
		return observatory;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getDescription() {
		return description;
	}

	public String getPayload() {
		return payload;
	}

	public String getRightAscension() {
		return rightAscension;
	}

	public String getDeclination() {
		return declination;
	}

	public String getOwner() {
		return owner;
	}

	public String getUpdateReason() {
		return updateReason;
	}

	public ZonedDateTime getTimeReceived() {
		return timeReceived;
	}

	public ZonedDateTime getUpdateTime() {
		return updateTime;
	}
}
