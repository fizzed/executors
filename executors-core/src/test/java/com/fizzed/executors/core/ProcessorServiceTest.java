package com.fizzed.executors.core;

import static com.fizzed.crux.util.TimeDuration.millis;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import org.junit.Test;
import static org.mockito.Mockito.spy;

public class ProcessorServiceTest {
 
    static public class TestProcessor implements Processor<CountDownLatch> {
        @Override
        public void execute(WorkerContext context, CountDownLatch task) throws ExecuteStopException, InterruptedException {
            Thread.sleep(100L);
            context.running("wow running!");
            task.countDown();
        }
    }
    
    static public class TestProcessorService extends ProcessorService<CountDownLatch,TestProcessor> {

        public TestProcessorService() {
            super("Test Processor Service");
            this.setInitialDelayStagger(0.0d);
        }

        @Override
        protected TestProcessor newProcessor() {
            return spy(new TestProcessor());
        }
        
    }
    
    @Test
    public void startStopStartStop() throws Exception {
        TestProcessorService service = new TestProcessorService();
        
        assertThat(service.getState(), is(ServiceState.STOPPED));
        
        service.start();
        
        millis(1000L).sleep();
        
        assertThat(service.getState(), is(ServiceState.STARTED));
        assertThat(service.getRunnables(), hasSize(1));
        assertThat(service.getRunnables().get(0).getMessage(), is("Idle"));
        
        final CountDownLatch latch1 = new CountDownLatch(1);
        
        service.submit(latch1);
        
        latch1.await(2L, TimeUnit.SECONDS);
        Thread.sleep(1000L);
        
        assertThat(latch1.getCount(), is(0L));
        assertThat(service.getRunnables().get(0).getMessage(), is("Idle"));
    }
    
}