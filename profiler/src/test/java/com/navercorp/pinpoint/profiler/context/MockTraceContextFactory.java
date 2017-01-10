/*
 * Copyright 2016 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.navercorp.pinpoint.profiler.context;

import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.profiler.sender.LoggingDataSender;

/**
 * @author emeroad
 */
public class MockTraceContextFactory {

    private EnhancedDataSender priorityDataSender = new LoggingDataSender();

    public void setPriorityDataSender(EnhancedDataSender priorityDataSender) {
        if (priorityDataSender == null) {
            throw new NullPointerException("priorityDataSender must not be null");
        }
        this.priorityDataSender = priorityDataSender;
    }

    public TraceContext create() {
        DefaultTraceContext traceContext = new DefaultTraceContext(new TestAgentInformation()) ;
        ProfilerConfig profilerConfig = new DefaultProfilerConfig();
        traceContext.setProfilerConfig(profilerConfig);


        traceContext.setPriorityDataSender(priorityDataSender);

        return traceContext;
    }
}
