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
package com.fizzed.executors.impl;

import static com.fizzed.crux.util.Maybe.maybe;
import com.fizzed.crux.util.StopWatch;
import com.fizzed.executors.core.WorkerRunnable;
import com.fizzed.executors.core.*;
import com.fizzed.crux.util.TimeDuration;
import static com.fizzed.executors.internal.ExecuteHelper.ZERO_DURATION;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkerRunnableImpl<W extends Worker> implements Runnable, WorkerRunnable<W> {
    
    protected final Logger log;
    protected final long id;
    protected final String name;
    protected final ReentrantLock lock;
    protected final W worker;
    protected StopWatch stopRequestedTimer;
    protected String message;
    protected WorkerState state;
    protected AtomicReference<Thread> threadRef;
    protected TimeDuration initialDelay;
    protected TimeDuration unhandledThrowableDelay;
    protected TimeDuration executeDelay;

    public WorkerRunnableImpl(
            long id,
            String name,
            W worker) {
        
        this.id = id;
        this.name = name;
        this.worker = worker;
        this.log = maybe(worker.getLogger()).orGet(() -> LoggerFactory.getLogger(this.getClass()));
        this.lock = new ReentrantLock();
        this.stopRequestedTimer = null;
        this.state = WorkerState.INITIAL;
        this.threadRef = new AtomicReference<>();
        this.initialDelay = null;
    }

    @Override
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public W getWorker() {
        return worker;
    }

    @Override
    public TimeDuration getInitialDelay() {
        return initialDelay;
    }

    @Override
    public void setInitialDelay(TimeDuration initialDelay) {
        this.initialDelay = initialDelay;
    }

    @Override
    public TimeDuration getUnhandledThrowableDelay() {
        return unhandledThrowableDelay;
    }

    @Override
    public void setUnhandledThrowableDelay(TimeDuration unhandledThrowableDelay) {
        this.unhandledThrowableDelay = unhandledThrowableDelay;
    }

    @Override
    public TimeDuration getExecuteDelay() {
        return executeDelay;
    }

    @Override
    public void setExecuteDelay(TimeDuration executeDelay) {
        this.executeDelay = executeDelay;
    }
    
    @Override
    public String getMessage() {
        this.lock.lock();
        try {
            return this.message;
        } finally {
            this.lock.unlock();
        }
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
    
    @Override
    public boolean isStopRequested() {
        this.lock.lock();
        try {
            return this.stopRequestedTimer != null;
        } finally {
            this.lock.unlock();
        }
    }
    
    @Override
    public void stop() {
        this.lock.lock();
        try {
            this.stopRequestedTimer = StopWatch.timeMillis();
            
            WorkerState prevState = this.state;
            switch (prevState) {
                case STOPPED:
                case INITIAL:
                    return;     // nothing to do
                case IDLE: {
                    final Thread currentThread = this.threadRef.get();
                    if (currentThread != null) {
                        currentThread.interrupt();
                    } else {
                        log.error("{}: Hmmm... current thread was null (something fishy)", this.name, new Exception());
                    }
                    break;
                }
                default: {
                    log.info("{}: stop requested (currently running {})", this.name, this.message);
                    break;
                }
            }
        } finally {
            this.lock.unlock();
        }
    }
    
    private void verifyNotStoppedOrStopRequested() throws ExecuteStopException {
        if (this.state == WorkerState.STOPPED) {
            throw new IllegalStateException("Worker currently stopped");
        }
        if (this.stopRequestedTimer != null) {
            throw new ExecuteStopException("Stop requested");
        }
    }
    
    protected void idle(TimeDuration duration, String message) throws ExecuteStopException, InterruptedException {
        final boolean hasDuration = duration != null && duration.gt(ZERO_DURATION);

        this.lock.lock();
        try {
            this.verifyNotStoppedOrStopRequested();
            this.state = WorkerState.IDLE;
            if (message != null) {
                this.message = message;
                if (hasDuration) {
                    this.message += " for " + duration;
                }
            }
        } finally {
            this.lock.unlock();
        }
        
        if (hasDuration) {
            try {
                String _message = maybe(message).orElse("idle (sleep)");
                log.debug("{}: {} for {}", this.name, _message, duration);
                Thread.sleep(duration.asMillis());
            } catch (InterruptedException e) {
                if (this.isStopRequested()) {
                    throw new ExecuteStopException("Stop requested", e);
                } else {
                    throw e;
                }
            }
        }
    }
    
    protected void running(String message) throws ExecuteStopException {
        this.lock.lock();
        try {
            this.verifyNotStoppedOrStopRequested();
            this.state = WorkerState.RUNNING;
            if (message != null) {
                this.message = message;
            }
        } finally {
            this.lock.unlock();
        }
    }
    
    private void setStopped(String message) {
        this.lock.lock();
        try {
            this.state = WorkerState.STOPPED;
            
            if (message != null) {
                this.message = message;
            }
            
            // externally requested?
            if (this.stopRequestedTimer != null) {
                this.stopRequestedTimer.stop();
                log.info("{}: stopped (in {})", this.name, this.stopRequestedTimer);
            } else {
                // self requested
                log.info("{}: stopped (initiated by self)", this.name);
            }
        } finally {
            this.lock.unlock();
        }
    }
    
    @Override
    public void run() {
        this.threadRef.set(Thread.currentThread());
        try {
            this.threadRef.get().setName(this.name);
            
            this.verifyNotStoppedOrStopRequested();
            
            this.idle(this.getInitialDelay(), "Initial delay");
            
            final WorkerContextImpl context = new WorkerContextImpl();
            
            while (!this.isStopRequested()) {
                try {
                    this.worker.execute(context);

                    this.idle(this.getExecuteDelay(), "Execute delay");
                } catch (ExecuteStopException | InterruptedException e) {
                    throw e;    // need to break out of while-loop
                } catch (Throwable t) {
                    // uh oh, unhandled throwable (do not break out of while loop)
                    log.error("{}: unhandled throwable!", this.name, t);
                    this.idle(this.getUnhandledThrowableDelay(), "Unhandled throwable delay");
                }
            }
            
            //log.debug("{}: worker exited", this.name);
        } catch (ExecuteStopException e) {
            //log.debug("{}: worker stopped", this.name);
        } catch (InterruptedException e) {
            log.warn("{}: worker interrupted", this.name, e);
        } finally {
            this.setStopped("Stopped");
            this.threadRef.set(null);
        }
    }
    
    private class WorkerContextImpl implements WorkerContext {

        @Override
        public long getId() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
        @Override
        public String getName() {
            return WorkerRunnableImpl.this.getName();
        }

        @Override
        public WorkerState getState() {
            return WorkerRunnableImpl.this.getState();
        }

        @Override
        public boolean isStopRequested() {
            return WorkerRunnableImpl.this.isStopRequested();
        }
        
        @Override
        public void idle(TimeDuration duration, String message) throws ExecuteStopException, InterruptedException {
            WorkerRunnableImpl.this.idle(duration, message);
        }

        @Override
        public void running(String message) throws ExecuteStopException {
            WorkerRunnableImpl.this.running(message);
        }
       
    }
    
}