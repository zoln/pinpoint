/*
 * Copyright 2016 Naver Corp.
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

package com.navercorp.pinpoint.collector.mapper.thrift.stat;

import com.navercorp.pinpoint.collector.mapper.thrift.ThriftBoMapper;
import com.navercorp.pinpoint.common.server.bo.stat.ActiveTraceBo;
import com.navercorp.pinpoint.common.server.bo.stat.AgentStatBo;
import com.navercorp.pinpoint.common.server.bo.stat.CpuLoadBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcDetailedBo;
import com.navercorp.pinpoint.common.server.bo.stat.TransactionBo;
import com.navercorp.pinpoint.thrift.dto.TAgentStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author HyunGil Jeong
 */
@Component
public class AgentStatMapper implements ThriftBoMapper<AgentStatBo, TAgentStat> {

    @Autowired
    private JvmGcBoMapper jvmGcBoMapper;

    @Autowired
    private JvmGcDetailedBoMapper jvmGcDetailedBoMapper;

    @Autowired
    private CpuLoadBoMapper cpuLoadBoMapper;

    @Autowired
    private TransactionBoMapper transactionBoMapper;

    @Autowired
    private ActiveTraceBoMapper activeTraceBoMapper;

    @Override
    public AgentStatBo map(TAgentStat tAgentStat) {
        if (tAgentStat == null) {
            return null;
        }
        final String agentId = tAgentStat.getAgentId();
        final long startTimestamp = tAgentStat.getStartTimestamp();
        final long timestamp = tAgentStat.getTimestamp();
        AgentStatBo agentStatBo = new AgentStatBo();
        agentStatBo.setAgentId(agentId);
        // jvmGc
        if (tAgentStat.isSetGc()) {
            JvmGcBo jvmGcBo = this.jvmGcBoMapper.map(tAgentStat.getGc());
            jvmGcBo.setAgentId(agentId);
            jvmGcBo.setStartTimestamp(startTimestamp);
            jvmGcBo.setTimestamp(timestamp);
            agentStatBo.setJvmGcBos(Arrays.asList(jvmGcBo));
        }
        // jvmGcDetailed
        if (tAgentStat.isSetGc()) {
            if (tAgentStat.getGc().isSetJvmGcDetailed()) {
                JvmGcDetailedBo jvmGcDetailedBo = this.jvmGcDetailedBoMapper.map(tAgentStat.getGc().getJvmGcDetailed());
                jvmGcDetailedBo.setAgentId(agentId);
                jvmGcDetailedBo.setStartTimestamp(startTimestamp);
                jvmGcDetailedBo.setTimestamp(timestamp);
                agentStatBo.setJvmGcDetailedBos(Arrays.asList(jvmGcDetailedBo));
            }
        }
        // cpuLoad
        if (tAgentStat.isSetCpuLoad()) {
            CpuLoadBo cpuLoadBo = this.cpuLoadBoMapper.map(tAgentStat.getCpuLoad());
            cpuLoadBo.setAgentId(agentId);
            cpuLoadBo.setStartTimestamp(startTimestamp);
            cpuLoadBo.setTimestamp(timestamp);
            agentStatBo.setCpuLoadBos(Arrays.asList(cpuLoadBo));
        }
        // transaction
        if (tAgentStat.isSetTransaction()) {
            TransactionBo transactionBo = this.transactionBoMapper.map(tAgentStat.getTransaction());
            transactionBo.setAgentId(agentId);
            transactionBo.setStartTimestamp(startTimestamp);
            transactionBo.setTimestamp(timestamp);
            transactionBo.setCollectInterval(tAgentStat.getCollectInterval());
            agentStatBo.setTransactionBos(Arrays.asList(transactionBo));
        }
        // activeTrace
        if (tAgentStat.isSetActiveTrace() && tAgentStat.getActiveTrace().isSetHistogram()) {
            ActiveTraceBo activeTraceBo = this.activeTraceBoMapper.map(tAgentStat.getActiveTrace());
            activeTraceBo.setAgentId(agentId);
            activeTraceBo.setStartTimestamp(startTimestamp);
            activeTraceBo.setTimestamp(timestamp);
            agentStatBo.setActiveTraceBos(Arrays.asList(activeTraceBo));
        }
        return agentStatBo;
    }
}
