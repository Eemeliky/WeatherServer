/*

package com.o3.tests;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static com.o3.tests.matchers.IsHttpSuccess.httpSuccess;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class Feature9Tests extends TestBase {

	private final String testPayload = "On February 25, 2025, Jupiter's Great Red Spot (GRS) was prominently " +
		"visible from Earth. The GRS is a massive, high-pressure storm system located in Jupiter's " +
		"southern hemisphere, rotating counterclockwise. During this time, Jupiter was observable " +
		"well past midnight, setting after 3 a.m. local time on the 1st and by 1:30 a.m. on the 28th. " +
		"Telescopic views revealed its dark equatorial belts, four Galilean moons, and occasionally " +
		"the Great Red Spot. Scopes over 6 inches in diameter provided finer details of Jupiter's " +
		"dynamic atmosphere, especially during moments of steady seeing. Jupiter's disk spanned " +
		"43 arcseconds on February 1 and decreased to 40 arcseconds by the end of the month. Its " +
		"rapid rotation, completing in under 10 hours, allowed observers to notice cloud motion " +
		"within 10 to 15 minutes.";


	@Test
	@DisplayName("Test description autofill")
	@Order(2)
	void testAiDesc() throws InterruptedException {
		logger.info("Testing AI message summarizing...");
		User user = Helpers.createUser();
		registerJson(user);
		testClient.setAuth(user.getUsername(), user.getPassword());

		Message message = new Message(
			Helpers.generateRandomString(1),
			"",
			testPayload);
		StatusCode result = testClient.sendMessage(message.toJsonString());
		assertThat(result.getBody(), result, is(httpSuccess()));

		// Uses search functionality
		// CHANGE CLIENT TIMEOUT TO GREATER THAN DEFAULT (20 secs) IF GENERATION TAKES LONG
		SearchQuery query = new SearchQuery(user.getNickname(), null, null, message.getIdentifier());
		StatusCode response = testClient.searchMessages(query);
		assertThat(response.getBody(), response, is(httpSuccess()));
		String messages = response.getBody();
		JSONArray messageArray = new JSONArray(messages);

		JSONObject AIDescMessage = messageArray.getJSONObject(messageArray.length() - 1);
		assertFalse(AIDescMessage.getString("recordDescription").isEmpty());
		assertNotEquals("N/A", AIDescMessage.getString("recordDescription"));
		assertNotEquals(message.getDescription(), AIDescMessage.getString("recordDescription"));
	}
}

 */
