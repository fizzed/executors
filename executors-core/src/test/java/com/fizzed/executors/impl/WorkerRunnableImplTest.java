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

import static com.fizzed.crux.util.TimeDuration.millis;
import com.fizzed.executors.core.ExecuteStopException;
import com.fizzed.executors.core.Worker;
import com.fizzed.executors.core.WorkerContext;
import com.fizzed.executors.core.WorkerState;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WorkerRunnableImplTest {
 
    @Test
    public void executeStopped() {
        Worker worker = mock(Worker.class);
        
        WorkerRunnableImpl runnable = spy(new WorkerRunnableImpl(1, "test-1", worker));
        
        assertThat(runnable.getState(), is(WorkerState.INITIAL));
        
        runnable.stop();
        
        runnable.run();
        
        assertThat(runnable.isStopRequested(), is(true));
        assertThat(runnable.getState(), is(WorkerState.STOPPED));
        assertThat(runnable.getMessage(), is("Stopped"));
    }
 
    @Test
    public void executeThrowsUnhandledException() throws Exception {
        Worker worker = mock(Worker.class);
        
        IllegalStateException ise = new IllegalStateException("Test exception");
        ExecuteStopException ese = new ExecuteStopException("Test stop");
        doThrow(ise, ese).when(worker).execute(any(WorkerContext.class));
        
        WorkerRunnableImpl runnable = spy(new WorkerRunnableImpl(1, "test-1", worker));
        
        assertThat(runnable.getState(), is(WorkerState.INITIAL));

        // worker should execute exactly twice
        runnable.run();
        
        verify(worker, times(2)).execute(any(WorkerContext.class));
        assertThat(runnable.isStopRequested(), is(false));
    }
 
    @Test
    public void executeThrowsUnhandledExceptionWithDelay() throws Exception {
        Worker worker = mock(Worker.class);
        
        IllegalStateException ise = new IllegalStateException("Test exception");
        ExecuteStopException ese = new ExecuteStopException("Test stop");
        doThrow(ise, ese).when(worker).execute(any(WorkerContext.class));
        
        WorkerRunnableImpl runnable = spy(new WorkerRunnableImpl(1, "test-1", worker));
        runnable.setUnhandledThrowableDelay(millis(10L));

        // worker should execute exactly twice, with a delay in-between
        runnable.run();
        
        verify(worker, times(2)).execute(any(WorkerContext.class));
        verify(runnable, times(1)).idle(eq(millis(10L)), eq("Unhandled throwable delay"));
    }
 
    @Test
    public void executeContinuallyCalled() throws Exception {
        Worker worker = mock(Worker.class);
        
        ExecuteStopException ese = new ExecuteStopException("Test stop");
        doNothing().doNothing().doThrow(ese).when(worker).execute(any(WorkerContext.class));
        
        WorkerRunnableImpl runnable = spy(new WorkerRunnableImpl(1, "test-1", worker));

        // worker should execute exactly 3 times
        runnable.run();
        
        verify(worker, times(3)).execute(any(WorkerContext.class));
    }
 
    @Test
    public void executeContinuallyCalledWithDelay() throws Exception {
        Worker worker = mock(Worker.class);
        
        ExecuteStopException ese = new ExecuteStopException("Test stop");
        doNothing().doNothing().doThrow(ese).when(worker).execute(any(WorkerContext.class));
        
        WorkerRunnableImpl runnable = spy(new WorkerRunnableImpl(1, "test-1", worker));
        runnable.setExecuteDelay(millis(10L));
        
        // worker should execute exactly 3 times
        runnable.run();
        
        verify(worker, times(3)).execute(any(WorkerContext.class));
        verify(runnable, times(2)).idle(eq(millis(10L)), eq("Execute delay"));
    }
    
}