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

package org.springframework.ai.ytoai.embedding;

import java.util.List;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.ytoai.YtoAiEmbeddingModel;
import org.springframework.ai.ytoai.YtoAiEmbeddingOptions;
import org.springframework.ai.ytoai.api.YtoAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.HighCardinalityKeyNames;
import static org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.LowCardinalityKeyNames;

/**
 * Integration tests for observation instrumentation in {@link YtoAiEmbeddingModel}.
 *
 * @author Geng Rong
 */
@SpringBootTest(classes = YtoAiEmbeddingModelObservationIT.Config.class)
@EnabledIfEnvironmentVariable(named = "YTO_AI_API_KEY", matches = ".+")
public class YtoAiEmbeddingModelObservationIT {

	@Autowired
	TestObservationRegistry observationRegistry;

	@Autowired
	YtoAiEmbeddingModel embeddingModel;

	@Test
	void observationForEmbeddingOperation() {
		var options = YtoAiEmbeddingOptions.builder()
			.withModel(YtoAiApi.EmbeddingModel.Embedding_2.getValue())
			.build();

		EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of("Here comes the sun"), options);

		EmbeddingResponse embeddingResponse = this.embeddingModel.call(embeddingRequest);
		assertThat(embeddingResponse.getResults()).isNotEmpty();

		EmbeddingResponseMetadata responseMetadata = embeddingResponse.getMetadata();
		assertThat(responseMetadata).isNotNull();

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.doesNotHaveAnyRemainingCurrentObservation()
			.hasObservationWithNameEqualTo(DefaultEmbeddingModelObservationConvention.DEFAULT_NAME)
			.that()
			.hasContextualNameEqualTo("embedding " + YtoAiApi.EmbeddingModel.Embedding_2.getValue())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),
					AiOperationType.EMBEDDING.value())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_PROVIDER.asString(), AiProvider.ZHIPUAI.value())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.REQUEST_MODEL.asString(),
					YtoAiApi.EmbeddingModel.Embedding_2.getValue())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.RESPONSE_MODEL.asString(), responseMetadata.getModel())
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(),
					String.valueOf(responseMetadata.getUsage().getPromptTokens()))
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
					String.valueOf(responseMetadata.getUsage().getTotalTokens()))
			.hasBeenStarted()
			.hasBeenStopped();
	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

		@Bean
		public YtoAiApi zhiPuAiApi() {
			return new YtoAiApi(System.getenv("YTO_AI_API_KEY"));
		}

		@Bean
		public YtoAiEmbeddingModel zhiPuAiEmbeddingModel(YtoAiApi zhiPuAiApi,
														 TestObservationRegistry observationRegistry) {
			return new YtoAiEmbeddingModel(zhiPuAiApi, MetadataMode.EMBED,
					YtoAiEmbeddingOptions.builder().withModel(YtoAiApi.DEFAULT_EMBEDDING_MODEL).build(),
					RetryTemplate.defaultInstance(), observationRegistry);
		}

	}

}
