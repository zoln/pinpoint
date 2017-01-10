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

package com.navercorp.pinpoint.common.server.bo.codec.stat;

import com.navercorp.pinpoint.common.server.bo.JvmGcType;
import com.navercorp.pinpoint.common.server.bo.stat.ActiveTraceBo;
import com.navercorp.pinpoint.common.server.bo.stat.CpuLoadBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcDetailedBo;
import com.navercorp.pinpoint.common.server.bo.stat.TransactionBo;
import com.navercorp.pinpoint.common.trace.SlotType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author HyunGil Jeong
 */
public class TestAgentStatFactory {

    private static final int MAX_NUM_TEST_VALUES = 20;

    private static final long TIMESTAMP_INTERVAL = 5000L;

    private static final Random RANDOM = new Random();

    public static List<JvmGcBo> createJvmGcBos(String agentId, long startTimestamp, long initialTimestamp) {
        final int numValues = RANDOM.nextInt(MAX_NUM_TEST_VALUES) + 1;
        return createJvmGcBos(agentId, startTimestamp, initialTimestamp, numValues);
    }

    public static List<JvmGcBo> createJvmGcBos(String agentId, long startTimestamp, long initialTimestamp, int numValues) {
        List<JvmGcBo> jvmGcBos = new ArrayList<JvmGcBo>(numValues);
        List<Long> startTimestamps = createStartTimestamps(startTimestamp, numValues);
        List<Long> timestamps = createTimestamps(initialTimestamp, numValues);
        List<Long> heapUseds = TestAgentStatDataPointFactory.LONG.createFluctuatingValues(
                256 * 1024 * 1024L,
                512 * 1024 * 1024L,
                10 * 1024 * 1024L,
                30 * 1024 * 1024L,
                numValues);
        List<Long> heapMaxes = TestAgentStatDataPointFactory.LONG.createConstantValues(
                2 * 1024 * 1024 * 1024L,
                2 * 1024 * 1024 * 1024L,
                numValues);
        List<Long> nonHeapUseds = TestAgentStatDataPointFactory.LONG.createIncreasingValues(
                16 * 1024 * 1024L,
                64 * 1024 * 1024L,
                1 * 1024 * 1024L,
                3 * 1024 * 1024L,
                numValues);
        List<Long> nonHeapMaxes = TestAgentStatDataPointFactory.LONG.createConstantValues(
                128 * 1024 * 1024L,
                128 * 1024 * 1024L,
                numValues);
        List<Long> gcOldCounts = TestAgentStatDataPointFactory.LONG.createIncreasingValues(
                0L,
                1000L,
                0L,
                10L,
                numValues);
        List<Long> gcOldTimes = TestAgentStatDataPointFactory.LONG.createIncreasingValues(
                0L,
                100000000L,
                100L,
                5000L,
                numValues);
        for (int i = 0; i < numValues; ++i) {
            JvmGcBo jvmGcBo = new JvmGcBo();
            jvmGcBo.setAgentId(agentId);
            jvmGcBo.setStartTimestamp(startTimestamps.get(i));
            jvmGcBo.setGcType(JvmGcType.CMS);
            jvmGcBo.setTimestamp(timestamps.get(i));
            jvmGcBo.setHeapUsed(heapUseds.get(i));
            jvmGcBo.setHeapMax(heapMaxes.get(i));
            jvmGcBo.setNonHeapUsed(nonHeapUseds.get(i));
            jvmGcBo.setNonHeapMax(nonHeapMaxes.get(i));
            jvmGcBo.setGcOldCount(gcOldCounts.get(i));
            jvmGcBo.setGcOldTime(gcOldTimes.get(i));
            jvmGcBos.add(jvmGcBo);
        }
        return jvmGcBos;
    }

    public static List<JvmGcDetailedBo> createJvmGcDetailedBos(String agentId, long startTimestamp, long initialTimestamp) {
        final int numValues = RANDOM.nextInt(MAX_NUM_TEST_VALUES) + 1;
        return createJvmGcDetailedBos(agentId, startTimestamp, initialTimestamp, numValues);
    }

