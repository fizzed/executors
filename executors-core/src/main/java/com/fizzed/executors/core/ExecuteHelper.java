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
import java.util.Random;

public class ExecuteHelper {
 
    static private final Random RANDOM = new Random();
    
    static public TimeDuration staggered(TimeDuration duration) {
        long jitter = (long)Math.floor((double)duration.getDuration() * (double)0.5);
        long leftLimit = duration.getDuration() - jitter;
        long rightLimit = duration.getDuration() + jitter;
        long staggeredDuration = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
        return new TimeDuration(staggeredDuration, duration.getUnit());
    }
    
}