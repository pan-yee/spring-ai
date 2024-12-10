/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.ytoai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ytoai.api.YtoAiApi;
import org.springframework.ai.ytoai.api.YtoAiImageApi;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * @author Geng Rong
 */
@SpringBootConfiguration
public class YtoAiTestConfiguration {

	@Bean
	public YtoAiApi ytoAiApi() {
		return new YtoAiApi(getApiKey());
	}

	@Bean
	public YtoAiImageApi ytoAiImageApi() {
		return new YtoAiImageApi(getApiKey());
	}

	private String getApiKey() {
		String apiKey = System.getenv("YTO_AI_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException(
					"You must provide an API key.  Put it in an environment variable under the name YTO_AI_API_KEY");
		}
		return apiKey;
	}

	@Bean
	public YtoAiChatModel ytoAiChatModel(YtoAiApi api) {
		return new YtoAiChatModel(api);
	}

	@Bean
	public YtoAiImageModel ytoAiImageModel(YtoAiImageApi imageApi) {
		return new YtoAiImageModel(imageApi);
	}

	@Bean
	public EmbeddingModel ytoAiEmbeddingModel(YtoAiApi api) {
		return new YtoAiEmbeddingModel(api);
	}

}
