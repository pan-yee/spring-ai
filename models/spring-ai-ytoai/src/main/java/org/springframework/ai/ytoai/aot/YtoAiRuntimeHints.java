package org.springframework.ai.ytoai.aot;/*
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

import org.springframework.ai.ytoai.api.YtoAiApi;
import org.springframework.ai.ytoai.api.YtoAiImageApi;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.springframework.ai.aot.AiRuntimeHints.findJsonAnnotatedClassesInPackage;

/**
 * The YtoAiRuntimeHints class is responsible for registering runtime hints for yto AI API
 * classes.
 *
 * @author Geng Rong
 * @since 1.0.0 M1
 */
public class YtoAiRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
		var mcs = MemberCategory.values();
		for (var tr : findJsonAnnotatedClassesInPackage(YtoAiApi.class)) {
			hints.reflection().registerType(tr, mcs);
		}
		for (var tr : findJsonAnnotatedClassesInPackage(YtoAiImageApi.class)) {
			hints.reflection().registerType(tr, mcs);
		}
	}

}
