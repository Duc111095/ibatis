package com.ducnh.ibatis.datasource.pooled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class PoolState {
	
	private final ReentrantLock lock = new ReentrantLock();
	
	protected PooledDataSource dataSource;
	
	protected final List<PooledConnection> idleConnections = new ArrayList<>();
	protected final List<PooledConnection> activeConnections = new ArrayList<>();
	protected long requestCount;
	protected long accumulatedRequestTime;
	protected long accumulatedCheckoutTime;
	protected long claimedOverdueConnectionCount;
	protected long accumulatedCheckoutTimeOfOverdueConnections;
	protected long accumulatedWaitTime;
	protected long hadToWaitCount;
	protected long badConnectionCount;
	
	public PoolState(PooledDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public long getRequestCount() {
		lock.lock();
		try {
			return requestCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getAverageRequestTime() {
		lock.lock();
		try {
			return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getAverageWaitTime() {
		lock.lock();
		try {
			return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getHadToWaitCount() {
		lock.lock();
		try {
			return hadToWaitCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getBadConnectionCount() {
		lock.lock();
		try {
			return badConnectionCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getClaimedOverdueConnectionCount() {
		lock.lock();
		try {
			return claimedOverdueConnectionCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getAverageOverdueCheckoutTime() {
		lock.unlock();
		try {
			return claimedOverdueConnectionCount == 0 ? 0 : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
		} finally {
			lock.unlock();
		}
	}
	
	public long getAverageCheckoutTime() {
		lock.lock();
		try {
			return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
		} finally {
			lock.unlock();
		}
	}
	
	public int getIdleConnectionCount() {
		lock.lock();
		try {
			return idleConnections.size();
		} finally {
			lock.unlock();
		}
	}
	
	public int getActiveConnectionCount() {
		lock.lock();
		try {
			return activeConnections.size();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public String toString() {
		lock.lock();
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("\n===CONFIGURATION============================");
			builder.append("\b jbdcDriver               ").append(dataSource.getDriver());
			builder.append("\n jdbcUrl                  ").append(dataSource.getUrl());
			builder.append("\n jdbcUsername                   ").append(dataSource.getUsername());
			builder.append("\n jdbcPassword                   ")
				.append(dataSource.getPassword() == null ? "NULL" : "************");
			builder.append("\n poolMaxActiveConnections       ").append(dataSource.poolMaximumActiveConnections);
			builder.append("\n poolMaxIdleConnections         ").append(dataSource.poolMaximumIdleConnections);
			builder.append("\n poolMaxCheckoutTime            ").append(dataSource.poolMaximumCheckoutTime);
			builder.append("\n poolTimeToWait                 ").append(dataSource.poolTimeToWait);
			builder.append("\n poolPingEnabled                ").append(dataSource.poolPingEnabled);
			builder.append("\n poolPingQuery                  ").append(dataSource.poolPingQuery);
			builder.append("\n poolPingConnectionsNotUsedFor  ").append(dataSource.poolPingConnectionsNotUsedFor);
			builder.append("\n ---STATUS-----------------------------------------------------");
			builder.append("\n activeConnections              ").append(getActiveConnectionCount());
			builder.append("\n idleConnections                ").append(getIdleConnectionCount());
			builder.append("\n requestCount                   ").append(getRequestCount());
			builder.append("\n averageRequestTime             ").append(getAverageRequestTime());
			builder.append("\n averageCheckoutTime            ").append(getAverageCheckoutTime());
			builder.append("\n claimedOverdue                 ").append(getClaimedOverdueConnectionCount());
			builder.append("\n averageOverdueCheckoutTime     ").append(getAverageOverdueCheckoutTime());
			builder.append("\n hadToWait                      ").append(getHadToWaitCount());
			builder.append("\n averageWaitTime                ").append(getAverageWaitTime());
			builder.append("\n badConnectionCount             ").append(getBadConnectionCount());
			builder.append("\n===============================================================");
			return builder.toString();
		} finally {
			lock.unlock();
		}
	}
}

