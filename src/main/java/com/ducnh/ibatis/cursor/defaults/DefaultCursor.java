package com.ducnh.ibatis.cursor.defaults;

import java.util.Iterator;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.session.RowBounds;

public class DefaultCursor<T> implements Cursor<T> {
	
	private final DefaultResultSetHandler resultSetHandler;
	private final ResultMap resultMap;
	private final ResultSetWrapper rsw;
	private final RowBounds rowBounds;
	protected final ObjectWrapperResultHandler<T> objectWrapperResultHandler = new ObjectWrapperResultHandler<>();
	
	private final CursorIterator cursorIterator = new CursorIterator();
	private boolean iteratorRetrieved;
	
	private CursorStatus status = CursorStatus.CREATED;
	private int indexWithRowBound = -1;
	
	private enum CursorStatus {
		CREATED,
		OPEN,
		CLOSED,
		CONSUMED
	}
	
	public DefaultCursor(DefaultResultSetHandler resultHandler, ResultMap resultMap, ResultSetWrapper rsw,
		RowBounds rowBounds) {
		this.resultSetHandler = resultSetHandler;
		this.resultMap = resultMap;
		this.rsw = rsw;
		this.rowBounds = rowBounds;
	}
	
	@Override
	public boolean isOpen() {
		return status == CursorStatus.OPEN;
	}
	
	@Override
	public boolean isConsumed() {
		return status == CursorStatus.CONSUMED;
	}

	@Override
	public int getCurrentIndex() {
		return rowBounds.getOffset() + cursorIterator.iteratorIndex;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
