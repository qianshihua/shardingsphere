/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.dbdiscovery.mysql.type;

import lombok.Getter;
import org.apache.shardingsphere.dbdiscovery.mysql.exception.replica.DuplicatePrimaryDataSourceException;
import org.apache.shardingsphere.dbdiscovery.spi.DatabaseDiscoveryProviderAlgorithm;
import org.apache.shardingsphere.dbdiscovery.spi.ReplicaDataSourceStatus;
import org.apache.shardingsphere.infra.executor.kernel.ExecutorEngine;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.util.exception.external.sql.type.wrapper.SQLWrapperException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Normal replication database discovery provider algorithm for MySQL.
 */
public final class MySQLNormalReplicationDatabaseDiscoveryProviderAlgorithm implements DatabaseDiscoveryProviderAlgorithm {
    
    private static final String SHOW_SLAVE_STATUS = "SHOW SLAVE STATUS";
    
    private static final String SHOW_SLAVE_HOSTS = "SHOW SLAVE HOSTS";
    
    private static final String SHOW_VARIABLES_READ_ONLY = "SHOW VARIABLES LIKE 'read_only'";
    
    @Getter
    private Properties props;
    
    private long delayMillisecondsThreshold;
    
    @Override
    public void init(final Properties props) {
        this.props = props;
        delayMillisecondsThreshold = Long.parseLong(props.getProperty("delay-milliseconds-threshold", "0"));
    }
    
    @Override
    public void checkEnvironment(final String databaseName, final Collection<DataSource> dataSources) {
        ExecutorService executorService = ExecutorEngine.createExecutorEngineWithCPUAndResources(dataSources.size()).getExecutorServiceManager().getExecutorService();
        checkPrimaryDataSource(databaseName, dataSources.stream().map(each -> asyncCheckEnvironment(executorService, each)).collect(Collectors.toList()));
    }
    
    private void checkPrimaryDataSource(final String databaseName, final Collection<CompletableFuture<Boolean>> completableFutures) {
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
        Iterator<CompletableFuture<Boolean>> primaryInstancesFuture = completableFutures.stream().iterator();
        int primaryInstanceCount = 0;
        while (primaryInstancesFuture.hasNext()) {
            if (primaryInstancesFuture.next().join()) {
                primaryInstanceCount++;
            }
        }
        ShardingSpherePreconditions.checkState(1 == primaryInstanceCount, () -> new DuplicatePrimaryDataSourceException(databaseName));
    }
    
    private CompletableFuture<Boolean> asyncCheckEnvironment(final ExecutorService executorService, final DataSource dataSource) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return isPrimaryInstance(dataSource);
            } catch (final SQLException ex) {
                throw new SQLWrapperException(ex);
            }
        }, executorService);
    }
    
    @Override
    public boolean isPrimaryInstance(final DataSource dataSource) throws SQLException {
        return !getReplicationInstances(dataSource).isEmpty() && isNotReadonlyInstance(dataSource);
    }
    
    private Collection<String> getReplicationInstances(final DataSource dataSource) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            return getReplicationInstances(statement);
        }
    }
    
    private Collection<String> getReplicationInstances(final Statement statement) throws SQLException {
        Collection<String> result = new LinkedList<>();
        try (ResultSet resultSet = statement.executeQuery(SHOW_SLAVE_HOSTS)) {
            while (resultSet.next()) {
                result.add(String.join(":", resultSet.getString("HOST"), resultSet.getString("PORT")));
            }
        }
        return result;
    }
    
    private boolean isNotReadonlyInstance(final DataSource dataSource) throws SQLException {
        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(SHOW_VARIABLES_READ_ONLY)) {
            return resultSet.next() && resultSet.getString("Value").equals("OFF");
        }
    }
    
    @Override
    public ReplicaDataSourceStatus loadReplicaStatus(final DataSource replicaDataSource) throws SQLException {
        try (
                Connection connection = replicaDataSource.getConnection();
                Statement statement = connection.createStatement()) {
            if (0L == delayMillisecondsThreshold) {
                return new ReplicaDataSourceStatus(true, 0L);
            }
            long replicationDelayMilliseconds = queryReplicationDelayMilliseconds(statement);
            boolean isDelay = replicationDelayMilliseconds >= delayMillisecondsThreshold;
            return new ReplicaDataSourceStatus(!isDelay, replicationDelayMilliseconds);
        }
    }
    
    private long queryReplicationDelayMilliseconds(final Statement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(SHOW_SLAVE_STATUS)) {
            if (resultSet.next()) {
                long delay = resultSet.getLong("Seconds_Behind_Master") * 1000;
                return resultSet.wasNull() ? Long.MAX_VALUE : delay;
            }
            return Long.MAX_VALUE;
        }
    }
    
    @Override
    public String getType() {
        return "MySQL.NORMAL_REPLICATION";
    }
}
