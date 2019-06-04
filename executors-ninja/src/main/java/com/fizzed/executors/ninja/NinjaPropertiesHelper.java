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
package com.fizzed.executors.ninja;

import com.fizzed.crux.util.TimeDuration;
import java.util.concurrent.TimeUnit;
import ninja.utils.NinjaProperties;

public class NinjaPropertiesHelper {
 
    static public TimeDuration getTimeDuration(
        NinjaProperties ninjaProperties, String key) {
        
        return getTimeDuration(ninjaProperties, key, null);
    }
    
    static public TimeDuration getTimeDuration(
        NinjaProperties ninjaProperties, String key, TimeUnit defaultUnit) {
        
        String v = ninjaProperties.get(key);
        
        return TimeDuration.parse(v, defaultUnit);
    }
    
    static public Double getDouble(
        NinjaProperties ninjaProperties, String key) {
        
        String v = ninjaProperties.get(key);
        
        if (v == null) {
            return null;
        }
        
        return Double.valueOf(v);
    }
    
}