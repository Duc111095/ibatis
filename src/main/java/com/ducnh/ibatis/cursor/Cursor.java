package com.ducnh.ibatis.cursor;

public interface Cursor<T> extends AutoCloseable, Iterable<T> {
	
	/**
	 * 
	 * @return true if the cursor has started to fetch items from database.
	 */
	boolean isOpen();
	
	/**
	 * 
	 * @return true if the cursor is fully consumed and has returned all elements matching the query.
	 */
	boolean isConsumed();

	/**
	 * Get the current item index. The first item has the index 0
	 * 
	 * @return -1 if the first cursor item has not been retrieved. The index of the current item retrieved.
	 */
	int getCurrentIndex();
	
	/**
	 * Closes the cursor.
	 */
	@Override
	void close();
}
