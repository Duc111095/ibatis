package com.ducnh.ibatis.scripting.xmltags;

import com.ducnh.ibatis.parsing.GenericTokenParser;
import com.ducnh.ibatis.parsing.TokenHandler;
import com.ducnh.ibatis.type.SimpleTypeRegistry;

public class TextSqlNode implements SqlNode {

	private final String text;
	public TextSqlNode(String text) {
		this.text = text;
	}
	
	public boolean isDynamic() {
		DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
		GenericTokenParser parser = createParser(checker);
		parser.parse(text);
		return checker.isDynamic();
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		GenericTokenParser parser = createParser(new BindingTokenParser(context));
		context.appendSql(context.parseParam(parser.parse(text)));
		return true;
	}
	
	private GenericTokenParser createParser(TokenHandler handler) {
		return new GenericTokenParser("${", "}", handler);
	}
	
	private static class BindingTokenParser implements TokenHandler {
		private final DynamicContext context;
		
		public BindingTokenParser(DynamicContext context) {
			this.context = context;
		}
		
		@Override
		public String handleToken(String content) {
			Object parameter = context.getBindings().get("_parameter");
			if (parameter == null) {
				context.getBindings().put("value", null);
			} else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
				context.getBindings().put("value", parameter);
			}
			Object value = OgnlCache.getValue(content, context.getBindings());
			return value == null ? "" : String.valueOf(value);
		}
	}
	
	private static class DynamicCheckerTokenParser implements TokenHandler {
		private boolean isDynamic;
		
		public DynamicCheckerTokenParser() {
		}
		
		public boolean isDynamic() {
			return isDynamic;
		}
		
		@Override
		public String handleToken(String content) {
			this.isDynamic = true;
			return null;
		}
	}
}