    public static List<JvmGcDetailedBo> createJvmGcDetailedBos(String agentId, long startTimestamp, long initialTimestamp, int numValues) {
        List<JvmGcDetailedBo> jvmGcDetailedBos = new ArrayList<JvmGcDetailedBo>(numValues);
        List<Long> startTimestamps = createStartTimestamps(startTimestamp, numValues);
        List<Long> timestamps = createTimestamps(initialTimestamp, numValues);
        List<Long> gcNewCounts = TestAgentStatDataPointFactory.LONG.createIncreasingValues(
                0L,
                10000000L,
                10L,
                1000L,
                numValues);
        List<Long> gcNewTimes = TestAgentStatDataPointFactory.LONG.createIncreasingValues(
                0L,
                100L,
                1L,
                50L,
                numValues);
        List<Double> codeCacheUseds = createRandomPercentageValues(numValues);
        List<Double> newGenUseds = createRandomPercentageValues(numValues);
        List<Double> oldGenUseds = createRandomPercentageValues(numValues);
        List<Double> survivorSpaceUseds = createRandomPercentageValues(numValues);
        List<Double> permGenUseds = createRandomPercentageValues(numValues);
        List<Double> metaspaceUseds = createRandomPercentageValues(numValues);
        for (int i = 0; i < numValues; ++i) {
            JvmGcDetailedBo jvmGcDetailedBo = new JvmGcDetailedBo();
            jvmGcDetailedBo.setAgentId(agentId);
            jvmGcDetailedBo.setStartTimestamp(startTimestamps.get(i));
            jvmGcDetailedBo.setTimestamp(timestamps.get(i));
            jvmGcDetailedBo.setGcNewCount(gcNewCounts.get(i));
            jvmGcDetailedBo.setGcNewTime(gcNewTimes.get(i));
            jvmGcDetailedBo.setCodeCacheUsed(codeCacheUseds.get(i));
            jvmGcDetailedBo.setNewGenUsed(newGenUseds.get(i));
            jvmGcDetailedBo.setOldGenUsed(oldGenUseds.get(i));
            jvmGcDetailedBo.setSurvivorSpaceUsed(survivorSpaceUseds.get(i));
            jvmGcDetailedBo.setPermGenUsed(permGenUseds.get(i));
            jvmGcDetailedBo.setMetaspaceUsed(metaspaceUseds.get(i));
            jvmGcDetailedBos.add(jvmGcDetailedBo);
        }
        return jvmGcDetailedBos;
    }

    public static List<CpuLoadBo> createCpuLoadBos(String agentId, long startTimestamp, long initialTimestamp) {
        final int numValues = RANDOM.nextInt(MAX_NUM_TEST_VALUES) + 1;
        return createCpuLoadBos(agentId, startTimestamp, initialTimestamp, numValues);
    }

    public static List<CpuLoadBo> createCpuLoadBos(String agentId, long startTimestamp, long initialTimestamp, int numValues) {
        List<CpuLoadBo> cpuLoadBos = new ArrayList<CpuLoadBo>(numValues);
        List<Long> startTimestamps = createStartTimestamps(startTimestamp, numValues);
        List<Long> timestamps = createTimestamps(initialTimestamp, numValues);
        List<Double> jvmCpuLoads = createRandomPercentageValues(numValues);
        List<Double> systemCpuLoads = createRandomPercentageValues(numValues);
        for (int i = 0; i < numValues; ++i) {
            CpuLoadBo cpuLoadBo = new CpuLoadBo();
            cpuLoadBo.setStartTimestamp(startTimestamps.get(i));
            cpuLoadBo.setAgentId(agentId);
            cpuLoadBo.setTimestamp(timestamps.get(i));
            cpuLoadBo.setJvmCpuLoad(jvmCpuLoads.get(i));
            cpuLoadBo.setSystemCpuLoad(systemCpuLoads.get(i));
            cpuLoadBos.add(cpuLoadBo);
        }
        return cpuLoadBos;
    }

    public static List<TransactionBo> createTransactionBos(String agentId, long startTimestamp, long initialTimestamp) {
        final int numValues = RANDOM.nextInt(MAX_NUM_TEST_VALUES) + 1;
        return createTransactionBos(agentId, startTimestamp, initialTimestamp, numValues);
    }

