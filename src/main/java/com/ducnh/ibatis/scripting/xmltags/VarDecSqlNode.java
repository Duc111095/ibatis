package com.ducnh.ibatis.scripting.xmltags;

public class VarDecSqlNode implements SqlNode {

	private final String name;
	private final String expression;
	
	public VarDecSqlNode(String name, String exp) {
		this.name = name;
		this.expression = exp;
	}
	
	@Override
	public boolean apply(DynamicContext context) {
		final Object value = OgnlCache.getValue(expression, context.getBindings());
		context.bind(name, value);
		return true;
	}
}
