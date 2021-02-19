package com.fizzed.executors.ninja;

import com.fizzed.executors.core.Processor;
import com.fizzed.executors.core.ProcessorService;
import com.google.inject.Injector;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

abstract public class NinjaProcessorService<T,P extends Processor<T>> extends ProcessorService<T,P> {

    protected final NinjaProperties ninjaProperties;
    protected final Injector injector;
    protected final String configurationPrefix;
    protected final Class<P> defaultProcessorType;
    
    public NinjaProcessorService(
            String name,
            NinjaProperties ninjaProperties,
            Injector injector,
            String configurationPrefix,
            Class<P> defaultProcessorType) {
        
        super(name);
        this.ninjaProperties = ninjaProperties;
        this.injector = injector;
        this.configurationPrefix = configurationPrefix;
        this.defaultProcessorType = defaultProcessorType;
        
        // delegate most of configuration to helper method
        NinjaWorkerService.configure(this.configurationPrefix, this.ninjaProperties, this);
    }
    
    @Override
    public P newProcessor() {
        return this.injector.getInstance(this.defaultProcessorType);
    }

    @Override @Start(order = 91)        // annotation triggers binds to ninja event
    public void start() {
        super.start();
    }
    
    @Override @Dispose                  // annotation triggers binds to ninja event
    public void stop() {
        super.stop();
    }
    
}