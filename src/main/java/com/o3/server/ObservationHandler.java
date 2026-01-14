package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.stream.Collectors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.o3.server.Util.sendResponse;

public class ObservationHandler implements HttpHandler {

	private final MessageDataBase database;
	private final WeatherService weatherService;

	/**
	 * Constructor to initialize the ObservationHandler with a database and weather service.
	 *
	 * @param dbName        The name of the database file.
	 * @param ws            The weather service instance for fetching weather data.
	 */
	public ObservationHandler(String dbName, WeatherService ws) throws SQLException, IOException {
		database = MessageDataBase.getInstance(dbName);
		weatherService = ws;
	}


	/**
	 * Handles incoming HTTP requests and routes them to the appropriate handler based on the HTTP method.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 */
	@Override
	public void handle(HttpExchange exchange) {
		try {
			String contentType = Util.getContentType(exchange.getRequestHeaders());
			if (contentType.equals("application/json")) {
				String method = exchange.getRequestMethod().toUpperCase();
				switch (method) {
					case "POST":
						postHandler(exchange);
						break;
					case "GET":
						getHandler(exchange);
						break;
					case "PUT":
						putHandler(exchange);
						break;
					default:
						Util.notSupported(exchange);
						break;
				}
			} else {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect Content-Type".getBytes());
			}
		} catch (AccessDeniedException ade) {
			System.err.println("Authentication error in observation handler: " + ade.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_UNAUTHORIZED).getBytes());
		} catch (Exception e) {
			System.err.println("Unhandled server error in observation handler: " + e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
	}

	/**
	 * Handles PUT requests to update an existing observation record.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 * @throws AccessDeniedException If the user is not authorized.
	 */
	private void putHandler(HttpExchange exchange) throws AccessDeniedException {
		String username = getUsername(exchange);
		try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(isr)) {

			String text = br.lines().collect(Collectors.joining("\n"));
			JSONObject jsonObject = new JSONObject(text);

			// Parse message and get required data
			String newDesc = Util.extractField(jsonObject, "recordDescription", "string");
			String updateReason = jsonObject.optString("updateReason", "N/A");
			String newAsc = jsonObject.optString("recordRightAscension", null);
			String newDec = jsonObject.optString("recordDeclination", null);
			String query = exchange.getRequestURI().getQuery();
			int recordId = parseQuery(query);
			int ownerId = database.getUserId(username);
			long updateTime = Util.timeZonedToLong(Util.getCurrentTime());

			// Check if user is authorized to update the record
			if (ownerId != database.getRecordOwnerId(recordId)) {
				int code = HttpURLConnection.HTTP_FORBIDDEN;
				sendResponse(exchange, code, Util.STATUS_MESSAGES.get(code).getBytes());
				return;
			}

			// Update record
			boolean updated = database.updateRecord(ownerId, recordId, newDesc, newAsc, newDec, updateTime, updateReason);
			if (!updated) {
				int code = HttpURLConnection.HTTP_INTERNAL_ERROR;
				sendResponse(exchange, code, Util.STATUS_MESSAGES.get(code).getBytes());
			} else {
				sendResponse(exchange, HttpURLConnection.HTTP_OK);
			}

		} catch (JSONException je) {
			System.err.println("Invalid (PUT) JSON format: " + je.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON format!".getBytes());
		} catch (IllegalArgumentException iae) {
			System.err.println("Argument error in update: " + iae.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_BAD_REQUEST).getBytes());
		} catch (SQLException SQLe) {
			System.err.println("SQL error in updating message: " + SQLe.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		} catch (IOException ioe) {
			System.err.println("File error in reading (PUT) request body: " + ioe.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		}
	}

	/**
	 * Handles GET requests to retrieve observation records.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 * @throws AccessDeniedException If the user is not authorized.
	 */
	private void getHandler(HttpExchange exchange) throws AccessDeniedException {
		String username = getUsername(exchange);
		try {
			SearchQuery searchQuery = new SearchQuery(null);	// Default search query
			String responseString = database.getObservations(searchQuery);
			sendResponse(exchange, HttpURLConnection.HTTP_OK,
				responseString.getBytes(StandardCharsets.UTF_8));
		} catch (SQLException e) {
			System.err.println("SQL error in getting observation records for user: " + username + "\n"
				+ e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		}
	}

	/**
	 * Handles POST requests to add a new observation record.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 * @throws AccessDeniedException If the user is not authorized.
	 */
	private void postHandler(HttpExchange exchange) throws AccessDeniedException {
		String username = getUsername(exchange);
		try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(isr)) {

			String text = br.lines().collect(Collectors.joining("\n"));
			if (text.isEmpty()) {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Request body is empty".getBytes());
				return;
			}

			JSONObject jsonObject = new JSONObject(text);
			ObservationRecord record = parseMessage(jsonObject, username);

			database.insertRecord(record);
			sendResponse(exchange, HttpURLConnection.HTTP_OK);

		} catch (JSONException je) {
			System.err.println("Invalid (POST) JSON format: " + je.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON format!".getBytes());
		} catch (ConnectException ce) {
			System.err.println("Connecting error with weather service: " + ce.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_UNAUTHORIZED).getBytes());
		} catch (IOException ioe) {
			System.err.println("File error in reading (POST) request body: " + ioe.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		} catch (SQLException SQLe) {
			System.err.println("SQL error in adding new message: " + SQLe.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		} catch (IllegalArgumentException iae) {
			System.err.println("Argument Error: " + iae.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		}
	}

	/**
	 * Parses the update query string to extract the record ID.
	 *
	 * @param query The query string from the URI.
	 * @return The record ID.
	 * @throws IllegalArgumentException If the query is invalid or the ID is missing.
	 */
	private int parseQuery(String query) {
		if (query == null || query.isEmpty()) {
			throw new IllegalArgumentException("Update query cannot be empty!");
		}
		String[] argPairs = query.split("&");
		if (argPairs.length == 1) {
			for (String arg : argPairs) {
				if (arg != null && !arg.isEmpty()) {
					String[] keyValuePair = arg.split("=", 2);
					if (keyValuePair.length != 2) {
						throw new IllegalArgumentException("Empty search argument!");
					}
					String key = keyValuePair[0];
					String value =  keyValuePair[1];
					if (key.equals("id")) {
						return Integer.parseInt(value);
					} else {
						throw new IllegalArgumentException("Invalid update query argument!");
					}
				}
			}
		}
		throw new IllegalArgumentException("Invalid update query!");
	}

	/**
	 * Extracts the username from the HTTP exchange's principal.
	 *
	 * @param exchange The HTTP exchange object.
	 * @return The username.
	 * @throws AccessDeniedException If the username is invalid or missing.
	 */
	private String getUsername(HttpExchange exchange) throws AccessDeniedException {
		Principal principal = exchange.getPrincipal();
		if (principal != null && principal.getName().contains(":")) {
			return principal.getName().split(":", 2)[1];
		}
		throw new AccessDeniedException("Invalid username:password string!");
	}

	/**
	 * Parses a JSON object into an ObservationRecord.
	 *
	 * @param message The JSON object containing the observation data.
	 * @param owner   The username of the record owner.
	 * @return The parsed ObservationRecord.
	 * @throws JSONException           If the JSON format is invalid.
	 * @throws IllegalArgumentException If required fields are missing or invalid.
	 */
	private ObservationRecord parseMessage(JSONObject message, String owner)
		throws JSONException, IllegalArgumentException {

		// Extract required fields from the JSON object.
		String identifier = Util.extractField(message,"recordIdentifier", "string");
		String description = Util.extractField(message,"recordDescription", "string");
		String payload = Util.extractField(message,"recordPayload", "string");
		String rightAscension = Util.extractField(message,"recordRightAscension", "string");
		String declination = Util.extractField(message,"recordDeclination", "string");

		// Add AI created description if description is empty
		if (description.isEmpty()) {
			description = LLMService.summarize(payload);
		}

		// Extract optional fields from the JSON object.
		if (message.has("observatory")) {
			JSONArray tmp = message.optJSONArray("observatory", null);
			if (tmp != null && !tmp.isEmpty()) {
				JSONObject observatory = (JSONObject) tmp.get(0);
				String name = Util.extractField(observatory, "observatoryName", "string");
				String latitude = Util.extractField(observatory, "latitude", "bigdecimal");
				String longitude = Util.extractField(observatory, "longitude", "bigdecimal");
				WeatherData weatherData = null;
				if (message.has("observatoryWeather")) {
					weatherData = weatherService.getData(latitude, longitude);
					if (weatherData == null) {
						throw new IllegalArgumentException("Cannot get weather temperature!");
					}
				}

				Observatory obs = new Observatory(name, latitude, longitude);
				return new ObservationRecord(identifier, description, payload,
					rightAscension, declination, owner, obs, weatherData, null);
			}
			throw new JSONException("Invalid observatory field!");
		}
		return new ObservationRecord(identifier,
			description, payload, rightAscension, declination, owner, null, null, null);
	}

}
