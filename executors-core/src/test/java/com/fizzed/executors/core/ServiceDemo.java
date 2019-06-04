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
import java.util.concurrent.TimeUnit;

public class ServiceDemo {
 
    static public class Consumer extends AbstractWorker {

        public Consumer(String name) {
            super(name);
        }

        @Override
        public void execute() throws ExecuteStopException, InterruptedException {
            while (!this.isStopped()) {
                this.running();

                log.debug("Running for 5 sec (should be allowed to finish)");
                Thread.sleep(5000L);
                
                this.idle(new TimeDuration(2, TimeUnit.SECONDS));
            }
        }
        
    }
    
    static public class Consumers extends AbstractService<Consumer> {

        public Consumers(int minPoolSize) {
            super("Consumers", minPoolSize);
        }

        @Override
        public Consumer newWorker(String workerName) {
            return new Consumer(workerName);
        }
        
    }
    
    
    
    static public void main(String[] args) throws Exception {
        
        Consumers consumers = new Consumers(2);
        consumers.setInitialDelay(new TimeDuration(3000, TimeUnit.MILLISECONDS));
        consumers.start();
        
//        Thread.sleep(1000L);
        Thread.sleep(4*1000L);
        
        consumers.stop();
        
        
        Thread.sleep(5000L);
        
        consumers.start();
        
        Thread.sleep(120*1000L);
    }
    
}