/*
 * Copyright 2016 Pinpoint contributors and NAVER Corp.
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
package com.navercorp.pinpoint.plugin.jboss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.List;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardHost;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.navercorp.pinpoint.bootstrap.context.Header;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.common.server.bo.SpanEventBo;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.test.junit4.BasePinpointTest;
import com.navercorp.pinpoint.test.junit4.IsRootSpan;

/**
 * The Class StandardHostValveInvokeModifierTest.
 *
 * @author hyungil.jeong
 */
public class StandardHostValveInvokeModifierTest extends BasePinpointTest {

    /** The Constant SERVICE_TYPE. */
    private static final ServiceType SERVICE_TYPE = JbossConstants.JBOSS;

    /** The Constant REQUEST_URI. */
    private static final String REQUEST_URI = "testRequestUri";

    /** The Constant SERVER_NAME. */
    private static final String SERVER_NAME = "serverForTest";

    /** The Constant SERVER_PORT. */
    private static final int SERVER_PORT = 19999;

    /** The Constant REMOTE_ADDRESS. */
    private static final String REMOTE_ADDRESS = "1.1.1.1";

    /** The Constant EMPTY_PARAM_KEYS. */
    private static final Enumeration<String> EMPTY_PARAM_KEYS = new Enumeration<String>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public String nextElement() {
            return null;
        }
    };

    /** The host. */
    private StandardHost host;

    /** The mock request. */
    @Mock
    private Request mockRequest;

    /** The mock response. */
    @Mock
    private Response mockResponse;

    /**
     * Sets the up.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        initMockRequest();
        // StandardHost's default constructor sets StandardHostValve as the first item in the pipeline.
        host = new StandardHost();
    }

    /**
     * Inits the mock request.
     */
    private void initMockRequest() {
        when(mockRequest.getRequestURI()).thenReturn(REQUEST_URI);
        when(mockRequest.getServerName()).thenReturn(SERVER_NAME);
        when(mockRequest.getServerPort()).thenReturn(SERVER_PORT);
        when(mockRequest.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        when(mockRequest.getParameterNames()).thenReturn(EMPTY_PARAM_KEYS);
    }

    /**
     * Invoke should be traced.
     *
     * @throws Exception the exception
     */
    @Test
    @IsRootSpan
    public void invokeShouldBeTraced() throws Exception {
        // Given
        // When
        host.invoke(mockRequest, mockResponse);
        // Then
        final List<SpanBo> rootSpans = getCurrentRootSpans();
        assertEquals(rootSpans.size(), 1);

        final SpanBo rootSpan = rootSpans.get(0);
        assertEquals(rootSpan.getParentSpanId(), -1);
        assertEquals(rootSpan.getServiceType(), SERVICE_TYPE.getCode());
        assertEquals(rootSpan.getRpc(), REQUEST_URI);
        assertEquals(rootSpan.getEndPoint(), SERVER_NAME + ":" + SERVER_PORT);
        assertTrue(StringUtils.isNotBlank(rootSpan.getRemoteAddr()));
    }

    /**
     * Invoke should trace exceptions.
     *
     * @throws Exception the exception
     */
    @Test
    @IsRootSpan
    public void invokeShouldTraceExceptions() throws Exception {
        // Given
        when(mockRequest.getContext()).thenThrow(new RuntimeException("expected exception."));
        // When
        try {
            host.invoke(mockRequest, mockResponse);
            assertTrue(false);
        } catch (final RuntimeException e) {
            // Then
            final List<SpanBo> rootSpans = getCurrentRootSpans();
            assertEquals(rootSpans.size(), 1);

            final SpanBo rootSpan = rootSpans.get(0);
            assertEquals(rootSpan.getParentSpanId(), -1);
            assertEquals(rootSpan.getServiceType(), SERVICE_TYPE.getCode());

            final List<SpanEventBo> spanEvents = getCurrentSpanEvents();
            final SpanEventBo spanEvent = spanEvents.get(0);

            assertTrue(spanEvent.hasException());
        }
    }

    /**
     * Invoke should continue tracing from request.
     *
     * @throws Exception the exception
     */
    @Test
    @IsRootSpan
    public void invokeShouldContinueTracingFromRequest() throws Exception {
        // Given
        // Set Transaction ID from remote source.
        final String sourceAgentId = "agentId";
        final long sourceAgentStartTime = 1234567890123L;
        final long sourceTransactionSequence = 12345678L;
        final String sourceTransactionId = TransactionIdUtils.formatString(sourceAgentId, sourceAgentStartTime, sourceTransactionSequence);
        when(mockRequest.getHeader(Header.HTTP_TRACE_ID.toString())).thenReturn(sourceTransactionId);
        // Set parent Span ID from remote source.
        final long sourceParentId = 99999;
        when(mockRequest.getHeader(Header.HTTP_PARENT_SPAN_ID.toString())).thenReturn(String.valueOf(sourceParentId));
        // When
        host.invoke(mockRequest, mockResponse);
        // Then
        final List<SpanBo> rootSpans = getCurrentRootSpans();
        assertEquals(rootSpans.size(), 1);

        final SpanBo rootSpan = rootSpans.get(0);
        // Check Transaction ID from remote source.
        assertEquals(TransactionIdUtils.formatString(rootSpan.getTransactionId()), sourceTransactionId);
        assertEquals(rootSpan.getTransactionId().getAgentId(), sourceAgentId);
        assertEquals(rootSpan.getTransactionId().getAgentStartTime(), sourceAgentStartTime);
        assertEquals(rootSpan.getTransactionId().getTransactionSequence(), sourceTransactionSequence);
        // Check parent Span ID from remote source.
        assertEquals(rootSpan.getParentSpanId(), sourceParentId);
    }

}
