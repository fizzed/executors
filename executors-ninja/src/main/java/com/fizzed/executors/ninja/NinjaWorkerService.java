package com.fizzed.executors.ninja;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.core.Worker;
import com.fizzed.executors.core.WorkerService;
import static com.fizzed.executors.ninja.NinjaPropertiesHelper.getDouble;
import static com.fizzed.executors.ninja.NinjaPropertiesHelper.getTimeDuration;
import com.google.inject.Injector;
import java.util.concurrent.TimeUnit;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

abstract public class NinjaWorkerService<W extends Worker> extends WorkerService<W> {

    protected final NinjaProperties ninjaProperties;
    protected final Injector injector;
    protected final String configurationPrefix;
    protected final Class<W> defaultWorkerType;
    
    public NinjaWorkerService(
            String name,
            NinjaProperties ninjaProperties,
            Injector injector,
            String configurationPrefix,
            Class<W> defaultWorkerType) {
        
        super(name);
        this.ninjaProperties = ninjaProperties;
        this.injector = injector;
        this.configurationPrefix = configurationPrefix;
        this.defaultWorkerType = defaultWorkerType;
        
        // delegate most of configuration to helper method
        NinjaWorkerService.configure(this.configurationPrefix, this.ninjaProperties, this);
    }
    
    @Override
    public W newWorker() {
        return this.injector.getInstance(this.defaultWorkerType);
    }

    @Override @Start(order = 91)        // annotation triggers binds to ninja event
    public void start() {
        super.start();
    }
    
    @Override @Dispose                  // annotation triggers binds to ninja event
    public void stop() {
        super.stop();
    }
    
    static public <W extends Worker> void configure(
            String configPrefix,
            NinjaProperties ninjaProperties,
            WorkerService<W> service) {
        
        String name = ninjaProperties.get(configPrefix + ".name");
        if (name != null) {
            service.setName(name);
        }
        
        Integer minPoolSize = null;
        Integer workers = ninjaProperties.getInteger(configPrefix + ".workers");
        if (workers != null) {
            minPoolSize = workers;
        }

        if (minPoolSize != null) {
            service.setMinPoolSize(minPoolSize);
        }

        TimeDuration initialDelay = getTimeDuration(ninjaProperties, configPrefix + ".initial_delay", TimeUnit.MILLISECONDS);
        if (initialDelay != null) {
            service.setInitialDelay(initialDelay);
        }
        
        Double initialDelayStagger = getDouble(ninjaProperties, configPrefix + ".initial_delay_stagger");
        if (initialDelayStagger != null) {
            service.setInitialDelayStagger(initialDelayStagger);
        }

        TimeDuration executeDelay = getTimeDuration(ninjaProperties, configPrefix + ".execute_delay", TimeUnit.MILLISECONDS);
        if (executeDelay != null) {
            service.setExecuteDelay(executeDelay);
        }

        TimeDuration unhandledThrowableDelay = getTimeDuration(ninjaProperties, configPrefix + ".unhandled_throwable_delay", TimeUnit.MILLISECONDS);
        if (unhandledThrowableDelay != null) {
            service.setUnhandledThrowableDelay(unhandledThrowableDelay);
        }

        TimeDuration shutdownTimeout = getTimeDuration(ninjaProperties, configPrefix + ".shutdown_timeout", TimeUnit.MILLISECONDS);
        if (shutdownTimeout != null) {
            service.setShutdownTimeout(shutdownTimeout);
        }
    }

}