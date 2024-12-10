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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.embedding.EmbeddingOptions;

/**
 * The ytoAiEmbeddingOptions class represents the options for ytoAI embedding.
 *
 * @author Geng Rong
 * @author Thomas Vitale
 * @since 1.0.0 M1
 */
@JsonInclude(Include.NON_NULL)
public class YtoAiEmbeddingOptions implements EmbeddingOptions {

	/**
	 * The default vector dimension is 2048. The model supports custom vector dimensions,
	 * and it is recommended to choose dimensions of 256, 512, 1024, or 2048.
	 */
	private @JsonProperty("dimensions") Integer dimensions;

	// @formatter:off
	/**
	 * ID of the model to use.
	 */
	private @JsonProperty("model") String model;
	// @formatter:on

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Integer getDimensions() {
		return this.dimensions;
	}

	public void setDimensions(Integer dimensions) {
		this.dimensions = dimensions;
	}

	public static class Builder {

		protected YtoAiEmbeddingOptions options;

		public Builder() {
			this.options = new YtoAiEmbeddingOptions();
		}

		public Builder withModel(String model) {
			this.options.setModel(model);
			return this;
		}

		public Builder withDimensions(Integer dimensions) {
			this.options.setDimensions(dimensions);
			return this;
		}

		public YtoAiEmbeddingOptions build() {
			return this.options;
		}

	}

}