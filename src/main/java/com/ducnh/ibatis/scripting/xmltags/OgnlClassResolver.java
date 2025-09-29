package com.ducnh.ibatis.scripting.xmltags;

import com.ducnh.ibatis.io.Resources;

import ognl.DefaultClassResolver;

public class OgnlClassResolver extends DefaultClassResolver{

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class toClassForName(String className) throws ClassNotFoundException {
		return Resources.classForName(className);
	}
}
