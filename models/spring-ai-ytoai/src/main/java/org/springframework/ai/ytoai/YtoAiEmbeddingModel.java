package org.springframework.ai.ytoai;/*
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

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.*;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.ytoai.api.YtoAiApi;
import org.springframework.ai.ytoai.api.YtoApiConstants;
import org.springframework.ai.ytoai.metadata.YtoAiUsage;
import org.springframework.lang.Nullable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ZhiPuAI Embedding Model implementation.
 *
 * @author Geng Rong
 * @since 1.0.0 M1
 */
public class YtoAiEmbeddingModel extends AbstractEmbeddingModel {

	private static final Logger logger = LoggerFactory.getLogger(YtoAiEmbeddingModel.class);

	private static final EmbeddingModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultEmbeddingModelObservationConvention();

	private final YtoAiEmbeddingOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	private final YtoAiApi ytoAiApi;

	private final MetadataMode metadataMode;

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	/**
	 * Conventions to use for generating observations.
	 */
	private EmbeddingModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	/**
	 * Constructor for the ZhiPuAiEmbeddingModel class.
	 * @param ytoAiApi The ZhiPuAiApi instance to use for making API requests.
	 */
	public YtoAiEmbeddingModel(YtoAiApi ytoAiApi) {
		this(ytoAiApi, MetadataMode.EMBED);
	}

