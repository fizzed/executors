package com.fizzed.executors.ninja;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.core.Worker;
import com.fizzed.executors.core.WorkerService;
import static com.fizzed.executors.ninja.NinjaPropertiesHelper.getDouble;
import static com.fizzed.executors.ninja.NinjaPropertiesHelper.getTimeDuration;
import java.util.concurrent.TimeUnit;
import ninja.utils.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class NinjaWorkerService {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

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