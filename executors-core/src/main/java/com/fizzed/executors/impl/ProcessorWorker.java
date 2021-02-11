package com.fizzed.executors.impl;

import com.fizzed.executors.core.ExecuteStopException;
import com.fizzed.executors.core.Worker;
import com.fizzed.executors.core.WorkerContext;
import java.util.concurrent.BlockingQueue;
import com.fizzed.executors.core.Processor;
import org.slf4j.Logger;

public class ProcessorWorker<T> implements Worker {

    private final BlockingQueue<T> taskQueue;
    private final Processor<T> processor;

    public ProcessorWorker(
            BlockingQueue<T> taskQueue,
            Processor<T> processor) {
        
        this.taskQueue = taskQueue;
        this.processor = processor;
    }

    @Override
    public Logger getLogger() {
        return this.processor.getLogger();
    }

    @Override
    public void execute(WorkerContext context) throws ExecuteStopException, InterruptedException {
        
        if (this.getLogger() != null) {
            this.getLogger().info("Ready");
        }
        
        // keep running unless a stop has been requested...
        while (!context.isStopRequested()) {
            context.idle();
            
            // while we wait for a task, this actually represents another form of
            // being "idle", so we'll guard against an interrupt from really representing a stop
            final T task = this.taskQueue.take();
            
            context.running();
            this.processor.execute(context, task);
        }
    }
    
}