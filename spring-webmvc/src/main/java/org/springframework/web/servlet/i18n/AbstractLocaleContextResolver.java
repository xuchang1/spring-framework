/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.i18n;

import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleContextResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Abstract base class for {@link LocaleContextResolver} implementations.
 * Provides support for a default locale and a default time zone.
 *
 * <p>Also provides pre-implemented versions of {@link #resolveLocale} and {@link #setLocale},
 * delegating to {@link #resolveLocaleContext} and {@link #setLocaleContext}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see #setDefaultLocale
 * @see #setDefaultTimeZone
 */
// 提供对默认语言环境和默认时区的支持
public abstract class AbstractLocaleContextResolver extends AbstractLocaleResolver implements LocaleContextResolver {

	@Nullable
	private TimeZone defaultTimeZone;


	/**
	 * Set a default TimeZone that this resolver will return if no other time zone found.
	 */
	public void setDefaultTimeZone(@Nullable TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * Return the default TimeZone that this resolver is supposed to fall back to, if any.
	 */
	@Nullable
	public TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}


	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		// 子类实现
		Locale locale = resolveLocaleContext(request).getLocale();
		return (locale != null ? locale : request.getLocale());
	}

	@Override
	public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
		// 子类实现
		setLocaleContext(request, response, (locale != null ? new SimpleLocaleContext(locale) : null));
	}

}
