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
import static com.fizzed.crux.util.TimeDuration.millis;
import static com.fizzed.crux.util.TimeDuration.seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDemo {
 
    static public class Consumer implements Worker {
        static private final Logger log = LoggerFactory.getLogger(Consumer.class);

        @Override
        public Logger getLogger() {
            return log;
        }
        
        @Override
        public void execute(WorkerContext context) throws ExecuteStopException, InterruptedException {
            while (!context.isStopRequested()) {
                context.running("Working!", () -> {
                    TimeDuration workTime = seconds(5);
                    log.debug("Running for {} (should be allowed to finish)", workTime);
                    workTime.sleep();
                });

                context.idle(seconds(2));
            }
        }
  
    }
    
    static public class Consumers extends WorkerService<Consumer> {

        public Consumers() {
            super("Consumers");
        }

        @Override
        public Consumer newWorker(String workerName) {
            return new Consumer();
        }
        
    }
    
    static public void main(String[] args) throws Exception {
        
        Consumers consumers = new Consumers();
        consumers.setMinPoolSize(2);
        consumers.setInitialDelay(seconds(5));
        consumers.setInitialDelayStagger(0.5d);
        
        consumers.start();
        
//        Thread.sleep(1000L);
        seconds(4).sleep();
        
        consumers.stop();
        
        
        seconds(5).sleep();
        
        consumers.start();
        
        
        seconds(26).sleep();
        
        consumers.stop();
    }
    
}