	/**
	 * Initializes a new instance of the ZhiPuAiEmbeddingModel class.
	 * @param ytoAiApi The ZhiPuAiApi instance to use for making API requests.
	 * @param metadataMode The mode for generating metadata.
	 */
	public YtoAiEmbeddingModel(YtoAiApi ytoAiApi, MetadataMode metadataMode) {
		this(ytoAiApi, metadataMode,
				YtoAiEmbeddingOptions.builder().withModel(YtoAiApi.DEFAULT_EMBEDDING_MODEL).build(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * Initializes a new instance of the ZhiPuAiEmbeddingModel class.
	 * @param ytoAiApi The ZhiPuAiApi instance to use for making API requests.
	 * @param metadataMode The mode for generating metadata.
	 * @param ytoAiEmbeddingOptions The options for ZhiPuAI embedding.
	 */
	public YtoAiEmbeddingModel(YtoAiApi ytoAiApi, MetadataMode metadataMode,
			YtoAiEmbeddingOptions ytoAiEmbeddingOptions) {
		this(ytoAiApi, metadataMode, ytoAiEmbeddingOptions, RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * Initializes a new instance of the ZhiPuAiEmbeddingModel class.
	 * @param ytoAiApi The ZhiPuAiApi instance to use for making API requests.
	 * @param metadataMode The mode for generating metadata.
	 * @param ytoAiEmbeddingOptions The options for ZhiPuAI embedding.
	 * @param retryTemplate - The RetryTemplate for retrying failed API requests.
	 */
	public YtoAiEmbeddingModel(YtoAiApi ytoAiApi, MetadataMode metadataMode,
			YtoAiEmbeddingOptions ytoAiEmbeddingOptions, RetryTemplate retryTemplate) {
		this(ytoAiApi, metadataMode, ytoAiEmbeddingOptions, retryTemplate, ObservationRegistry.NOOP);
	}

	/**
	 * Initializes a new instance of the ZhiPuAiEmbeddingModel class.
	 * @param ytoAiApi - The ZhiPuAiApi instance to use for making API requests.
	 * @param metadataMode - The mode for generating metadata.
	 * @param options - The options for ZhiPuAI embedding.
	 * @param retryTemplate - The RetryTemplate for retrying failed API requests.
	 * @param observationRegistry - The ObservationRegistry used for instrumentation.
	 */
	public YtoAiEmbeddingModel(YtoAiApi ytoAiApi, MetadataMode metadataMode, YtoAiEmbeddingOptions options,
			RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {
		Assert.notNull(ytoAiApi, "ZhiPuAiApi must not be null");
		Assert.notNull(metadataMode, "metadataMode must not be null");
		Assert.notNull(options, "options must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		Assert.notNull(observationRegistry, "observationRegistry must not be null");

		this.ytoAiApi = ytoAiApi;
		this.metadataMode = metadataMode;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}

	@Override
	public float[] embed(Document document) {
		Assert.notNull(document, "Document must not be null");
		return this.embed(document.getFormattedContent(this.metadataMode));
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		Assert.notEmpty(request.getInstructions(), "At least one text is required!");
		if (request.getInstructions().size() != 1) {
			logger.warn(
					"ZhiPu Embedding does not support batch embedding. Will make multiple API calls to embed(Document)");
		}
		YtoAiEmbeddingOptions requestOptions = mergeOptions(request.getOptions(), this.defaultOptions);

		var observationContext = EmbeddingModelObservationContext.builder()
			.embeddingRequest(request)
			.provider(YtoApiConstants.PROVIDER_NAME)
			.requestOptions(requestOptions)
			.build();

		return EmbeddingModelObservationDocumentation.EMBEDDING_MODEL_OPERATION
			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry)
			.observe(() -> {
				List<float[]> embeddingList = new ArrayList<>();

				var totalUsage = new YtoAiApi.Usage(0, 0, 0);

				for (String inputContent : request.getInstructions()) {
					var apiRequest = createEmbeddingRequest(inputContent, requestOptions);

					YtoAiApi.EmbeddingList<YtoAiApi.Embedding> response = this.retryTemplate
						.execute(ctx -> this.ytoAiApi.embeddings(apiRequest).getBody());
					if (response == null || response.data() == null || response.data().isEmpty()) {
						logger.warn("No embeddings returned for input: {}", inputContent);
						embeddingList.add(new float[0]);
					}
					else {
						int completionTokens = totalUsage.completionTokens() + response.usage().completionTokens();
						int promptTokens = totalUsage.promptTokens() + response.usage().promptTokens();
						int totalTokens = totalUsage.totalTokens() + response.usage().totalTokens();
						totalUsage = new YtoAiApi.Usage(completionTokens, promptTokens, totalTokens);
						embeddingList.add(response.data().get(0).embedding());
					}
				}

				String model = (request.getOptions() != null && request.getOptions().getModel() != null)
						? request.getOptions().getModel() : "unknown";

				var metadata = new EmbeddingResponseMetadata(model, YtoAiUsage.from(totalUsage));

				var indexCounter = new AtomicInteger(0);

				List<Embedding> embeddings = embeddingList.stream()
					.map(e -> new Embedding(e, indexCounter.getAndIncrement()))
					.toList();

				EmbeddingResponse embeddingResponse = new EmbeddingResponse(embeddings, metadata);

				observationContext.setResponse(embeddingResponse);

				return embeddingResponse;
			});
	}

	/**
	 * Merge runtime and default {@link EmbeddingOptions} to compute the final options to
	 * use in the request.
	 */
	private YtoAiEmbeddingOptions mergeOptions(@Nullable EmbeddingOptions runtimeOptions,
			YtoAiEmbeddingOptions defaultOptions) {
		var runtimeOptionsForProvider = ModelOptionsUtils.copyToTarget(runtimeOptions, EmbeddingOptions.class,
				YtoAiEmbeddingOptions.class);

		if (runtimeOptionsForProvider == null) {
			return defaultOptions;
		}

		return YtoAiEmbeddingOptions.builder()
			.withModel(ModelOptionsUtils.mergeOption(runtimeOptionsForProvider.getModel(), defaultOptions.getModel()))
			.withDimensions(ModelOptionsUtils.mergeOption(runtimeOptionsForProvider.getDimensions(),
					defaultOptions.getDimensions()))
			.build();
	}

	private YtoAiApi.EmbeddingRequest<String> createEmbeddingRequest(String text, EmbeddingOptions requestOptions) {
		return new YtoAiApi.EmbeddingRequest<>(text, requestOptions.getModel(), requestOptions.getDimensions());
	}

	public void setObservationConvention(EmbeddingModelObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

}
