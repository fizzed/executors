package com.fizzed.executors.core;

import static com.fizzed.crux.util.TimeDuration.millis;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WorkerServiceTest {
 
    static public class TestWorker implements Worker {
        @Override
        public void execute(WorkerContext context) throws ExecuteStopException, InterruptedException {
            context.running();
            Thread.sleep(100L);
        }
    }
    
    static public class TestWorkerService extends WorkerService<TestWorker> {

        public TestWorkerService() {
            super("Test Service");
        }

        @Override
        public TestWorker newWorker(String workerName) {
            return spy(new TestWorker());
        }
        
    }
    
    @Test
    public void startStopStartStop() throws Exception {
        TestWorkerService service = new TestWorkerService();
        
        assertThat(service.getState(), is(ServiceState.STOPPED));
        
        service.start();
        
        millis(1000L).sleep();
        
        assertThat(service.getState(), is(ServiceState.STARTED));
        assertThat(service.getRunnables(), hasSize(1));
        
        for (WorkerRunnable runnable : service.getRunnables()) {
            verify(runnable.getWorker(), atLeast(1)).execute(any(WorkerContext.class));
        }
        
        service.stop();
        
        millis(1000L).sleep();
        
        assertThat(service.getState(), is(ServiceState.STOPPED));
        assertThat(service.getRunnables(), hasSize(1));
        
        
        // restart it!
        service.start();
        
        millis(1000L).sleep();
        
        assertThat(service.getState(), is(ServiceState.STARTED));
        assertThat(service.getRunnables(), hasSize(1));
        
        for (WorkerRunnable runnable : service.getRunnables()) {
            verify(runnable.getWorker(), atLeast(1)).execute(any(WorkerContext.class));
        }
    }
    
    @Test
    public void insaneRunning() throws Exception {
        TestWorkerService service = new TestWorkerService();
        
        
    }
    
}