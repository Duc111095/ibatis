package com.ducnh.ibatis.scripting.xmltags;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.session.Configuration;

public class ForEachSqlNode implements SqlNode {
	
	private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;
	private final String collectionExpression;
	private final Boolean nullable;
	private final SqlNode contents;
	private final String open;
	private final String close;
	private final String separator;
	private final String item;
	private final String index;
	private final Configuration configuration;

	public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index,
		String item, String open, String close, String separator) {
		this(configuration, contents, collectionExpression, null, index, item, open, close, separator);
	}
	
	public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, Boolean nullable,
		String index, String item, String open, String close, String separator) {
		this.collectionExpression = collectionExpression;
		this.nullable = nullable;
		this.contents = contents;
		this.open = open;
		this.close = close;
		this.separator = separator;
		this.index = index;
		this.item = item;
		this.configuration = configuration;
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		Map<String, Object> bindings = context.getBindings();
		final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings,
			Optional.ofNullable(nullable).orElseGet(configuration::isNullableOnForEach));
		if (iterable == null || !iterable.iterator().hasNext()) {
			return true;
		}
		boolean first = true;
		applyOpen(context);
		int i = 0;
		for (Object o : iterable) {
			DynamicContext scopedContext;
			if (first || separator == null) {
				scopedContext = new PrefixedContext(context, "");
			} else {
				scopedContext = new PrefixedContext(context, separator);
			}
			if (o instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
				applyIndex(scopedContext, mapEntry.getKey());
				applyItem(scopedContext, mapEntry.getValue());
			} else {
				applyIndex(scopedContext, i);
				applyItem(scopedContext, o);
			}
			contents.apply(scopedContext);
			if (first) {
				first = !((PrefixedContext) scopedContext).isPrefixApplied();
			}
			i++;
		}
		applyClose(context);
		return true;
	}
	
	private void applyIndex(DynamicContext context, Object o) {
		if (index != null) {
			context.bind(index, o);
		}
	}
	
	private void applyItem(DynamicContext context, Object o) {
		if (item != null) {
			context.bind(item, o);
		}
	}
	
	private void applyOpen(DynamicContext context) {
		if (open != null) {
			context.appendSql(open);
		}
	}
	
	private void applyClose(DynamicContext context) {
		if (close != null) {
			context.appendSql(close);
		}
	}
	
	private class PrefixedContext extends DynamicContext {
		private final DynamicContext delegate;
		private final String prefix;
		private boolean prefixApplied;
		
		public PrefixedContext(DynamicContext delegate, String prefix) {
			super(configuration, delegate.getParameterObject(), delegate.getParameterType(), delegate.getParamNameResolver(),
				delegate.isParamExists());
			this.delegate = delegate;
			this.prefix = prefix;
			this.prefixApplied = false;
			this.bindings.putAll(delegate.getBindings());
		}
		
		public boolean isPrefixApplied() {
			return prefixApplied;
		}
		
		@Override
		public void appendSql(String sql) {
			if (!prefixApplied && sql != null && sql.trim().length() > 0) {
				delegate.appendSql(prefix);
				prefixApplied = true;
			}
			delegate.appendSql(sql);
		}
		
		@Override
		public String getSql() {
			return delegate.getSql();
		}
		
		@Override
		public List<ParameterMapping> getParameterMappings() {
			return delegate.getParameterMappings();
		}
	}
}
