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
import com.navercorp.pinpoint.thrift.dto.TAgentStatBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HyunGil Jeong
 */
@Component
public class AgentStatBatchMapper implements ThriftBoMapper<AgentStatBo, TAgentStatBatch> {

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
    public AgentStatBo map(TAgentStatBatch tAgentStatBatch) {
        if (!tAgentStatBatch.isSetAgentStats()) {
            return null;
        }
        AgentStatBo agentStatBo = new AgentStatBo();
        final String agentId = tAgentStatBatch.getAgentId();
        final long startTimestamp = tAgentStatBatch.getStartTimestamp();
        agentStatBo.setAgentId(agentId);
        List<JvmGcBo> jvmGcBos = new ArrayList<>(tAgentStatBatch.getAgentStatsSize());
        List<JvmGcDetailedBo> jvmGcDetailedBos = new ArrayList<>(tAgentStatBatch.getAgentStatsSize());
        List<CpuLoadBo> cpuLoadBos = new ArrayList<>(tAgentStatBatch.getAgentStatsSize());
        List<TransactionBo> transactionBos = new ArrayList<>(tAgentStatBatch.getAgentStatsSize());
        List<ActiveTraceBo> activeTraceBos = new ArrayList<>(tAgentStatBatch.getAgentStatsSize());
        for (TAgentStat tAgentStat : tAgentStatBatch.getAgentStats()) {
            final long timestamp = tAgentStat.getTimestamp();
            // jvmGc
            if (tAgentStat.isSetGc()) {
                JvmGcBo jvmGcBo = this.jvmGcBoMapper.map(tAgentStat.getGc());
                jvmGcBo.setAgentId(agentId);
                jvmGcBo.setStartTimestamp(startTimestamp);
                jvmGcBo.setTimestamp(timestamp);
                jvmGcBos.add(jvmGcBo);
            }
            // jvmGcDetailed
            if (tAgentStat.isSetGc()) {
                if (tAgentStat.getGc().isSetJvmGcDetailed()) {
                    JvmGcDetailedBo jvmGcDetailedBo = this.jvmGcDetailedBoMapper.map(tAgentStat.getGc().getJvmGcDetailed());
                    jvmGcDetailedBo.setAgentId(agentId);
                    jvmGcDetailedBo.setStartTimestamp(startTimestamp);
                    jvmGcDetailedBo.setTimestamp(timestamp);
                    jvmGcDetailedBos.add(jvmGcDetailedBo);
                }
            }
            // cpuLoad
            if (tAgentStat.isSetCpuLoad()) {
                CpuLoadBo cpuLoadBo = this.cpuLoadBoMapper.map(tAgentStat.getCpuLoad());
                cpuLoadBo.setAgentId(agentId);
                cpuLoadBo.setStartTimestamp(startTimestamp);
                cpuLoadBo.setTimestamp(timestamp);
                cpuLoadBos.add(cpuLoadBo);
            }
            // transaction
            if (tAgentStat.isSetTransaction()) {
                TransactionBo transactionBo = this.transactionBoMapper.map(tAgentStat.getTransaction());
                transactionBo.setAgentId(agentId);
                transactionBo.setStartTimestamp(startTimestamp);
                transactionBo.setTimestamp(timestamp);
                transactionBo.setCollectInterval(tAgentStat.getCollectInterval());
                transactionBos.add(transactionBo);
            }
            // activeTrace
            if (tAgentStat.isSetActiveTrace() && tAgentStat.getActiveTrace().isSetHistogram()) {
                ActiveTraceBo activeTraceBo = this.activeTraceBoMapper.map(tAgentStat.getActiveTrace());
                activeTraceBo.setAgentId(agentId);
                activeTraceBo.setStartTimestamp(startTimestamp);
                activeTraceBo.setTimestamp(timestamp);
                activeTraceBos.add(activeTraceBo);
            }
        }
        agentStatBo.setJvmGcBos(jvmGcBos);
        agentStatBo.setJvmGcDetailedBos(jvmGcDetailedBos);
        agentStatBo.setCpuLoadBos(cpuLoadBos);
        agentStatBo.setTransactionBos(transactionBos);
        agentStatBo.setActiveTraceBos(activeTraceBos);
        return agentStatBo;
    }
}
