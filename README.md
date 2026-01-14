# Simple weather observation java server made as a school project

### Features:
- Live weather data fetching
- Search Functionality from saved observations
- Saved observations can be updated
- Possibility for automatic AI summary on observations
  - Project includes a `models/` folder where user needs to place his own LLM model. If you are using other model than ´ggml-model-gpt4all-falcon-q4_0.bin´ also change the model name from `LLMService.java`.