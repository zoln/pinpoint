package com.navercorp.pinpoint.plugin.hystrix.interceptor;

import com.navercorp.pinpoint.bootstrap.context.AsyncTraceId;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanAsyncEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.Scope;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.hystrix.HystrixPluginConstants;

/**
 * for hystrix-core 1.3
 * @author Jiaqi Feng
 */
@Scope(HystrixPluginConstants.HYSTRIX_COMMAND_EXECUTION_SCOPE)
public class HystrixCommandExecuteCommandInterceptor extends SpanAsyncEventSimpleAroundInterceptor {
    protected final PLogger logger = PLoggerFactory.getLogger(getClass());
    protected final boolean isDebug = logger.isDebugEnabled();

    public HystrixCommandExecuteCommandInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncTraceId asyncTraceId, Object target, Object[] args) {
        if (isDebug) {
            logger.debug("HystrixCommandExecuteCommandInterceptor.doInBeforeTrace()");
        }
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.debug("HystrixCommandExecuteCommandInterceptor.doInAfterTrace()");
        }
        recorder.recordServiceType(HystrixPluginConstants.HYSTRIX_INTERNAL_SERVICE_TYPE);
        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);
    }
}
