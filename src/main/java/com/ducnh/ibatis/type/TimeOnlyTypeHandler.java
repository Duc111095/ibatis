package com.ducnh.ibatis.type;

import java.util.Date;

public class TimeOnlyTypeHandler extends BaseTypeHandler<Date>{

	public static final TimeOnlyTypeHandler INSTANCE = new TimeOnlyTypeHandler();
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType )
}
