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

import com.fizzed.crux.util.TimeDuration;

public interface WorkerContext {
 
    String getName();
    
    WorkerState getState();

    boolean isStopRequested();
    
    //void stop();
    
    default void idle() throws ExecuteStopException, InterruptedException {
        this.idle((TimeDuration)null, (String)null);
    }
    
    default void idle(TimeDuration duration) throws ExecuteStopException, InterruptedException {
        this.idle(duration, (String)null);
    }
    
    void idle(TimeDuration duration, String message) throws ExecuteStopException, InterruptedException;
    
    default void running() throws ExecuteStopException {
        this.running("Execute");
    }
    
    void running(String message) throws ExecuteStopException;
    
    default void running(ExecuteRunnable runnable) throws ExecuteStopException, InterruptedException {
        this.running((String)null, runnable);
    }
    
    default void running(String message, ExecuteRunnable runnable) throws ExecuteStopException, InterruptedException {
        this.running(message);
        try {
            runnable.run();
        } finally {
            this.idle();
        }
    }
    
//    void stop() throws ExecuteStopException;
    
}