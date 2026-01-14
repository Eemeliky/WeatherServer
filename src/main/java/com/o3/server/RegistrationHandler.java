package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import static com.o3.server.Util.sendResponse;

public class RegistrationHandler implements HttpHandler {

	private final UserAuthenticator userAuthenticator;

	public RegistrationHandler(UserAuthenticator authenticator) {
		this.userAuthenticator = authenticator;
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
				if (method.equals("POST")) {
					postHandler(exchange);
				} else {
					Util.notSupported(exchange);
				}
			} else {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Incorrect Content-Type".getBytes());
			}
		} catch (Exception e) {
			System.err.println("Unhandled server error in registration: " + e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
	}

	/**
	 * Handles POST requests for user registration.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 */
	private void postHandler(HttpExchange exchange) {
		try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(isr)) {

			String text = br.lines().collect(Collectors.joining("\n"));
			if (text.isEmpty()) {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Request body is empty".getBytes());
				return;
			}

			JSONObject jsonObject = new JSONObject(text);

			// Get user information from message
			String username = Util.extractField(jsonObject, "username", "string");
			String password = Util.extractField(jsonObject, "password", "string");
			String email = Util.extractField(jsonObject, "email", "string");
			String nickname = null;
			if (jsonObject.has("userNickname") && !jsonObject.isNull("userNickname")) {
				nickname = jsonObject.getString("userNickname");
			}

			// Basic barebones user data validation
			if (userAuthenticator.invalidString(username)) {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Username cannot be empty!".getBytes());
				return;
			}
			if (userAuthenticator.invalidString(password)) {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Password cannot be empty!".getBytes());
				return;
			}
			if (userAuthenticator.invalidEmail(email)) {
				sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid email address!".getBytes());
				return;
			}
			if (userAuthenticator.isUser(username)) {
				sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, "Username is already in use!".getBytes());
				return;
			}
			if (userAuthenticator.emailInUse(email)) {
				sendResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, "Email is already in use!".getBytes());
				return;
			}

			// Register user
			registerUser(username, password, email, nickname);

			sendResponse(exchange, HttpURLConnection.HTTP_OK, "User registered successfully".getBytes());

		} catch (JSONException je) {
			System.err.println("Invalid JSON format: " + je.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON format!".getBytes());
		} catch (Exception e) {
			System.err.println("Error in registration: " + e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				"Internal server error during registration".getBytes());
		}
	}

	/**
	 * Registers a new user in the system.
	 *
	 * @param username The username of the new user.
	 * @param password The password of the new user.
	 * @param email    The email address of the new user.
	 * @param nickname The nickname of the new user (optional).
	 */
	private void registerUser(String username, String password, String email, String nickname) throws SQLException {
		if (nickname != null) {
			userAuthenticator.addUser(new User(username, password, email, nickname));
			return;
		}
		userAuthenticator.addUser(new User(username, password, email));
	}

}
