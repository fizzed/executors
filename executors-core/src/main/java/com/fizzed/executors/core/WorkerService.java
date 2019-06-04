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

import com.fizzed.executors.internal.ExecuteHelper;
import com.fizzed.crux.util.StopWatch;
import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.impl.WorkerRunnableImpl;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WorkerService<W extends Worker> implements Service {
    
    protected Logger log;
    protected final AtomicLong workerIds;
    private final CopyOnWriteArrayList<WorkerRunnable<W>> runnables;
    protected AtomicReference<ServiceState> stateRef;
    protected ScheduledThreadPoolExecutor executors;
    private String name;
    private int minPoolSize;
    private TimeDuration shutdownTimeout;
    private TimeDuration initialDelay;
    private Double initialDelayStagger;
    private TimeDuration executeDelay;
    private TimeDuration unhandledThrowableDelay;
    
    public WorkerService(
            String name) {
        
        this.workerIds = new AtomicLong();
        this.stateRef = new AtomicReference<>(ServiceState.STOPPED);
        this.name = name;
        this.minPoolSize = 1;
        this.log = LoggerFactory.getLogger(this.getClass());
        this.shutdownTimeout = new TimeDuration(60, TimeUnit.SECONDS);
        this.runnables = new CopyOnWriteArrayList<>();
        this.initialDelay = null;
        this.initialDelayStagger = null;
        this.executeDelay = null;
        this.unhandledThrowableDelay = TimeDuration.seconds(5);
    }

    @Override
    public ServiceState getState() {
        return this.stateRef.get();
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public TimeDuration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(TimeDuration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public TimeDuration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(TimeDuration initialDelay) {
        this.initialDelay = initialDelay;
    }

    public Double getInitialDelayStagger() {
        return initialDelayStagger;
    }

    public void setInitialDelayStagger(Double initialDelayStagger) {
        this.initialDelayStagger = initialDelayStagger;
    }

    public TimeDuration getExecuteDelay() {
        return executeDelay;
    }

    public void setExecuteDelay(TimeDuration executeDelay) {
        this.executeDelay = executeDelay;
    }

    public TimeDuration getUnhandledThrowableDelay() {
        return unhandledThrowableDelay;
    }

    public void setUnhandledThrowableDelay(TimeDuration unhandledThrowableDelay) {
        this.unhandledThrowableDelay = unhandledThrowableDelay;
    }

    public List<WorkerRunnable<W>> getRunnables() {
        return this.runnables;
    }
    
    abstract public W newWorker(long workerId, String workerName);
    
    protected WorkerRunnableImpl<W> buildWorkerRunnable() {
        final long workerId = this.workerIds.incrementAndGet();
        
        final String workerName = this.name + "-" + workerId;
        
        final W worker = this.newWorker(workerId, workerName);
        
        // build runnable that handles it
        final WorkerRunnableImpl<W> runnable = new WorkerRunnableImpl<>(
            workerId, workerName, worker);

        final TimeDuration _initialDelay = this.initialDelayStagger != null ?
            ExecuteHelper.staggered(this.getInitialDelay(), this.initialDelayStagger)
                : this.getInitialDelay();
        
        runnable.setInitialDelay(_initialDelay);
        runnable.setExecuteDelay(this.getExecuteDelay());
        runnable.setUnhandledThrowableDelay(this.getUnhandledThrowableDelay());
        
        return runnable;
    }
    
    @Override
    public void start() {
        if (!this.stateRef.compareAndSet(ServiceState.STOPPED, ServiceState.STARTING)) {
            throw new IllegalStateException(this.name + ": service not currently stopped");
        }
        
        final StopWatch timer = StopWatch.timeMillis();
        try {
            log.info("{}: service starting...", this.name);

            // clear out any previous runnables
            this.runnables.clear();
            
            this.executors = new ScheduledThreadPoolExecutor(
                this.minPoolSize, (Runnable runnable) -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    return thread;
                });
            
            for (int i = 0; i < this.minPoolSize; i++) {
                final WorkerRunnableImpl<W> runnable = this.buildWorkerRunnable();
                
                this.runnables.add(runnable);

                this.executors.submit(runnable);
            }

            log.info("{}: service started (in {})", this.name, timer);
            this.stateRef.set(ServiceState.STARTED);
        } finally {
            
        }
    }
    
    @Override
    public void stop() {
        if (!this.stateRef.compareAndSet(ServiceState.STARTED, ServiceState.STOPPING)) {
            throw new IllegalStateException(this.name + ": service not currently started");
        }
        
        final StopWatch timer = StopWatch.timeMillis();
        try {
            log.info("{}: service stopping...", this.name);
            
            this.executors.shutdown();
            
            this.runnables.forEach(runnable -> {
                runnable.stop();
            });
            
            try {
                if (!this.executors.awaitTermination(this.shutdownTimeout.getDuration(), this.shutdownTimeout.getUnit())) {
                    log.warn("{}: service did not shutdown gracefully, forcing stop now!", this.name);
                    this.executors.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("{}: service interrupted while shutting down...", this.name, e);
                log.warn("{}: service will now terminate with a likley un-orderly shutdown", this.name);
                this.executors.shutdownNow();
            }
            
            log.info("{}: service stopped (in {})", this.name, timer);
            this.executors = null;
            this.stateRef.set(ServiceState.STOPPED);
        } finally {
            
        }
    }

}