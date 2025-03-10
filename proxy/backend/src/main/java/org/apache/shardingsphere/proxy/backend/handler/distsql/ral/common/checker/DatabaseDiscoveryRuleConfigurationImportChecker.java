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

package org.apache.shardingsphere.proxy.backend.handler.distsql.ral.common.checker;

import org.apache.shardingsphere.dbdiscovery.api.config.DatabaseDiscoveryRuleConfiguration;
import org.apache.shardingsphere.dbdiscovery.spi.DatabaseDiscoveryProviderAlgorithm;
import org.apache.shardingsphere.distsql.handler.exception.algorithm.InvalidAlgorithmConfigurationException;
import org.apache.shardingsphere.distsql.handler.exception.algorithm.MissingRequiredAlgorithmException;
import org.apache.shardingsphere.distsql.handler.exception.storageunit.MissingRequiredStorageUnitsException;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.util.spi.type.typed.TypedSPILoader;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * Database discovery rule configuration import checker.
 */
public final class DatabaseDiscoveryRuleConfigurationImportChecker {
    
    private static final String DB_DISCOVERY = "Database discovery";
    
    /**
     * Check database discovery rule configuration.
     *
     * @param database database
     * @param currentRuleConfig current rule configuration
     */
    public void check(final ShardingSphereDatabase database, final DatabaseDiscoveryRuleConfiguration currentRuleConfig) {
        if (null == database || null == currentRuleConfig) {
            return;
        }
        String databaseName = database.getName();
        checkDataSources(databaseName, database, currentRuleConfig);
        checkDiscoverTypeAndHeartbeat(databaseName, currentRuleConfig);
    }
    
    private void checkDataSources(final String databaseName, final ShardingSphereDatabase database, final DatabaseDiscoveryRuleConfiguration currentRuleConfig) {
        Collection<String> requiredDataSources = new LinkedHashSet<>();
        currentRuleConfig.getDataSources().forEach(each -> requiredDataSources.addAll(each.getDataSourceNames()));
        Collection<String> notExistedDataSources = database.getResourceMetaData().getNotExistedDataSources(requiredDataSources);
        ShardingSpherePreconditions.checkState(notExistedDataSources.isEmpty(), () -> new MissingRequiredStorageUnitsException(databaseName, notExistedDataSources));
    }
    
    private void checkDiscoverTypeAndHeartbeat(final String databaseName, final DatabaseDiscoveryRuleConfiguration currentRuleConfig) {
        Collection<String> invalidInput = currentRuleConfig.getDiscoveryTypes().values().stream().map(AlgorithmConfiguration::getType)
                .filter(each -> !TypedSPILoader.contains(DatabaseDiscoveryProviderAlgorithm.class, each)).collect(Collectors.toList());
        ShardingSpherePreconditions.checkState(invalidInput.isEmpty(), () -> new InvalidAlgorithmConfigurationException(DB_DISCOVERY.toLowerCase(), invalidInput));
        currentRuleConfig.getDataSources().forEach(each -> {
            if (!currentRuleConfig.getDiscoveryTypes().containsKey(each.getDiscoveryTypeName())) {
                invalidInput.add(each.getDiscoveryTypeName());
            }
            if (!currentRuleConfig.getDiscoveryHeartbeats().containsKey(each.getDiscoveryHeartbeatName())) {
                invalidInput.add(each.getDiscoveryHeartbeatName());
            }
        });
        ShardingSpherePreconditions.checkState(invalidInput.isEmpty(), () -> new MissingRequiredAlgorithmException(DB_DISCOVERY, databaseName, invalidInput));
    }
}
