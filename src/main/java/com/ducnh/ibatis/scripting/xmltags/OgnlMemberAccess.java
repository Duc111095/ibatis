package com.ducnh.ibatis.scripting.xmltags;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import com.ducnh.ibatis.reflection.Reflector;

import ognl.MemberAccess;
import ognl.OgnlContext;

public class OgnlMemberAccess  implements MemberAccess{

	private final boolean canControlMemberAccessible;
	 
	OgnlMemberAccess() {
		this.canControlMemberAccessible = Reflector.canControlMemberAccessible();
	}
	
	@Override
	public Object setup(OgnlContext context, Object target, Member member, String propertyName) {
		Object result = null;
		if (isAccessible(context, target, member, propertyName)) {
			AccessibleObject accessible = (AccessibleObject) member;
			if (!accessible.isAccessible()) {
				result = Boolean.FALSE;
				accessible.setAccessible(true);
			}
		}
		return result;
	}
	
	@Override
	public void restore(OgnlContext context, Object target, Member member, String propertyName, Object state) {
		
	}
	
	@Override
	public boolean isAccessible(OgnlContext context, Object target, Member member, String propertyName) {
		return canControlMemberAccessible;
	}
}
