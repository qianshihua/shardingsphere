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

package org.apache.shardingsphere.readwritesplitting.spi;

import org.apache.shardingsphere.infra.algorithm.ShardingSphereAlgorithm;
import org.apache.shardingsphere.infra.context.transaction.TransactionConnectionContext;

import java.util.List;

/**
 * Read query load-balance algorithm.
 */
public interface ReadQueryLoadBalanceAlgorithm extends ShardingSphereAlgorithm {
    
    /**
     * Get data source.
     * 
     * @param name read query logic data source name
     * @param writeDataSourceName name of write data source
     * @param readDataSourceNames names of read data sources
     * @param context context
     * @return name of selected data source
     */
    String getDataSource(String name, String writeDataSourceName, List<String> readDataSourceNames, TransactionConnectionContext context);
}
