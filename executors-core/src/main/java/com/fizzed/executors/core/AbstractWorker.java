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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWorker implements Worker, Runnable {
    
    protected final Logger log;
    protected final String name;
    protected final ReentrantLock lock;
    protected final Condition stopCondition;
    protected WorkerState state;
    protected AtomicReference<Thread> threadRef;
    protected TimeDuration initialDelay;

    public AbstractWorker(
            String name) {
        
        this.lock = new ReentrantLock();
        this.stopCondition = this.lock.newCondition();
        
        this.log = LoggerFactory.getLogger(this.getClass());
        this.name = name;
        this.state = WorkerState.IDLE;
        this.threadRef = new AtomicReference<>();
        this.initialDelay = null;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public WorkerState getState() {
        this.lock.lock();
        try {
            return this.state;
        } finally {
            this.lock.unlock();
        }
    }

    public TimeDuration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(TimeDuration initialDelay) {
        this.initialDelay = initialDelay;
    }

    @Override
    public boolean isStopped() {
        this.lock.lock();
        try {
            return this.state == WorkerState.STOPPED;
        } finally {
            this.lock.unlock();
        }
    }
    
    @Override
    public void stop() {
        this.lock.lock();
        try {
            WorkerState prevState = this.state;
            
            this.state = WorkerState.STOPPED;
            
            // if stopped replaced idle then its safe to interrupt the thread now!
            if (prevState == WorkerState.IDLE) {
                final Thread currentThread = this.threadRef.get();
                if (currentThread != null) {
                    currentThread.interrupt();
                }
            }
        } finally {
            this.lock.unlock();
        }
    }
    
    protected void idle(TimeDuration duration) throws ExecuteStopException, InterruptedException {
        this.lock.lock();
        try {
            if (this.state == WorkerState.STOPPED) {
                throw new ExecuteStopException("Stop requested");
            }
            
            this.state = WorkerState.IDLE;
        } finally {
            this.lock.unlock();
        }
        
        long sleepMillis = duration != null ? duration.asMillis() : 0;
        
        if (sleepMillis > 0) {
            try {
                log.debug("Idle (sleep) for {}", duration);
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                if (this.state == WorkerState.STOPPED) {
                    throw new ExecuteStopException("Stop requested", e);
                } else {
                    throw e;
                }
            }
        }
    }
    
    protected void running() throws ExecuteStopException {
        this.lock.lock();
        try {
            if (this.state == WorkerState.STOPPED) {
                throw new ExecuteStopException("Stop requested");
            }
            
            this.state = WorkerState.RUNNING;
        } finally {
            this.lock.unlock();
        }
    }
    
    @Override
    public void run() {
        this.threadRef.set(Thread.currentThread());
        try {
            this.threadRef.get().setName(this.name);
            
            if (this.initialDelay != null) {
                this.idle(this.initialDelay);
            }
            
            this.execute();
            
            log.info("{}: worker stopped (exited)", this.name);
        } catch (ExecuteStopException e) {
            log.info("{}: worker stopped", this.name);
        } catch (InterruptedException e) {
            log.info("{}: worker interrupted", this.name, e);
        } finally {
            this.threadRef.set(null);
        }
    }
    
}