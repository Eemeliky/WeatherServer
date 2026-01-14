package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.o3.server.Util.sendResponse;

public class SearchHandler implements HttpHandler {

	private final MessageDataBase database;

	/**
	 * Constructor to initialize the SearchHandler with a database connection.
	 *
	 * @param dbName The name of the database file.
	 */
	public SearchHandler(String dbName) throws SQLException, IOException {
		this.database = MessageDataBase.getInstance(dbName);
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
				if (method.equals("GET")) {
					getHandler(exchange);
				} else {
					Util.notSupported(exchange);
				}
			} else {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect Content-Type".getBytes());
			}
		} catch (AccessDeniedException ade) {
			System.err.println("Authentication error in search handler: " + ade.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_UNAUTHORIZED).getBytes());
		} catch (IllegalArgumentException iae) {
			System.err.println("Argument Error: " + iae.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_BAD_REQUEST).getBytes());
		} catch (Exception e) {
			System.err.println("Unhandled server error in search: " + e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
	}

	/**
	 * Handles GET requests to search for observation records.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 * @throws IllegalArgumentException If the search query is invalid or missing.
	 * @throws AccessDeniedException    If the user is not authorized.
	 */
	private void getHandler(HttpExchange exchange) throws IllegalArgumentException, AccessDeniedException {
		String username = getUsername(exchange);
		try {
			String query = exchange.getRequestURI().getQuery();
			if (query == null || query.isEmpty()) {
				throw new IllegalArgumentException("Search query cannot be empty!");
			}

			// Parse the query string into search arguments.
			Map<String, String> searchArgs = getSearchArgs(query);
			SearchQuery searchQuery = new SearchQuery(searchArgs);

			// Get searched observations
			String responseString = database.getObservations(searchQuery);
			sendResponse(exchange, HttpURLConnection.HTTP_OK,
				responseString.getBytes(StandardCharsets.UTF_8));

		} catch (SQLException e) {
			System.err.println("SQL error in getting searched records for user: " + username + "\n"
				+ e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				Util.STATUS_MESSAGES.get(HttpURLConnection.HTTP_INTERNAL_ERROR).getBytes());
		}
	}

	/**
	 * Parses the query string into a map of search arguments.
	 *
	 * @param query The query string from the URI.
	 * @return A map of search arguments (key-value pairs).
	 * @throws IllegalArgumentException If the query string contains invalid or unsupported arguments.
	 */
	private Map<String, String> getSearchArgs(String query) throws IllegalArgumentException {
		Map<String, String> searchArgs = new HashMap<>();
		String[] argPairs = query.split("&");
		for (String arg : argPairs) {
			if (arg != null && !arg.isEmpty()) {
				String[] keyValuePair = arg.split("=", 2);
				if (keyValuePair.length != 2) {
					throw new IllegalArgumentException("Empty search argument!");
				}
				String key = keyValuePair[0];
				String value =  keyValuePair[1];
				if (!key.equals("identification") && !key.equals("nickname") &&
					!key.equals("after") && !key.equals("before")) {
					throw new IllegalArgumentException("Invalid search argument!");
				}
				searchArgs.put(key, value);
			}
		}
		return searchArgs;
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

}
