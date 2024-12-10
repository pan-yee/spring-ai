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

package org.springframework.ai.ytoai.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Geng Rong
 */
@EnabledIfEnvironmentVariable(named = "YTO_AI_API_KEY", matches = ".+")
public class YtoAiApiIT {

	YtoAiApi ytoAiApi = new YtoAiApi(System.getenv("YTO_AI_API_KEY"));

	@Test
	void chatCompletionEntity() {
		YtoAiApi.ChatCompletionMessage chatCompletionMessage = new YtoAiApi.ChatCompletionMessage("Hello world", YtoAiApi.ChatCompletionMessage.Role.USER);
		ResponseEntity<YtoAiApi.ChatCompletion> response = this.ytoAiApi
			.chatCompletionEntity(new YtoAiApi.ChatCompletionRequest(List.of(chatCompletionMessage), "glm-3-turbo", 0.7, false));

		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void chatCompletionEntityWithMoreParams() {
		YtoAiApi.ChatCompletionMessage chatCompletionMessage = new YtoAiApi.ChatCompletionMessage("Hello world", YtoAiApi.ChatCompletionMessage.Role.USER);
		ResponseEntity<YtoAiApi.ChatCompletion> response = this.ytoAiApi
			.chatCompletionEntity(new YtoAiApi.ChatCompletionRequest(List.of(chatCompletionMessage), "glm-3-turbo", 1024, null,
					false, 0.95, 0.7, null, null, null, "test_request_id", false));

		assertThat(response).isNotNull();
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void chatCompletionStream() {
		YtoAiApi.ChatCompletionMessage chatCompletionMessage = new YtoAiApi.ChatCompletionMessage("Hello world", YtoAiApi.ChatCompletionMessage.Role.USER);
		Flux<YtoAiApi.ChatCompletionChunk> response = this.ytoAiApi
			.chatCompletionStream(new YtoAiApi.ChatCompletionRequest(List.of(chatCompletionMessage), "glm-3-turbo", 0.7, true));

		assertThat(response).isNotNull();
		assertThat(response.collectList().block()).isNotNull();
	}

	@Test
	void embeddings() {
		ResponseEntity<YtoAiApi.EmbeddingList<YtoAiApi.Embedding>> response = this.ytoAiApi
			.embeddings(new YtoAiApi.EmbeddingRequest<>("Hello world", YtoAiApi.DEFAULT_EMBEDDING_MODEL, 1024));

		assertThat(response).isNotNull();
		assertThat(Objects.requireNonNull(response.getBody()).data()).hasSize(1);
		assertThat(response.getBody().data().get(0).embedding()).hasSize(1024);
	}

}
