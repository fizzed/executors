/*
 * Copyright 2019 Fizzed, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fizzed.executors.core;

public interface Service {
 
    ServiceState getState();
    
    default boolean isStarted() {
        return this.getState() == ServiceState.STARTED;
    }
    
    default boolean isStarting() {
        return this.getState() == ServiceState.STARTING;
    }
    
    default boolean isStopped() {
        return this.getState() == ServiceState.STOPPED;
    }
    
    default boolean isStopping() {
        return this.getState() == ServiceState.STOPPING;
    }
    
    void start();
    
    void stop();
    
}