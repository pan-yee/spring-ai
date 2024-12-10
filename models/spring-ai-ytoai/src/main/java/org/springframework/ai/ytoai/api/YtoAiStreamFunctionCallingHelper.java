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

import org.springframework.ai.ytoai.api.YtoAiApi.ChatCompletion;
import org.springframework.ai.ytoai.api.YtoAiApi.ChatCompletionChunk;
import org.springframework.ai.ytoai.api.YtoAiApi.ChatCompletionFinishReason;
import org.springframework.ai.ytoai.api.YtoAiApi.ChatCompletionMessage;
import org.springframework.ai.ytoai.api.YtoAiApi.ChatCompletionMessage.ChatCompletionFunction;
import org.springframework.ai.ytoai.api.YtoAiApi.ChatCompletionMessage.*;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to support Streaming function calling. It can merge the streamed
 * ChatCompletionChunk in case of function calling message.
 *
 * @author Geng Rong
 * @since 1.0.0 M1
 */
public class YtoAiStreamFunctionCallingHelper {

	/**
	 * Merge the previous and current ChatCompletionChunk into a single one.
	 * @param previous the previous ChatCompletionChunk
	 * @param current the current ChatCompletionChunk
	 * @return the merged ChatCompletionChunk
	 */
	public YtoAiApi.ChatCompletionChunk merge(YtoAiApi.ChatCompletionChunk previous,
			YtoAiApi.ChatCompletionChunk current) {

		if (previous == null) {
			return current;
		}

		String id = (current.id() != null ? current.id() : previous.id());
		Long created = (current.created() != null ? current.created() : previous.created());
		String model = (current.model() != null ? current.model() : previous.model());
		String systemFingerprint = (current.systemFingerprint() != null ? current.systemFingerprint()
				: previous.systemFingerprint());
		String object = (current.object() != null ? current.object() : previous.object());

		YtoAiApi.ChatCompletionChunk.ChunkChoice previousChoice0 = (CollectionUtils.isEmpty(previous.choices()) ? null
				: previous.choices().get(0));
		YtoAiApi.ChatCompletionChunk.ChunkChoice currentChoice0 = (CollectionUtils.isEmpty(current.choices()) ? null
				: current.choices().get(0));

		YtoAiApi.ChatCompletionChunk.ChunkChoice choice = merge(previousChoice0, currentChoice0);
		List<YtoAiApi.ChatCompletionChunk.ChunkChoice> chunkChoices = choice == null ? List.of() : List.of(choice);
		return new YtoAiApi.ChatCompletionChunk(id, chunkChoices, created, model, systemFingerprint, object);
	}

	private YtoAiApi.ChatCompletionChunk.ChunkChoice merge(YtoAiApi.ChatCompletionChunk.ChunkChoice previous,
			YtoAiApi.ChatCompletionChunk.ChunkChoice current) {
		if (previous == null) {
			return current;
		}

		YtoAiApi.ChatCompletionFinishReason finishReason = (current.finishReason() != null ? current.finishReason()
				: previous.finishReason());
		Integer index = (current.index() != null ? current.index() : previous.index());

		YtoAiApi.ChatCompletionMessage message = merge(previous.delta(), current.delta());

		YtoAiApi.LogProbs logprobs = (current.logprobs() != null ? current.logprobs() : previous.logprobs());
		return new YtoAiApi.ChatCompletionChunk.ChunkChoice(finishReason, index, message, logprobs);
	}

