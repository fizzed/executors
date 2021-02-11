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

import com.fizzed.executors.impl.ProcessorWorker;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class ProcessorService<T,P extends Processor<T>> extends WorkerService<ProcessorWorker<T>> {

    private final BlockingQueue<T> taskQueue;
    
    public ProcessorService(
            String name) {
        
        super(name);
        
        this.taskQueue = new LinkedBlockingQueue<>();
    }

    abstract protected P newProcessor();

    @Override
    protected ProcessorWorker<T> newWorker() {
        final Processor processor = this.newProcessor();
        
        return new ProcessorWorker<>(this.taskQueue, processor);
    }
    
    
    
    public void submit(T task) {
        // TODO: should we reject any new tasks AFTER being stopped?
        
        // push the task onto the tail of the queue
        this.taskQueue.offer(task);
    }
    
}