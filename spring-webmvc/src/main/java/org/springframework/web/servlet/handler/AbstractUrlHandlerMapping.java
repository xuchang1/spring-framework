/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Abstract base class for URL-mapped {@link HandlerMapping} implementations.
 *
 * <p>Supports literal matches and pattern matches such as "/test/*", "/test/**",
 * and others. For details on pattern syntax refer to {@link PathPattern} when
 * parsed patterns are {@link #usesPathPatterns() enabled} or see
 * {@link AntPathMatcher} otherwise. The syntax is largely the same but the
 * {@code PathPattern} syntax is more tailored for web applications, and its
 * implementation is more efficient.
 *
 * <p>All path patterns are checked in order to find the most exact match for the
 * current request path where the "most exact" is the longest path pattern that
 * matches the current request path.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 16.04.2003
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping implements MatchableHandlerMapping {

	/**
	 * 根路径的处理器
	 */
	@Nullable
	private Object rootHandler;

	/**
	 * 使用后置的 / 匹配
	 */
	private boolean useTrailingSlashMatch = false;

	/**
	 * 是否延迟加载处理器
	 */
	private boolean lazyInitHandlers = false;

	/**
	 * 路径和处理器的映射
	 */
	private final Map<String, Object> handlerMap = new LinkedHashMap<>();

	/**
	 * 路径模式->handler的映射
	 */
	private final Map<PathPattern, Object> pathPatternHandlerMap = new LinkedHashMap<>();


	@Override
	public void setPatternParser(PathPatternParser patternParser) {
		Assert.state(this.handlerMap.isEmpty(),
				"PathPatternParser must be set before the initialization of " +
						"the handler map via ApplicationContextAware#setApplicationContext.");
		super.setPatternParser(patternParser);
	}

	/**
	 * Set the root handler for this handler mapping, that is,
	 * the handler to be registered for the root path ("/").
	 * <p>Default is {@code null}, indicating no root handler.
	 */
	public void setRootHandler(@Nullable Object rootHandler) {
		this.rootHandler = rootHandler;
	}

	/**
	 * Return the root handler for this handler mapping (registered for "/"),
	 * or {@code null} if none.
	 */
	@Nullable
	public Object getRootHandler() {
		return this.rootHandler;
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 * If enabled a URL pattern such as "/users" also matches to "/users/".
	 * <p>The default value is {@code false}.
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (getPatternParser() != null) {
			getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
		}
	}

	/**
	 * Whether to match to URLs irrespective of the presence of a trailing slash.
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * Set whether to lazily initialize handlers. Only applicable to
	 * singleton handlers, as prototypes are always lazily initialized.
	 * Default is "false", as eager initialization allows for more efficiency
	 * through referencing the controller objects directly.
	 * <p>If you want to allow your controllers to be lazily initialized,
	 * make them "lazy-init" and set this flag to true. Just making them
	 * "lazy-init" will not work, as they are initialized through the
	 * references from the handler mapping in this case.
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * Look up a handler for the URL path of the given request.
	 * @param request current HTTP request
	 * @return the handler instance, or {@code null} if none found
	 */
	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		// <1> 获得请求的路径
		String lookupPath = initLookupPath(request);
		Object handler;
		// 使用了路径匹配模式
		if (usesPathPatterns()) {
			RequestPath path = ServletRequestPathUtils.getParsedRequestPath(request);
			// 查找handler并构建HandlerExecutionChain返回
			handler = lookupHandler(path, lookupPath, request);
		}
		else {
			handler = lookupHandler(lookupPath, request);
		}

		// 未找到，则进行根路径及默认路径逻辑判断
		if (handler == null) {
			// We need to care for the default handler directly, since we need to
			// expose the PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE for it as well.
			Object rawHandler = null;
			// 根路径处理
			if (StringUtils.matchesCharacter(lookupPath, '/')) {
				rawHandler = getRootHandler();
			}
			// 使用默认的
			if (rawHandler == null) {
				rawHandler = getDefaultHandler();
			}
			if (rawHandler != null) {
				// Bean name or resolved handler?
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					rawHandler = obtainApplicationContext().getBean(handlerName);
				}
				validateHandler(rawHandler, request);
				// 构建HandlerExecutionChain
				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		return handler;
	}

	/**
	 * Look up a handler instance for the given URL path. This method is used
	 * when parsed {@code PathPattern}s are {@link #usesPathPatterns() enabled}.
	 * @param path the parsed RequestPath
	 * @param lookupPath the String lookupPath for checking direct hits
	 * @param request current HTTP request
	 * @return a matching handler, or {@code null} if not found
	 * @since 5.3
	 */
	@Nullable
	protected Object lookupHandler(
			RequestPath path, String lookupPath, HttpServletRequest request) throws Exception {

		// 直接从缓存中获取并构建 HandlerExecutionChain
		Object handler = getDirectMatch(lookupPath, request);
		if (handler != null) {
			return handler;
		}

		// Pattern match?
		List<PathPattern> matches = null;
		// pathPatternHandlerMap中的key进行模式匹配，match的数据添加到集合中
		for (PathPattern pattern : this.pathPatternHandlerMap.keySet()) {
			if (pattern.matches(path.pathWithinApplication())) {
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}
		if (matches == null) {
			return null;
		}
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
			if (logger.isTraceEnabled()) {
				logger.trace("Matching patterns " + matches);
			}
		}
		PathPattern pattern = matches.get(0);
		// 获取排序后第一个pattern对应的handler
		handler = this.pathPatternHandlerMap.get(pattern);
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}
		validateHandler(handler, request);
		PathContainer pathWithinMapping = pattern.extractPathWithinPattern(path.pathWithinApplication());
		// 构建HandlerExecutionChain并返回
		return buildPathExposingHandler(handler, pattern.getPatternString(), pathWithinMapping.value(), null);
	}

	/**
	 * Look up a handler instance for the given URL path. This method is used
	 * when String pattern matching with {@code PathMatcher} is in use.
	 * @param lookupPath the path to match patterns against
	 * @param request current HTTP request
	 * @return a matching handler, or {@code null} if not found
	 * @see #exposePathWithinMapping
	 * @see AntPathMatcher
	 */
	@Nullable
	protected Object lookupHandler(String lookupPath, HttpServletRequest request) throws Exception {
		// 直接根据路径查询
		Object handler = getDirectMatch(lookupPath, request);
		if (handler != null) {
			return handler;
		}

		// Pattern match?
		List<String> matchingPatterns = new ArrayList<>();
		// 缓存的路径与当前路径进行匹配
		for (String registeredPattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(registeredPattern, lookupPath)) {
				matchingPatterns.add(registeredPattern);
			}
			// 使用后置的 / 判断
			else if (useTrailingSlashMatch()) {
				// 当前路径不是以后置的/结尾，添加后进行匹配
				if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", lookupPath)) {
					matchingPatterns.add(registeredPattern + "/");
				}
			}
		}

		String bestMatch = null;
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
		// 排序后获取第一个
		if (!matchingPatterns.isEmpty()) {
			matchingPatterns.sort(patternComparator);
			if (logger.isTraceEnabled() && matchingPatterns.size() > 1) {
				logger.trace("Matching patterns " + matchingPatterns);
			}
			bestMatch = matchingPatterns.get(0);
		}

		// 根据路径，获取handler
		if (bestMatch != null) {
			handler = this.handlerMap.get(bestMatch);
			if (handler == null) {
				if (bestMatch.endsWith("/")) {
					handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
				}
				if (handler == null) {
					throw new IllegalStateException(
							"Could not find handler for best pattern match [" + bestMatch + "]");
				}
			}
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = obtainApplicationContext().getBean(handlerName);
			}
			validateHandler(handler, request);
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, lookupPath);

			// There might be multiple 'best patterns', let's make sure we have the correct URI template variables
			// for all of them
			// 获得路径参数集合
			Map<String, String> uriTemplateVariables = new LinkedHashMap<>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, lookupPath);
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isTraceEnabled() && uriTemplateVariables.size() > 0) {
				logger.trace("URI variables " + uriTemplateVariables);
			}
			return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
		}

		// No handler found...
		return null;
	}

	@Nullable
	private Object getDirectMatch(String urlPath, HttpServletRequest request) throws Exception {
		// 从缓存中，根据路径直接查找
		Object handler = this.handlerMap.get(urlPath);
		if (handler != null) {
			// Bean name or resolved handler?
			if (handler instanceof String) {
				String handlerName = (String) handler;
				handler = obtainApplicationContext().getBean(handlerName);
			}
			// 未做任何处理
			validateHandler(handler, request);
			// 基于handler构建HandlerExecutionChain，添加了拦截器PathExposingHandlerInterceptor
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}
		return null;
	}

	/**
	 * Validate the given handler against the current request.
	 * <p>The default implementation is empty. Can be overridden in subclasses,
	 * for example to enforce specific preconditions expressed in URL mappings.
	 * @param handler the handler object to validate
	 * @param request current HTTP request
	 * @throws Exception if validation failed
	 */
	protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
	}

	/**
	 * Build a handler object for the given raw handler, exposing the actual
	 * handler, the {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}, as well as
	 * the {@link #URI_TEMPLATE_VARIABLES_ATTRIBUTE} before executing the handler.
	 * <p>The default implementation builds a {@link HandlerExecutionChain}
	 * with a special interceptor that exposes the path attribute and URI
	 * template variables
	 * @param rawHandler the raw handler to expose
	 * @param pathWithinMapping the path to expose before executing the handler
	 * @param uriTemplateVariables the URI template variables, can be {@code null} if no variables found
	 * @return the final handler object
	 */
	protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
			String pathWithinMapping, @Nullable Map<String, String> uriTemplateVariables) {
		// <1> 创建 HandlerExecutionChain 对象
		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		// <2.1> 添加 PathExposingHandlerInterceptor 拦截器，到 chain 中
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		// <2.2> 添加 UriTemplateVariablesHandlerInterceptor 拦截器，到 chain 中
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}

	/**
	 * Expose the path within the current mapping as request attribute.
	 * @param pathWithinMapping the path within the current mapping
	 * @param request the request to expose the path to
	 * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	protected void exposePathWithinMapping(String bestMatchingPattern, String pathWithinMapping,
			HttpServletRequest request) {

		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
		request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
	}

	/**
	 * Expose the URI templates variables as request attribute.
	 * @param uriTemplateVariables the URI template variables
	 * @param request the request to expose the path to
	 * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	protected void exposeUriTemplateVariables(Map<String, String> uriTemplateVariables, HttpServletRequest request) {
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	@Override
	@Nullable
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.isNull(getPatternParser(), "This HandlerMapping uses PathPatterns.");
		// 获得请求路径
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		// 模式匹配，若匹配，则返回 RequestMatchResult 对象
		if (getPathMatcher().match(pattern, lookupPath)) {
			return new RequestMatchResult(pattern, lookupPath, getPathMatcher());
		}
		else if (useTrailingSlashMatch()) {
			// 后置 / 处理
			if (!pattern.endsWith("/") && getPathMatcher().match(pattern + "/", lookupPath)) {
				return new RequestMatchResult(pattern + "/", lookupPath, getPathMatcher());
			}
		}
		return null;
	}

	/**
	 * Register the specified handler for the given URL paths.
	 * @param urlPaths the URLs that the bean should be mapped to
	 * @param beanName the name of the handler bean
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * Register the specified handler for the given URL path.
	 * @param urlPath the URL the bean should be mapped to
	 * @param handler the handler instance or handler bean name String
	 * (a bean name will automatically be resolved into the corresponding handler bean)
	 * @throws BeansException if the handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	// 处理器 Handler 注册，维护路径->处理器的映射关系
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// Eagerly resolve handler if referencing singleton via name.
		// 非懒加载且是string类型（beanName），获取对应的bean
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			ApplicationContext applicationContext = obtainApplicationContext();
			if (applicationContext.isSingleton(handlerName)) {
				resolvedHandler = applicationContext.getBean(handlerName);
			}
		}

		// 已存在判断
		Object mappedHandler = this.handlerMap.get(urlPath);
		if (mappedHandler != null) {
			if (mappedHandler != resolvedHandler) {
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
						"]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
			}
		}
		else {
			// 根路径处理
			if (urlPath.equals("/")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Root mapping to " + getHandlerDescription(handler));
				}
				setRootHandler(resolvedHandler);
			}
			else if (urlPath.equals("/*")) {
				// 默认路径处理
				if (logger.isTraceEnabled()) {
					logger.trace("Default mapping to " + getHandlerDescription(handler));
				}
				setDefaultHandler(resolvedHandler);
			}
			else {
				// 映射关系缓存
				this.handlerMap.put(urlPath, resolvedHandler);
				if (getPatternParser() != null) {
					// PatternParser 解析 路径，生成 PathPattern 对象
					this.pathPatternHandlerMap.put(getPatternParser().parse(urlPath), resolvedHandler);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
				}
			}
		}
	}

	private String getHandlerDescription(Object handler) {
		return (handler instanceof String ? "'" + handler + "'" : handler.toString());
	}


	/**
	 * Return the handler mappings as a read-only Map, with the registered path
	 * or pattern as key and the handler object (or handler bean name in case of
	 * a lazy-init handler), as value.
	 * @see #getDefaultHandler()
	 */
	public final Map<String, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * Identical to {@link #getHandlerMap()} but populated when parsed patterns
	 * are {@link #usesPathPatterns() enabled}; otherwise empty.
	 * @since 5.3
	 */
	public final Map<PathPattern, Object> getPathPatternHandlerMap() {
		return (this.pathPatternHandlerMap.isEmpty() ?
				Collections.emptyMap() : Collections.unmodifiableMap(this.pathPatternHandlerMap));
	}

	/**
	 * Indicates whether this handler mapping support type-level mappings. Default to {@code false}.
	 */
	protected boolean supportsTypeLevelMappings() {
		return false;
	}


	/**
	 * Special interceptor for exposing the
	 * {@link AbstractUrlHandlerMapping#PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE} attribute.
	 * @see AbstractUrlHandlerMapping#exposePathWithinMapping
	 */
	private class PathExposingHandlerInterceptor implements HandlerInterceptor {

		/**
		 * 最佳匹配的路径
		 */
		private final String bestMatchingPattern;

		/**
		 * 被匹配的路径
		 */
		private final String pathWithinMapping;

		public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
			this.bestMatchingPattern = bestMatchingPattern;
			this.pathWithinMapping = pathWithinMapping;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			// 暴露 BEST_MATCHING_PATTERN_ATTRIBUTE、PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE 属性
			exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);

			// 设置属性
			request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
			request.setAttribute(INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
			return true;
		}

	}

	/**
	 * Special interceptor for exposing the
	 * {@link AbstractUrlHandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE} attribute.
	 * @see AbstractUrlHandlerMapping#exposePathWithinMapping
	 */
	private class UriTemplateVariablesHandlerInterceptor implements HandlerInterceptor {

		private final Map<String, String> uriTemplateVariables;

		public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
			this.uriTemplateVariables = uriTemplateVariables;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			exposeUriTemplateVariables(this.uriTemplateVariables, request);
			return true;
		}
	}

}
