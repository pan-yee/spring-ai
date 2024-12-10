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


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.ytoai.YtoAiImageOptions;
import org.springframework.ai.ytoai.api.YtoAiImageApi;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * YtoAiImageModel is a class that implements the ImageModel interface. It provides a
 * client for calling the YtoAI image generation API.
 *
 * @author Geng Rong
 * @since 1.0.0 M1
 */
public class YtoAiImageModel implements ImageModel {

	private final static Logger logger = LoggerFactory.getLogger(YtoAiImageModel.class);

	public final RetryTemplate retryTemplate;

	private final org.springframework.ai.ytoai.YtoAiImageOptions defaultOptions;

	private final YtoAiImageApi ytoAiImageApi;

	public YtoAiImageModel(YtoAiImageApi ytoAiImageApi) {
		this(ytoAiImageApi, org.springframework.ai.ytoai.YtoAiImageOptions.builder().build(), RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	public YtoAiImageModel(YtoAiImageApi ytoAiImageApi, org.springframework.ai.ytoai.YtoAiImageOptions defaultOptions,
                           RetryTemplate retryTemplate) {
		Assert.notNull(ytoAiImageApi, "YtoAiImageApi must not be null");
		Assert.notNull(defaultOptions, "defaultOptions must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		this.ytoAiImageApi = ytoAiImageApi;
		this.defaultOptions = defaultOptions;
		this.retryTemplate = retryTemplate;
	}

	public org.springframework.ai.ytoai.YtoAiImageOptions getDefaultOptions() {
		return this.defaultOptions;
	}

	@Override
	public ImageResponse call(ImagePrompt imagePrompt) {
		return this.retryTemplate.execute(ctx -> {

			String instructions = imagePrompt.getInstructions().get(0).getText();

			YtoAiImageApi.YtoAiImageRequest imageRequest = new YtoAiImageApi.YtoAiImageRequest(instructions,
					YtoAiImageApi.DEFAULT_IMAGE_MODEL);

			if (this.defaultOptions != null) {
				imageRequest = ModelOptionsUtils.merge(this.defaultOptions, imageRequest,
						YtoAiImageApi.YtoAiImageRequest.class);
			}

			if (imagePrompt.getOptions() != null) {
				imageRequest = ModelOptionsUtils.merge(toYtoAiImageOptions(imagePrompt.getOptions()), imageRequest,
						YtoAiImageApi.YtoAiImageRequest.class);
			}

			// Make the request
			ResponseEntity<YtoAiImageApi.YtoAiImageResponse> imageResponseEntity = this.ytoAiImageApi
				.createImage(imageRequest);

			// Convert to org.springframework.ai.model derived ImageResponse data type
			return convertResponse(imageResponseEntity, imageRequest);
		});
	}

	private ImageResponse convertResponse(ResponseEntity<YtoAiImageApi.YtoAiImageResponse> imageResponseEntity,
			YtoAiImageApi.YtoAiImageRequest ytoAiImageRequest) {
		YtoAiImageApi.YtoAiImageResponse imageApiResponse = imageResponseEntity.getBody();
		if (imageApiResponse == null) {
			logger.warn("No image response returned for request: {}", ytoAiImageRequest);
			return new ImageResponse(List.of());
		}

		List<ImageGeneration> imageGenerationList = imageApiResponse.data()
			.stream()
			.map(entry -> new ImageGeneration(new Image(entry.url(), null)))
			.toList();

		return new ImageResponse(imageGenerationList);
	}

	/**
	 * Convert the {@link ImageOptions} into {@link YtoAiImageOptions}.
	 * @param runtimeImageOptions the image options to use.
	 * @return the converted {@link YtoAiImageOptions}.
	 */
	private org.springframework.ai.ytoai.YtoAiImageOptions toYtoAiImageOptions(ImageOptions runtimeImageOptions) {
		org.springframework.ai.ytoai.YtoAiImageOptions.Builder ytoAiImageOptionsBuilder = org.springframework.ai.ytoai.YtoAiImageOptions.builder();
		if (runtimeImageOptions != null) {
			if (runtimeImageOptions.getModel() != null) {
				ytoAiImageOptionsBuilder.withModel(runtimeImageOptions.getModel());
			}
			if (runtimeImageOptions instanceof org.springframework.ai.ytoai.YtoAiImageOptions runtimeYtoAiImageOptions) {
				if (runtimeYtoAiImageOptions.getUser() != null) {
					ytoAiImageOptionsBuilder.withUser(runtimeYtoAiImageOptions.getUser());
				}
			}
		}
		return ytoAiImageOptionsBuilder.build();
	}

}
