package com.o3.server;

import com.hexadevlabs.gpt4all.LLModel;
import java.io.File;
import java.nio.file.Path;

public class LLMService {

	public LLMService() {
	}

	/**
	 * Summarizes the given input text using a local LLM.
	 *
	 * @param input The input string to summarize.
	 * @return A short description of the input text, or "N/A" if an error occurs.
	 */
	public static String summarize(String input) {

		String prompt = "Give very short description about the following text \"";

		// Limit input size
		// Falcon model seems to struggle (possibly running out of memory?) with 500+ chars long input strings
		if (input.length() > 400) {
			prompt += input.substring(0, 400) + "\"";
		} else {
			prompt += input + "\"";
		}

		// Set model name and path
		String modelFileName = "ggml-model-gpt4all-falcon-q4_0.bin";	// Change this if using any other model
		String projectPath = System.getProperty("user.dir");
		String modelFilePath = projectPath + "/models/" + modelFileName;

		// Check if model exists
		File file = new File(modelFilePath);
		if (!file.exists()) {
			System.err.println("Missing model or incorrect model name!");
			return "N/A";
		}

		try (LLModel model = new LLModel(Path.of(modelFilePath))) {
			LLModel.GenerationConfig config = LLModel.config()
				.withRepeatPenalty(5)
				.withNCtx(4096)
				.withNPredict(4096)
				.build();

			return model.generate(prompt, config, false).trim();

		} catch (Exception e) {
			System.err.println("Error on generating summary: " + e.getMessage());
			return "N/A";
		}
	}

}