    public static List<TransactionBo> createTransactionBos(String agentId, long startTimestamp, long initialTimestamp, int numValues) {
        List<TransactionBo> transactionBos = new ArrayList<TransactionBo>(numValues);
        List<Long> startTimestamps = createStartTimestamps(startTimestamp, numValues);
        List<Long> timestamps = createTimestamps(initialTimestamp, numValues);
        List<Long> collectIntervals = TestAgentStatDataPointFactory.LONG.createFluctuatingValues(
                100L,
                10000L,
                10L,
                100L,
                numValues);
        List<Long> sampledNewCounts = TestAgentStatDataPointFactory.LONG.createFluctuatingValues(
                100L,
                10000L,
                10L,
                100L,
                numValues);
        List<Long> sampledContinuationCounts = TestAgentStatDataPointFactory.LONG.createFluctuatingValues(
                100L,
                10000L,
                10L,
                100L,
                numValues);
        List<Long> unsampledNewCount = TestAgentStatDataPointFactory.LONG.createFluctuatingValues(
                100L,
                10000L,
                10L,
                100L,
                numValues);
        List<Long> unsampledContinuationCount = TestAgentStatDataPointFactory.LONG.createFluctuatingValues(
                100L,
                10000L,
                10L,
                100L,
                numValues);
        for (int i = 0; i < numValues; ++i) {
            TransactionBo transactionBo = new TransactionBo();
            transactionBo.setAgentId(agentId);
            transactionBo.setStartTimestamp(startTimestamps.get(i));
            transactionBo.setTimestamp(timestamps.get(i));
            transactionBo.setCollectInterval(collectIntervals.get(i));
            transactionBo.setSampledNewCount(sampledNewCounts.get(i));
            transactionBo.setSampledContinuationCount(sampledContinuationCounts.get(i));
            transactionBo.setUnsampledNewCount(unsampledNewCount.get(i));
            transactionBo.setUnsampledContinuationCount(unsampledContinuationCount.get(i));
            transactionBos.add(transactionBo);
        }
        return transactionBos;
    }

    public static List<ActiveTraceBo> createActiveTraceBos(String agentId, long startTimestamp, long initialTimestamp) {
        final int numValues = RANDOM.nextInt(MAX_NUM_TEST_VALUES) + 1;
        return createActiveTraceBos(agentId, startTimestamp, initialTimestamp, numValues);
    }

    public static List<ActiveTraceBo> createActiveTraceBos(String agentId, long startTimestamp, long initialTimestamp, int numValues) {
        List<ActiveTraceBo> activeTraceBos = new ArrayList<ActiveTraceBo>(numValues);
        List<Long> startTimestamps = createStartTimestamps(startTimestamp, numValues);
        List<Long> timestamps = createTimestamps(initialTimestamp, numValues);
        List<Integer> fastTraceCounts = TestAgentStatDataPointFactory.INTEGER.createRandomValues(0, 1000, numValues);
        List<Integer> normalTraceCounts = TestAgentStatDataPointFactory.INTEGER.createRandomValues(0, 1000, numValues);
        List<Integer> slowTraceCounts = TestAgentStatDataPointFactory.INTEGER.createRandomValues(0, 1000, numValues);
        List<Integer> verySlowTraceCounts = TestAgentStatDataPointFactory.INTEGER.createRandomValues(0, 1000, numValues);
        int histogramSchemaType = 1;
        for (int i = 0; i < numValues; ++i) {
            ActiveTraceBo activeTraceBo = new ActiveTraceBo();
            activeTraceBo.setAgentId(agentId);
            activeTraceBo.setStartTimestamp(startTimestamps.get(i));
            activeTraceBo.setTimestamp(timestamps.get(i));
            activeTraceBo.setHistogramSchemaType(histogramSchemaType);
            if (RANDOM.nextInt(5) > 0) {
                Map<SlotType, Integer> activeTraceCounts = new HashMap<SlotType, Integer>();
                activeTraceCounts.put(SlotType.FAST, fastTraceCounts.get(i));
                activeTraceCounts.put(SlotType.NORMAL, normalTraceCounts.get(i));
                activeTraceCounts.put(SlotType.SLOW, slowTraceCounts.get(i));
                activeTraceCounts.put(SlotType.VERY_SLOW, verySlowTraceCounts.get(i));
                activeTraceBo.setActiveTraceCounts(activeTraceCounts);
            } else {
                activeTraceBo.setActiveTraceCounts(Collections.<SlotType, Integer>emptyMap());
            }
            activeTraceBos.add(activeTraceBo);
        }
        return activeTraceBos;
    }

    private static List<Long> createStartTimestamps(long startTimestamp, int numValues) {
        return TestAgentStatDataPointFactory.LONG.createConstantValues(startTimestamp, startTimestamp, numValues);
    }

    private static List<Long> createTimestamps(long initialTimestamp, int numValues) {
        long minTimestampInterval = TIMESTAMP_INTERVAL - 5L;
        long maxTimestampInterval = TIMESTAMP_INTERVAL + 5L;
        return TestAgentStatDataPointFactory.LONG.createIncreasingValues(initialTimestamp, initialTimestamp, minTimestampInterval, maxTimestampInterval, numValues);
    }

    private static List<Double> createRandomPercentageValues(int numValues) {
        List<Double> values = new ArrayList<Double>(numValues);
        for (int i = 0; i < numValues; ++i) {
            int randomInt = RANDOM.nextInt(101);
            double value = randomInt;
            if (randomInt != 100) {
                value += RANDOM.nextDouble();
            }
            values.add(value);
        }
        return values;
    }
}
