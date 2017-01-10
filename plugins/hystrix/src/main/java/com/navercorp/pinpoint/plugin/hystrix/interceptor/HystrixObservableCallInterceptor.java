package com.navercorp.pinpoint.plugin.hystrix.interceptor;

import com.navercorp.pinpoint.bootstrap.context.AsyncTraceId;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanAsyncEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.plugin.hystrix.HystrixPluginConstants;
import com.navercorp.pinpoint.plugin.hystrix.field.EnclosingInstanceFieldGetter;

/**
 * for hystrix-core above 1.4
 *
 * Created by jack on 4/21/16.
 */

public abstract class HystrixObservableCallInterceptor extends SpanAsyncEventSimpleAroundInterceptor {

    protected HystrixObservableCallInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncTraceId asyncTraceId, Object target, Object[] arqgs) {
    }
    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordServiceType(HystrixPluginConstants.HYSTRIX_INTERNAL_SERVICE_TYPE);
        recorder.recordApi(methodDescriptor);
        recorder.recordAttribute(HystrixPluginConstants.HYSTRIX_COMMAND_EXECUTION_ANNOTATION_KEY, getExecutionType());
        recorder.recordException(throwable);
    }

    @Override
    protected AsyncTraceId getAsyncTraceId(Object target) {
        return super.getAsyncTraceId(getEnclosingInstance(target));
    }

    protected abstract String getExecutionType();

    protected Object getEnclosingInstance(Object target) {
        if (!(target instanceof EnclosingInstanceFieldGetter)) {
            if (isDebug) {
                logger.debug("Invalid target object. Need field accessor({}).", EnclosingInstanceFieldGetter.class.getName());
            }
            return null;
        } else {
            return ((EnclosingInstanceFieldGetter) target)._$PINPOINT$_getEnclosingInstance();
        }
    }
}
