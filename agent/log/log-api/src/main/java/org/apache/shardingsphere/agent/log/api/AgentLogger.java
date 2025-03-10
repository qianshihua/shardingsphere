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

package org.apache.shardingsphere.agent.log.api;

/**
 * Agent logger.
 */
public interface AgentLogger {
    
    /**
     * Info.
     *
     * @param msg message
     */
    void info(String msg);
    
    /**
     * Info.
     *
     * @param format format
     * @param arguments arguments
     */
    void info(String format, Object... arguments);
    
    /**
     * Error.
     *
     * @param format format
     * @param arguments arguments
     */
    void error(String format, Object... arguments);
    
    /**
     * Error.
     *
     * @param msg message
     */
    void error(String msg);
    
    /**
     * Debug.
     *
     * @param format format
     * @param arguments arguments
     */
    void debug(String format, Object... arguments);
    
    /**
     * Debug.
     *
     * @param msg message
     */
    void debug(String msg);
}
