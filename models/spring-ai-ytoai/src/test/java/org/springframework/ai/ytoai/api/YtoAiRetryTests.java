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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.ytoai.*;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;

/**
 * @author Geng Rong
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class YtoAiRetryTests {

	private TestRetryListener retryListener;

	private RetryTemplate retryTemplate;

	private @Mock YtoAiApi zhiPuAiApi;

	private @Mock YtoAiImageApi zhiPuAiImageApi;

	private YtoAiChatModel chatModel;

	private YtoAiEmbeddingModel embeddingModel;

	private YtoAiImageModel imageModel;

	@BeforeEach
	public void beforeEach() {
		this.retryTemplate = RetryUtils.SHORT_RETRY_TEMPLATE;
		this.retryListener = new TestRetryListener();
		this.retryTemplate.registerListener(this.retryListener);

		this.chatModel = new YtoAiChatModel(this.zhiPuAiApi, YtoAiChatOptions.builder().build(), null,
				this.retryTemplate);
		this.embeddingModel = new YtoAiEmbeddingModel(this.zhiPuAiApi, MetadataMode.EMBED,
				YtoAiEmbeddingOptions.builder().build(), this.retryTemplate);
		this.imageModel = new YtoAiImageModel(this.zhiPuAiImageApi, YtoAiImageOptions.builder().build(),
				this.retryTemplate);
	}

	@Test
	public void zhiPuAiChatTransientError() {

		var choice = new YtoAiApi.ChatCompletion.Choice(YtoAiApi.ChatCompletionFinishReason.STOP, 0,
				new YtoAiApi.ChatCompletionMessage("Response", YtoAiApi.ChatCompletionMessage.Role.ASSISTANT), null);
		YtoAiApi.ChatCompletion expectedChatCompletion = new YtoAiApi.ChatCompletion("id", List.of(choice), 666L, "model", null, null,
				new YtoAiApi.Usage(10, 10, 10));

		given(this.zhiPuAiApi.chatCompletionEntity(isA(YtoAiApi.ChatCompletionRequest.class)))
			.willThrow(new TransientAiException("Transient Error 1"))
			.willThrow(new TransientAiException("Transient Error 2"))
			.willReturn(ResponseEntity.of(Optional.of(expectedChatCompletion)));

		var result = this.chatModel.call(new Prompt("text"));

		assertThat(result).isNotNull();
		assertThat(result.getResult().getOutput().getContent()).isSameAs("Response");
		assertThat(this.retryListener.onSuccessRetryCount).isEqualTo(2);
		assertThat(this.retryListener.onErrorRetryCount).isEqualTo(2);
	}

	@Test
	public void zhiPuAiChatNonTransientError() {
		given(this.zhiPuAiApi.chatCompletionEntity(isA(YtoAiApi.ChatCompletionRequest.class)))
			.willThrow(new RuntimeException("Non Transient Error"));
		assertThrows(RuntimeException.class, () -> this.chatModel.call(new Prompt("text")));
	}

	@Test
	public void zhiPuAiChatStreamTransientError() {

		var choice = new YtoAiApi.ChatCompletionChunk.ChunkChoice(YtoAiApi.ChatCompletionFinishReason.STOP, 0,
				new YtoAiApi.ChatCompletionMessage("Response", YtoAiApi.ChatCompletionMessage.Role.ASSISTANT), null);
		YtoAiApi.ChatCompletionChunk expectedChatCompletion = new YtoAiApi.ChatCompletionChunk("id", List.of(choice), 666L, "model", null,
				null);

		given(this.zhiPuAiApi.chatCompletionStream(isA(YtoAiApi.ChatCompletionRequest.class)))
			.willThrow(new TransientAiException("Transient Error 1"))
			.willThrow(new TransientAiException("Transient Error 2"))
			.willReturn(Flux.just(expectedChatCompletion));

		var result = this.chatModel.stream(new Prompt("text"));

		assertThat(result).isNotNull();
		assertThat(result.collectList().block().get(0).getResult().getOutput().getContent()).isSameAs("Response");
		assertThat(this.retryListener.onSuccessRetryCount).isEqualTo(2);
		assertThat(this.retryListener.onErrorRetryCount).isEqualTo(2);
	}

	@Test
	public void zhiPuAiChatStreamNonTransientError() {
		given(this.zhiPuAiApi.chatCompletionStream(isA(YtoAiApi.ChatCompletionRequest.class)))
			.willThrow(new RuntimeException("Non Transient Error"));
		assertThrows(RuntimeException.class, () -> this.chatModel.stream(new Prompt("text")).collectList().block());
	}

	@Test
	public void zhiPuAiEmbeddingTransientError() {

		YtoAiApi.EmbeddingList<YtoAiApi.Embedding> expectedEmbeddings = new YtoAiApi.EmbeddingList<>("list",
				List.of(new YtoAiApi.Embedding(0, new float[] { 9.9f, 8.8f })), "model", new YtoAiApi.Usage(10, 10, 10));

		given(this.zhiPuAiApi.embeddings(isA(YtoAiApi.EmbeddingRequest.class)))
			.willThrow(new TransientAiException("Transient Error 1"))
			.willThrow(new TransientAiException("Transient Error 2"))
			.willReturn(ResponseEntity.of(Optional.of(expectedEmbeddings)));

		var result = this.embeddingModel
			.call(new org.springframework.ai.embedding.EmbeddingRequest(List.of("text1", "text2"), null));

		assertThat(result).isNotNull();
		assertThat(result.getResult().getOutput()).isEqualTo(new float[] { 9.9f, 8.8f });
		assertThat(this.retryListener.onSuccessRetryCount).isEqualTo(0);
		assertThat(this.retryListener.onErrorRetryCount).isEqualTo(2);
	}

	@Test
	public void zhiPuAiEmbeddingNonTransientError() {
		given(this.zhiPuAiApi.embeddings(isA(YtoAiApi.EmbeddingRequest.class)))
			.willThrow(new RuntimeException("Non Transient Error"));
		assertThrows(RuntimeException.class, () -> this.embeddingModel
			.call(new org.springframework.ai.embedding.EmbeddingRequest(List.of("text1", "text2"), null)));
	}

	@Test
	public void zhiPuAiImageTransientError() {

		var expectedResponse = new YtoAiImageApi.YtoAiImageResponse(678L, List.of(new YtoAiImageApi.Data("url678")));

		given(this.zhiPuAiImageApi.createImage(isA(YtoAiImageApi.YtoAiImageRequest.class)))
			.willThrow(new TransientAiException("Transient Error 1"))
			.willThrow(new TransientAiException("Transient Error 2"))
			.willReturn(ResponseEntity.of(Optional.of(expectedResponse)));

		var result = this.imageModel.call(new ImagePrompt(List.of(new ImageMessage("Image Message"))));

		assertThat(result).isNotNull();
		assertThat(result.getResult().getOutput().getUrl()).isEqualTo("url678");
		assertThat(this.retryListener.onSuccessRetryCount).isEqualTo(2);
		assertThat(this.retryListener.onErrorRetryCount).isEqualTo(2);
	}

	@Test
	public void zhiPuAiImageNonTransientError() {
		given(this.zhiPuAiImageApi.createImage(isA(YtoAiImageApi.YtoAiImageRequest.class)))
			.willThrow(new RuntimeException("Transient Error 1"));
		assertThrows(RuntimeException.class,
				() -> this.imageModel.call(new ImagePrompt(List.of(new ImageMessage("Image Message")))));
	}

	private class TestRetryListener implements RetryListener {

		int onErrorRetryCount = 0;

		int onSuccessRetryCount = 0;

		@Override
		public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
			this.onSuccessRetryCount = context.getRetryCount();
		}

		@Override
		public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {
			this.onErrorRetryCount = context.getRetryCount();
		}

	}

}
