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
package com.fizzed.executors.impl;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.internal.ExecuteHelper;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import org.junit.Test;

/**
 *
 * @author jjlauer
 */
public class ExecuteHelperTest {
 
    @Test
    public void staggered() {
        assertThat(ExecuteHelper.staggered(null, 0.0d), is(nullValue()));
        assertThat(ExecuteHelper.staggered(TimeDuration.millis(0), 0.0d), is(ExecuteHelper.ZERO_DURATION));
        
        TimeDuration duration = TimeDuration.millis(1000L);
        
        TimeDuration staggered;
        
        for (int i = 0; i < 100; i++) {
            staggered = ExecuteHelper.staggered(duration, 0.5d);
            assertThat(staggered, is(not(duration)));
            assertThat(staggered, greaterThanOrEqualTo(TimeDuration.millis(500L)));
            assertThat(staggered, lessThanOrEqualTo(TimeDuration.millis(1500L)));
        }
    }
    
}