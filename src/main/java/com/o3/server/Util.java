package com.o3.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import java.util.List;

public class Util {

	public static final Map<Integer, String> STATUS_MESSAGES = Map.of(
		HttpURLConnection.HTTP_OK, "OK",
		HttpURLConnection.HTTP_BAD_REQUEST, "Bad Request",
		HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized",
		HttpURLConnection.HTTP_FORBIDDEN, "Forbidden",
		HttpURLConnection.HTTP_NOT_FOUND, "Not Found",
		HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error",
		HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Implemented"
	);

	public Util() {
	}


	/**
	 * Sends an HTTP response with the specified response code and no response body.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 * @param respCode The HTTP response code to send.
	 */
	public static void sendResponse(HttpExchange exchange, int respCode) {
		try {
			exchange.sendResponseHeaders(respCode, -1);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.flush();
			}
		} catch (IOException e) {
			System.err.println("CRITICAL ERROR in sending response: " + e.getMessage());
		}

	}

	/**
	 * Sends an HTTP response with the specified response code and response body.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 * @param respCode The HTTP response code to send.
	 * @param wBytes   The response body as a byte array.
	 */
	public static void sendResponse(HttpExchange exchange, int respCode, byte[] wBytes) {
		try {
			exchange.sendResponseHeaders(respCode, wBytes.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				if (wBytes.length > 0) {
					outputStream.write(wBytes);
				}
				outputStream.flush();
			}
		} catch (IOException e) {
			System.err.println("CRITICAL ERROR in sending response: " + e.getMessage());
		}
	}

	/**
	 * Extracts a field from a JSON object and returns its value as a string.
	 *
	 * @param jsonObject The JSON object to extract the field from.
	 * @param field      The name of the field to extract.
	 * @param type       The expected type of the field ("STRING" or "BIGDECIMAL").
	 * @return The value of the field as a string.
	 * @throws JSONException           If the field is missing or null in the JSON object.
	 * @throws IllegalArgumentException If the specified type is invalid.
	 */
	public static String extractField(JSONObject jsonObject, String field, String type)
		throws JSONException, IllegalArgumentException {
		switch (type.toUpperCase()) {
			case "STRING":
				if (jsonObject.has(field) && !jsonObject.isNull(field)) {
					return jsonObject.getString(field);
				}
				throw new JSONException("Invalid field [" + field + "]");
			case "BIGDECIMAL":
				if (jsonObject.has(field) && !jsonObject.isNull(field)) {
					return jsonObject.getBigDecimal(field).toString();
				}
				throw new JSONException("Invalid field [" + field + "]");
			default:
				throw new IllegalArgumentException("Illegal argument type [" + type + "]");
		}
	}


	/**
	 * Retrieves the "Content-Type" header from the HTTP request headers.
	 *
	 * @param headers The HTTP request headers.
	 * @return The value of the "Content-Type" header, or "Unknown" if it is not present.
	 */
	public static String getContentType(Headers headers) {
		return headers.getOrDefault("Content-Type", List.of())
			.stream()
			.findFirst()
			.orElse("Unknown");
	}

	/**
	 * Sends a "Bad Request" response for unsupported HTTP methods.
	 *
	 * @param exchange The HTTP exchange object containing the request and response.
	 */
	public static void notSupported(HttpExchange exchange) {
		byte[] bytes = STATUS_MESSAGES
			.get(HttpURLConnection.HTTP_BAD_REQUEST)
			.getBytes(StandardCharsets.UTF_8);
		sendResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, bytes);
	}

	/**
	 * Retrieves the current time in UTC as a ZonedDateTime.
	 *
	 * @return The current time in UTC.
	 */
	public static ZonedDateTime getCurrentTime() {
		return ZonedDateTime.now(ZoneId.of("UTC"));
	}

	/**
	 * Converts a ZonedDateTime to a long representing the epoch time in milliseconds.
	 *
	 * @param zTime The ZonedDateTime to convert.
	 * @return The epoch time in milliseconds.
	 */
	public static long timeZonedToLong(ZonedDateTime zTime) {
		return zTime.toInstant().toEpochMilli();
	}

	/**
	 * Converts a long representing the epoch time in milliseconds to a ZonedDateTime in UTC.
	 *
	 * @param epoch The epoch time in milliseconds.
	 * @return The corresponding ZonedDateTime in UTC.
	 */
	public static ZonedDateTime timeLongToZoned(long epoch) {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
	}

}
