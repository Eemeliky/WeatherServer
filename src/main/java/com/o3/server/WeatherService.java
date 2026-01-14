package com.o3.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherService {

	private final HttpClient client;

	public WeatherService() {
		client = HttpClient.newHttpClient();
	}

	/**
	 * Retrieves weather data for a given latitude and longitude.
	 *
	 * @param latitude  The latitude of the location.
	 * @param longitude The longitude of the location.
	 * @return A WeatherData object containing the weather information, or null if an error occurs.
	 */
	public WeatherData getData(String latitude, String longitude) {
		String targetURL = "http://localhost:4001/wfs?latlon=" + latitude + "," + longitude +
			"&parameters=Temperature,Pressure,Humidity,TotalCloudCover,RadiationGlobalAccumulation";
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(targetURL))
				.GET()
				.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				System.err.println("Response status code [" + response.statusCode() + "]");
				return null;
			}

			String[] data = lazyParseXML(response.body());
			if (data[0] != null) {
				String temp = celsiusToKelvin(data[0]);
				return new WeatherData(temp, data[1], data[2], data[3], data[4]);
			}

		} catch (Exception e) {
			System.err.println("Weather service error: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Parses an XML string to extract weather parameter values.
	 * This method uses a simple regex-based approach to extract values from the XML.
	 *
	 * @param xmlString The XML string to parse.
	 * @return An array of weather parameter values (temperature, pressure, humidity, cloud cover, radiation).
	 */
	private String[] lazyParseXML(String xmlString) {
		String[] data = new String[5];
		Pattern pattern = Pattern.compile("<BsWfs:ParameterValue>(.*?)</BsWfs:ParameterValue>");
		Matcher matcher = pattern.matcher(xmlString);
		int i = 0;
		while (matcher.find()) {
			if ((matcher.group(1) == null) || i > 4) {
				break;
			}
			data[i++] = matcher.group(1);
		}
		return data;
	}

	/**
	 * Converts a temperature value from Celsius to Kelvin.
	 *
	 * @param temp The temperature in Celsius as a string.
	 * @return The temperature in Kelvin as a string, formatted to two decimal places.
	 */
	private String celsiusToKelvin(String temp) {
		double celsius = Float.parseFloat(temp);
		double kelvin = celsius + 273.15;
		return String.format("%.2f", kelvin);
	}

}
