/*
 * Copyright 2014 NAVER Corp.
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
 */

package com.navercorp.pinpoint.profiler;

import com.navercorp.pinpoint.bootstrap.util.IdValidateUtils;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import com.navercorp.pinpoint.common.Version;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.JvmUtils;
import com.navercorp.pinpoint.common.util.SystemPropertyKey;
import com.navercorp.pinpoint.profiler.util.RuntimeMXBeanUtils;


/**
 * @author emeroad
 */
public class AgentInformationFactory {

    private final String agentId;
    private final String applicationName;

    public AgentInformationFactory(String agentId, String applicationName) {
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }
        if (applicationName == null) {
            throw new NullPointerException("applicationName must not be null");
        }

        this.agentId = checkId(agentId);
        this.applicationName = checkId(applicationName);
    }

    public AgentInformation createAgentInformation(ServiceType serverType) {
        if (serverType == null) {
            throw new NullPointerException("serverType must not be null");
        }
        final String machineName = NetworkUtils.getHostName();
        final String hostIp = NetworkUtils.getHostIp();
        final long startTime = RuntimeMXBeanUtils.getVmStartTime();
        final int pid = RuntimeMXBeanUtils.getPid();
        final String jvmVersion = JvmUtils.getSystemProperty(SystemPropertyKey.JAVA_VERSION);
        return new AgentInformation(agentId, applicationName, startTime, pid, machineName, hostIp, serverType, jvmVersion, Version.VERSION);
    }

    private String checkId(String id) {
        if (!IdValidateUtils.validateId(id)) {
            throw new IllegalStateException("invalid Id=" + id);
        }
        return id;
    }


}
