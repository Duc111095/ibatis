package com.ducnh.ibatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import com.ducnh.ibatis.session.Configuration;

public class WhereSqlNode extends TrimSqlNode {

	private static final List<String> prefixList = Arrays.asList("AND ", "OR ", "AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");
	
	public WhereSqlNode(Configuration configuration, SqlNode contents) {
		super(configuration, contents, "WHERE", prefixList, null, null);
	}
}