	private ChatCompletionMessage merge(YtoAiApi.ChatCompletionMessage previous, ChatCompletionMessage current) {
		String content = (current.content() != null ? current.content()
				: (previous.content() != null) ? previous.content() : "");
		Role role = (current.role() != null ? current.role() : previous.role());
		role = (role != null ? role : ChatCompletionMessage.Role.ASSISTANT); // default to
																				// ASSISTANT
																				// (if
																				// null
		String name = (current.name() != null ? current.name() : previous.name());
		String toolCallId = (current.toolCallId() != null ? current.toolCallId() : previous.toolCallId());

		List<ChatCompletionMessage.ToolCall> toolCalls = new ArrayList<>();
		ToolCall lastPreviousTooCall = null;
		if (previous.toolCalls() != null) {
			lastPreviousTooCall = previous.toolCalls().get(previous.toolCalls().size() - 1);
			if (previous.toolCalls().size() > 1) {
				toolCalls.addAll(previous.toolCalls().subList(0, previous.toolCalls().size() - 1));
			}
		}
		if (current.toolCalls() != null) {
			if (current.toolCalls().size() > 1) {
				throw new IllegalStateException("Currently only one tool call is supported per message!");
			}
			var currentToolCall = current.toolCalls().iterator().next();
			if (currentToolCall.id() != null) {
				if (lastPreviousTooCall != null) {
					toolCalls.add(lastPreviousTooCall);
				}
				toolCalls.add(currentToolCall);
			}
			else {
				toolCalls.add(merge(lastPreviousTooCall, currentToolCall));
			}
		}
		else {
			if (lastPreviousTooCall != null) {
				toolCalls.add(lastPreviousTooCall);
			}
		}
		return new ChatCompletionMessage(content, role, name, toolCallId, toolCalls);
	}

	private ChatCompletionMessage.ToolCall merge(ToolCall previous, ToolCall current) {
		if (previous == null) {
			return current;
		}
		String id = (current.id() != null ? current.id() : previous.id());
		String type = (current.type() != null ? current.type() : previous.type());
		ChatCompletionMessage.ChatCompletionFunction function = merge(previous.function(), current.function());
		return new ToolCall(id, type, function);
	}

	private ChatCompletionMessage.ChatCompletionFunction merge(ChatCompletionFunction previous,
			ChatCompletionFunction current) {
		if (previous == null) {
			return current;
		}
		String name = (current.name() != null ? current.name() : previous.name());
		StringBuilder arguments = new StringBuilder();
		if (previous.arguments() != null) {
			arguments.append(previous.arguments());
		}
		if (current.arguments() != null) {
			arguments.append(current.arguments());
		}
		return new ChatCompletionMessage.ChatCompletionFunction(name, arguments.toString());
	}

	/**
	 * @param chatCompletion the ChatCompletionChunk to check
	 * @return true if the ChatCompletionChunk is a streaming tool function call.
	 */
	public boolean isStreamingToolFunctionCall(ChatCompletionChunk chatCompletion) {

		if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.choices())) {
			return false;
		}

		var choice = chatCompletion.choices().get(0);
		if (choice == null || choice.delta() == null) {
			return false;
		}
		return !CollectionUtils.isEmpty(choice.delta().toolCalls());
	}

	/**
	 * @param chatCompletion the ChatCompletionChunk to check
	 * @return true if the ChatCompletionChunk is a streaming tool function call and it is
	 * the last one.
	 */
	public boolean isStreamingToolFunctionCallFinish(ChatCompletionChunk chatCompletion) {

		if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.choices())) {
			return false;
		}

		var choice = chatCompletion.choices().get(0);
		if (choice == null || choice.delta() == null) {
			return false;
		}
		return choice.finishReason() == ChatCompletionFinishReason.TOOL_CALLS;
	}

	/**
	 * Convert the ChatCompletionChunk into a ChatCompletion. The Usage is set to null.
	 * @param chunk the ChatCompletionChunk to convert
	 * @return the ChatCompletion
	 */
	public ChatCompletion chunkToChatCompletion(ChatCompletionChunk chunk) {
		List<ChatCompletion.Choice> choices = chunk.choices()
			.stream()
			.map(chunkChoice -> new ChatCompletion.Choice(chunkChoice.finishReason(), chunkChoice.index(),
					chunkChoice.delta(), chunkChoice.logprobs()))
			.toList();

		return new ChatCompletion(chunk.id(), choices, chunk.created(), chunk.model(), chunk.systemFingerprint(),
				"chat.completion", null);
	}

}