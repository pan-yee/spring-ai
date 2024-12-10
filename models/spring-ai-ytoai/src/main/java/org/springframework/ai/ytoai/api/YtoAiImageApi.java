package org.springframework.ai.ytoai.api;/*
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * ytoAI Image API.
 *
 * @see <a href= "https://open.bigmodel.cn/dev/howuse/cogview">CogView Images</a>
 * @author Geng Rong
 * @since 1.0.0 M1
 */
public class YtoAiImageApi {

	public static final String DEFAULT_IMAGE_MODEL = ImageModel.CogView_3.getValue();

	private final RestClient restClient;

	/**
	 * Create a new ytoAI Image api with base URL set to {@code https://api.ytoAI.com}.
	 * @param zhiPuAiToken ytoAI apiKey.
	 */
	public YtoAiImageApi(String zhiPuAiToken) {
		this(YtoApiConstants.DEFAULT_BASE_URL, zhiPuAiToken, RestClient.builder());
	}

	/**
	 * Create a new ytoAI Image API with the provided base URL.
	 * @param baseUrl the base URL for the ytoAI API.
	 * @param zhiPuAiToken ytoAI apiKey.
	 * @param restClientBuilder the rest client builder to use.
	 */
	public YtoAiImageApi(String baseUrl, String zhiPuAiToken, RestClient.Builder restClientBuilder) {
		this(baseUrl, zhiPuAiToken, restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	/**
	 * Create a new ytoAI Image API with the provided base URL.
	 * @param baseUrl the base URL for the ytoAI API.
	 * @param zhiPuAiToken ytoAI apiKey.
	 * @param restClientBuilder the rest client builder to use.
	 * @param responseErrorHandler the response error handler to use.
	 */
	public YtoAiImageApi(String baseUrl, String zhiPuAiToken, RestClient.Builder restClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		this.restClient = restClientBuilder.baseUrl(baseUrl).defaultHeaders(h -> h.setBearerAuth(zhiPuAiToken)
		// h.setContentType(MediaType.APPLICATION_JSON);
		).defaultStatusHandler(responseErrorHandler).build();
	}

	public ResponseEntity<YtoAiImageResponse> createImage(YtoAiImageRequest zhiPuAiImageRequest) {
		Assert.notNull(zhiPuAiImageRequest, "Image request cannot be null.");
		Assert.hasLength(zhiPuAiImageRequest.prompt(), "Prompt cannot be empty.");

		return this.restClient.post()
			.uri("/v4/images/generations")
			.body(zhiPuAiImageRequest)
			.retrieve()
			.toEntity(YtoAiImageResponse.class);
	}

	/**
	 * ytoAI Image API model.
	 * <a href="https://open.bigmodel.cn/dev/howuse/cogview">CogView</a>
	 */
	public enum ImageModel {

		CogView_3("cogview-3");

		private final String value;

		ImageModel(String model) {
			this.value = model;
		}

		public String getValue() {
			return this.value;
		}

	}

	// @formatter:off
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record YtoAiImageRequest(
		@JsonProperty("prompt") String prompt,
		@JsonProperty("model") String model,
		@JsonProperty("user_id") String user) {

		public YtoAiImageRequest(String prompt, String model) {
			this(prompt, model, null);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record YtoAiImageResponse(
		@JsonProperty("created") Long created,
		@JsonProperty("data") List<Data> data) {
	}
	// @formatter:onn

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Data(@JsonProperty("url") String url) {

	}

}