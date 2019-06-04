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

import com.fizzed.crux.util.StopWatch;
import com.fizzed.crux.util.TimeDuration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService<W extends AbstractWorker> implements Service {
    
    protected Logger log;
    protected final AtomicInteger workerIds;
    protected AtomicReference<ServiceState> stateRef;
    protected ScheduledThreadPoolExecutor executors;
    private String name;
    private int minPoolSize;
    private TimeDuration shutdownTimeout;
    private TimeDuration initialDelay;
    private boolean staggeredInitialDelay;
    private CopyOnWriteArrayList<W> workers;
    
    public AbstractService(
            String name,
            int minPoolSize) {
        
        this.workerIds = new AtomicInteger();
        this.stateRef = new AtomicReference<>(ServiceState.STOPPED);
        this.name = name;
        this.minPoolSize = minPoolSize;
        this.log = LoggerFactory.getLogger(this.getClass());
        this.shutdownTimeout = new TimeDuration(60, TimeUnit.SECONDS);
        this.workers = new CopyOnWriteArrayList<>();
        this.initialDelay = null;
        this.staggeredInitialDelay = true;
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

    public boolean isStaggeredInitialDelay() {
        return staggeredInitialDelay;
    }

    public void setStaggeredInitialDelay(boolean staggeredInitialDelay) {
        this.staggeredInitialDelay = staggeredInitialDelay;
    }
    
    private W _newWorker() {
        final int id = this.workerIds.incrementAndGet();
        final String workerName = this.name + "-" + id;
        return this.newWorker(workerName);
    }
    
    abstract public W newWorker(String workerName);
    
    @Override
    public void start() {
        if (!this.stateRef.compareAndSet(ServiceState.STOPPED, ServiceState.STARTING)) {
            throw new IllegalStateException(this.name + ": service not currently stopped");
        }
        
        final StopWatch timer = StopWatch.timeMillis();
        try {
            log.info("{}: service starting...", this.name);

            this.executors = new ScheduledThreadPoolExecutor(
                this.minPoolSize, (Runnable runnable) -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    return thread;
                });
            
            for (int i = 0; i < this.minPoolSize; i++) {
                W worker = this._newWorker();
                
                if (this.initialDelay != null) {
                    if (this.staggeredInitialDelay) {
                        worker.setInitialDelay(ExecuteHelper.staggered(this.initialDelay));
                    } else {
                        worker.setInitialDelay(this.initialDelay);
                    }
                }
                
                this.workers.add(worker);

                this.executors.submit(worker);
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
            
            this.workers.forEach(worker -> {
                worker.stop();
            });
            
            try {
                if (!this.executors.awaitTermination(this.shutdownTimeout.getDuration(), this.shutdownTimeout.getUnit())) {
                    log.warn("{}: service did not shutdown gracefully, forcing stop now!", this.name);
                    this.executors.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("{}: service interrupted while shutting down...", this.name, e);
                log.warn("{}: service will now terminate with a likley un-orderly shutdown", this.name);
                final List<Runnable> runnables = this.executors.shutdownNow();
            }
            
            log.info("{}: service stopped (in {})", this.name, timer);
            this.executors = null;
            this.stateRef.set(ServiceState.STOPPED);
        } finally {
            
        }
    }

}