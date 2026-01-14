package com.o3.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MessageDataBase {

	private static volatile MessageDataBase instance;
	private final HikariDataSource dataSource;
	private final SecureRandom secureRandom;


	/**
	 * Private constructor to initialize the database connection and create the database file if it doesn't exist.
	 *
	 * @param dbName The name of the database file.
	 */
	private MessageDataBase(String dbName) throws SQLException, IOException {
		secureRandom = new SecureRandom();

		// Create database file if missing
		File database = new File(dbName);
		if (!database.exists() || database.isDirectory()) {
			if (!database.createNewFile()) {
				throw new IOException("Cannot create database file!");
			}
		}

		// Set up config
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:sqlite:" + dbName);
		config.setMaximumPoolSize(10);
		config.setMinimumIdle(5);
		config.setIdleTimeout(300000);  // 5 minutes
		config.setMaxLifetime(600000);  // 10 minutes

		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		dataSource = new HikariDataSource(config);

		initializeDatabase();
	}


	/**
	 * Returns the singleton instance of the MessageDataBase class.
	 *
	 * @param dbName The name of the database file.
	 */
	public static MessageDataBase getInstance(String dbName) throws SQLException, IOException {
		if (instance == null) {
			synchronized (MessageDataBase.class) {
				if (instance == null) {
					instance = new MessageDataBase(dbName);
				}
			}
		}
		return instance;
	}

	/**
	 * Initializes the database by executing SQL commands from an initialization file.
	 */
	private void initializeDatabase() throws SQLException, IOException {
		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement();
			 BufferedReader br = new BufferedReader(new FileReader("src/main/resources/initialize.sql"))) {

			StringBuilder stringSQL = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				stringSQL.append(line).append("\n");
			}

			String[] statementsSQL = stringSQL.toString().split(";");

			for (String command : statementsSQL) {
				if (!command.trim().isEmpty()) {
					stmt.execute(command.trim() + ";");
				}
			}
		}
	}

	/**
	 * Encrypts a password using a secure hashing algorithm.
	 *
	 * @param password The plain text password to encrypt.
	 * @return The encrypted password.
	 */
	private String encrypt(String password) {
		byte[] bytes = new byte[13];
		secureRandom.nextBytes(bytes);
		String saltBytes = new String(Base64.getEncoder().encode(bytes));
		String salt = "$6$" + saltBytes;
		return Crypt.crypt(password, salt.replace("+", "a"));
	}

	/**
	 * Inserts a new user into the database.
	 *
	 * @param newUser The user object containing user details.
	 */
	public void insertUser(User newUser) throws SQLException {
		String insertSQL = "INSERT INTO users (username, password, email, nickname) VALUES(?,?,?,?)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
			ps.setString(1, newUser.getUsername());
			ps.setString(2, encrypt(newUser.getPassword()));
			ps.setString(3, newUser.getEmail());
			ps.setString(4, newUser.getNickname());
			ps.executeUpdate();
		}
	}

	/**
	 * Retrieves the user ID for a given username.
	 *
	 * @param username The username to search for.
	 * @return The user ID.
	 */
	public int getUserId(String username) throws SQLException {
		String command = "SELECT id FROM users WHERE username = ? LIMIT 1";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(command)) {
			ps.setString(1, username);
			try (ResultSet result = ps.executeQuery()) {
				if (result.next()) {
					return result.getInt("id");
				} else {
					throw new SQLException("User not found!");
				}
			}
		}
	}

	public int getRecordOwnerId(int recordId) throws SQLException {
		String command = "SELECT owner_id FROM records WHERE id = ? LIMIT 1";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(command)) {
			ps.setInt(1, recordId);
			try (ResultSet result = ps.executeQuery()) {
				if (result.next()) {
					return result.getInt("owner_id");
				} else {
					throw new SQLException("Record not found!");
				}
			}
		}
	}

	/**
	 * Updates a record in the database with new values.
	 *
	 * @param ownerId      The ID of the record owner.
	 * @param recordId     The ID of the record to update.
	 * @param newDesc      The new description (optional).
	 * @param newAsc       The new right ascension (optional).
	 * @param newDec       The new declination (optional).
	 * @param updateTime   The timestamp of the update.
	 * @param updateReason The reason for the update.
	 * @return True if the update was successful, false otherwise.
	 */
	public boolean updateRecord(int ownerId, int recordId, String newDesc, String newAsc, String newDec,
								long updateTime, String updateReason) throws SQLException {

		// Build update statement
		StringBuilder query = new StringBuilder("UPDATE records SET ");
		List<String> params = new ArrayList<>();
		if (newDesc != null) {
			query.append("description = ?, ");
			params.add(newDesc);
		}
		if (newAsc != null) {
			query.append("right_ascension = ?, ");
			params.add(newAsc);
		}
		if (newDec != null) {
			query.append("declination = ?, ");
			params.add(newDec);
		}
		query.append("update_reason = ?, ");
		query.append("modified = ?");
		query.append(" WHERE owner_id = ? AND id = ?");

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(query.toString())) {
			int i = 0;
			for (; i < params.size(); i++) {
				ps.setString(i + 1, params.get(i));
			}
			ps.setString(++i, updateReason);
			ps.setLong(++i, updateTime);
			ps.setInt(++i, ownerId);
			ps.setInt(++i, recordId);
			return (ps.executeUpdate() > 0);
		}
	}

	/**
	 * Inserts a new observatory into the database.
	 *
	 * @param observatory The observatory object.
	 * @param weatherId   The ID of the associated weather data (optional).
	 * @return The generated ID of the inserted observatory.
	 */
	private int insertObservatory(Observatory observatory, Integer weatherId) throws SQLException {
		String insertSQL = "INSERT INTO observatories (name, latitude, longitude, weather_id) VAlUES (?,?,?,?)";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
			ps.setString(1, observatory.name());
			ps.setString(2, observatory.latitude());
			ps.setString(3, observatory.longitude());
			if (weatherId != null) {
				ps.setInt(4, weatherId);
			} else {
				ps.setNull(4, Types.INTEGER);
			}
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getInt(1);
				} else {
					throw new SQLException("Error in getting observation id!");
				}
			}
		}
	}

	/**
	 * Inserts weather data into the database.
	 *
	 * @param weatherData The weather data object containing at least temperature, Others optional.
	 * @return The generated ID of the inserted weather data.
	 */
	private int insertWeatherData(WeatherData weatherData) throws SQLException {
		String insertSQL = "INSERT INTO weather " +
			"(temperature, pressure, humidity, cloud_cover, light_volume) VAlUES (?,?,?,?,?)";
		try(Connection conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(insertSQL)) {
			ps.setString(1, weatherData.temperature());
			setNullableString(ps, 2, weatherData.pressure());
			setNullableString(ps, 3, weatherData.humidity());
			setNullableString(ps, 4, weatherData.totalCloudCover());
			setNullableString(ps, 5, weatherData.radiationGlobalAccumulation());
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getInt(1);
				} else {
					throw new SQLException("Error in getting weather id!");
				}
			}
		}
	}

	/**
	 * Sets a nullable string parameter in a prepared statement.
	 *
	 * @param ps    The prepared statement.
	 * @param index The parameter index.
	 * @param value The string value to set (nullable).
	 */
	private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
		if (value != null) {
			ps.setString(index, value);
		} else {
			ps.setNull(index, Types.VARCHAR);
		}
	}

	/**
	 * Inserts a new observation record into the database.
	 *
	 * @param record The observation record object containing all relevant details.
	 */
	public void insertRecord(ObservationRecord record) throws SQLException {
		int ownerId = getUserId(record.getOwner());
		Integer observatoryId = null;
		Integer weatherId = null;

		// Insert weather data if available and get its ID.
		if (record.hasWeatherData()) {
			weatherId = insertWeatherData(record.getWeatherData());
		}

		// Insert observatory data if available and get its ID.
		if (record.hasObservatory()) {
			observatoryId = insertObservatory(record.getObservatory(), weatherId);
		}

		String insertSQL = "INSERT INTO records " +
			"(identifier, " +
			"description, " +
			"payload, " +
			"right_ascension, " +
			"declination, " +
			"owner_id, " +
			"time_received, " +
			"update_reason, " +
			"modified, " +
			"observatory_id)" +
			" VALUES(?,?,?,?,?,?,?,?,?,?)";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
			ps.setString(1, record.getIdentifier());
			ps.setString(2, record.getDescription());
			ps.setString(3, record.getPayload());
			ps.setString(4, record.getRightAscension());
			ps.setString(5, record.getDeclination());
			ps.setInt(6, ownerId);
			ps.setLong(7, Util.timeZonedToLong(record.getTimeReceived()));
			ps.setString(8, record.getUpdateReason());
			ps.setLong(9, Util.timeZonedToLong(record.getUpdateTime()));
			if (observatoryId != null) {
				ps.setInt(10, observatoryId);
			} else {
				ps.setNull(10, Types.INTEGER);
			}
			ps.executeUpdate();
		}
	}

	/**
	 * Retrieves observations from the database based on a search query. Default search query returns everything.
	 *
	 * @param searchQuery The search query object containing the SQL command and parameters.
	 * @return A JSON string representing the retrieved observations.
	 */
	public String getObservations(SearchQuery searchQuery) throws SQLException {
		String command = searchQuery.getCommand();
		JSONArray jsonArray = new JSONArray();

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(command)) {
			searchQuery.setParams(ps);
			try (ResultSet results = ps.executeQuery()) {

				while (results.next()) {
					ObservationRecord obsRec;
					Integer idx = results.getInt("id");
					String id = results.getString("identifier");
					String desc = results.getString("description");
					String payload = results.getString("payload");
					String asc = results.getString("right_ascension");
					String dec = results.getString("declination");
					String owner = results.getString("owner");
					long time = results.getLong("time_received");
					String updateStr = results.getString("update_reason");
					long modified = results.getLong("modified");
					Observatory obs = null;
					WeatherData wData = null;

					// Check if observatory data is available.
					if (results.getString("name") != null) {
						obs = new Observatory(
							results.getString("name"),
							results.getString("latitude"),
							results.getString("longitude")
						);
						// Check if weather data is available.
						if (results.getString("temperature") != null) {
							wData = new WeatherData(
								results.getString("temperature"),
								results.getString("pressure"),
								results.getString("humidity"),
								results.getString("clouds"),
								results.getString("light")
							);
						}
					}
					// Create an observation record and add it to the JSON array.
					obsRec = new ObservationRecord(idx, id, desc, payload, asc,
						dec, owner, time, obs, wData, updateStr, modified);
					jsonArray.put(obsRec.getJSONObject());
				}
			}
		}
		return jsonArray.toString();
	}

	/**
	 * Checks if a user exists in the database by username.
	 *
	 * @param username The username to check.
	 * @return True if the user exists, false otherwise.
	 */
	public boolean isUser(String username) throws SQLException {
		String selectSQL = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(selectSQL)) {
			ps.setString(1, username);
			try(ResultSet results = ps.executeQuery()) {
				return results.next();
			}
		}
	}

	/**
	 * Checks if an email is already used in the database.
	 *
	 * @param email The email to check.
	 * @return True if the email is used, false otherwise.
	 */
	public boolean usedEmail(String email) throws SQLException {
		String selectSQL = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(selectSQL)) {
			ps.setString(1, email);
			try(ResultSet results = ps.executeQuery()) {
				return results.next();
			}
		}
	}

	/**
	 * Authenticates a user by verifying their username and password.
	 *
	 * @param username The username of the user.
	 * @param password The plain text password to verify.
	 * @return True if the authentication is successful, false otherwise.
	 */
	public boolean authenticateUser(String username, String password) throws SQLException {
		String selectSQL = "SELECT password FROM users WHERE username = ? LIMIT 1";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(selectSQL)) {
			ps.setString(1, username);
			try (ResultSet results = ps.executeQuery()) {
				if (results.next()) {
					String hashedPassword = results.getString("Password");
					return hashedPassword.equals(Crypt.crypt(password, hashedPassword));
				}
			}
		}
		return false;
	}

}
