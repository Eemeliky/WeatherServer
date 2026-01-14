package com.o3.server;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class SearchQuery {

	private final String command;	// The dynamically built SQL command.
	private final Map<String, String> params = new HashMap<>();		// Map of search parameters and their values.


	/**
	 * Constructs a SearchQuery object and builds the SQL command based on the provided search arguments.
	 *
	 * @param searchArgs A map of search arguments (key-value pairs) to filter the query.
	 */
	public SearchQuery(Map<String, String> searchArgs) {
		this.command = buildCommand(searchArgs);
	}


	/**
	 * Dynamically builds the SQL command based on the provided search arguments.
	 *
	 * @param searchArgs A map of search arguments (key-value pairs) to filter the query.
	 * @return The dynamically built SQL command as a string.
	 */
	private String buildCommand(Map<String, String> searchArgs) {
		// Start with the default SQL query.
		StringBuilder sb = new StringBuilder(getDefault());
		int added = 0;	// Tracks the number of conditions added to the WHERE clause.
		if (searchArgs != null && !searchArgs.isEmpty()) {
			sb.append(" WHERE ");
			// "nickname" search argument
			if (searchArgs.containsKey("nickname")) {
				sb.append("u.nickname = ? ");
				params.put("nickname", searchArgs.get("nickname"));
			}
			// "identification" search argument
			if (searchArgs.containsKey("identification")) {
				if (added != params.size()) {
					sb.append("AND ");
					added++;
				}
				sb.append("r.identifier = ? ");
				params.put("identification", searchArgs.get("identification"));
			}
			// "before" search argument
			if (searchArgs.containsKey("before")) {
				if (added != params.size()) {
					sb.append("AND ");
					added++;
				}
				sb.append("r.time_received < ? ");
				params.put("before", searchArgs.get("before"));
			}
			// "after" search argument
			if (searchArgs.containsKey("after")) {
				if (added != params.size()) {
					sb.append("AND ");
				}
				sb.append("r.time_received > ? ");
				params.put("after", searchArgs.get("after"));
			}
		}
		return sb.toString().trim();
	}

	public String getCommand() {
		return command;
	}

	/**
	 * Sets the parameters for the prepared statement based on the search arguments.
	 *
	 * @param ps The prepared statement to set the parameters for.
	 */
	public void setParams(PreparedStatement ps) throws SQLException {
		int index = 1;
		long time;
		if (params.containsKey("nickname")) {
			ps.setString(index, params.get("nickname"));
			index++;
		}
		if (params.containsKey("identification")) {
			ps.setString(index, params.get("identification"));
			index++;
		}
		if (params.containsKey("before")) {
			time = parseStringTime(params.get("before"));
			ps.setLong(index, time);
			index++;
		}
		if (params.containsKey("after")) {
			time = parseStringTime(params.get("after"));
			ps.setLong(index, time);
		}
	}

	/**
	 * Parses a string representation of a date-time into a timestamp (milliseconds since epoch).
	 *
	 * @param time The string representation of the date-time in 'yyyy-MM-dd'T'HH:mm:ss.SSSXX' format.
	 * @return The timestamp in milliseconds since epoch.
	 */
	private long parseStringTime(String time) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
		OffsetDateTime dateTime = OffsetDateTime.parse(time, formatter);
		return dateTime.toInstant().toEpochMilli();
	}

	/**
	 * Returns the default SQL query for retrieving observation records.
	 * This query includes joins with users, observatories, and weather data tables.
	 *
	 * @return The default SQL query as a string.
	 */
	private String getDefault() {
		return "SELECT " +
			"r.id, r.identifier, r.description, r.payload, r.right_ascension, r.declination, r.time_received, " +
			"r.update_reason, r.modified, " +
			"u.nickname AS owner, o.name AS name, o.latitude AS latitude, o.longitude AS longitude, " +
			"w.temperature AS temperature, w.pressure AS pressure, w.humidity AS humidity, " +
			"w.cloud_cover AS clouds, w.light_volume AS light " +
			"FROM records r " +
			"JOIN users u ON r.owner_id = u.id " +
			"LEFT JOIN observatories o ON r.observatory_id = o.id " +
			"LEFT JOIN weather w ON o.weather_id = w.id";
	}

}
