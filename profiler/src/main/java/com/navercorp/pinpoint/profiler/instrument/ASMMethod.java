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
 */
package com.navercorp.pinpoint.profiler.instrument;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentContext;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.Scope;
import com.navercorp.pinpoint.bootstrap.interceptor.registry.InterceptorRegistry;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScope;
import com.navercorp.pinpoint.common.util.Asserts;
import com.navercorp.pinpoint.profiler.context.DefaultMethodDescriptor;
import com.navercorp.pinpoint.profiler.instrument.interceptor.CaptureType;
import com.navercorp.pinpoint.profiler.instrument.interceptor.InterceptorDefinition;
import com.navercorp.pinpoint.profiler.instrument.interceptor.InterceptorDefinitionFactory;
import com.navercorp.pinpoint.profiler.instrument.interceptor.InterceptorType;
import com.navercorp.pinpoint.profiler.interceptor.factory.AnnotatedInterceptorFactory;
import com.navercorp.pinpoint.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.navercorp.pinpoint.profiler.util.JavaAssistUtils;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jaehong.kim
 */
public class ASMMethod implements InstrumentMethod {
    // TODO fix inject InterceptorDefinitionFactory
    private static final InterceptorDefinitionFactory interceptorDefinitionFactory = new InterceptorDefinitionFactory();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final InstrumentContext pluginContext;
    private final InterceptorRegistryBinder interceptorRegistryBinder;
    private final ASMClass declaringClass;
    private final ASMMethodNodeAdapter methodNode;
    private final MethodDescriptor descriptor;

    public ASMMethod(InstrumentContext pluginContext, InterceptorRegistryBinder interceptorRegistryBinder, ASMClass declaringClass, MethodNode methodNode) {
        this(pluginContext, interceptorRegistryBinder, declaringClass, new ASMMethodNodeAdapter(JavaAssistUtils.javaNameToJvmName(declaringClass.getName()), methodNode));
    }

    public ASMMethod(InstrumentContext pluginContext, InterceptorRegistryBinder interceptorRegistryBinder, ASMClass declaringClass, ASMMethodNodeAdapter methodNode) {
        this.pluginContext = pluginContext;
        this.interceptorRegistryBinder = interceptorRegistryBinder;
        this.declaringClass = declaringClass;
        this.methodNode = methodNode;

        final String[] parameterVariableNames = this.methodNode.getParameterNames();
        final int lineNumber = this.methodNode.getLineNumber();

        final DefaultMethodDescriptor descriptor = new DefaultMethodDescriptor(declaringClass.getName(), methodNode.getName(), getParameterTypes(), parameterVariableNames);
        descriptor.setLineNumber(lineNumber);

        this.descriptor = descriptor;
    }

    @Override
    public String getName() {
        return this.methodNode.getName();
    }

    @Override
    public String[] getParameterTypes() {
        return this.methodNode.getParameterTypes();
    }

    @Override
    public String getReturnType() {
        return this.methodNode.getReturnType();
    }

    @Override
    public int getModifiers() {
        return this.methodNode.getAccess();
    }

    @Override
    public boolean isConstructor() {
        return this.methodNode.isConstructor();
    }

    @Override
    public MethodDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public int addInterceptor(String interceptorClassName) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        return addInterceptor0(interceptorClassName, null, null, null);
    }

    @Override
    public int addInterceptor(String interceptorClassName, Object[] constructorArgs) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        return addInterceptor0(interceptorClassName, constructorArgs, null, null);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, String scopeName) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scopeName, "scopeName");
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, null, interceptorScope, null);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, InterceptorScope scope) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scope, "scope");
        return addInterceptor0(interceptorClassName, null, scope, null);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, String scopeName, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scopeName, "scopeName");
        Asserts.notNull(executionPolicy, "executionPolicy");
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, null, interceptorScope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(scope, "scope");
        Asserts.notNull(executionPolicy, "executionPolicy");
        return addInterceptor0(interceptorClassName, null, scope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, String scopeName) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scopeName, "scopeName");
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, constructorArgs, interceptorScope, null);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scope, "scope");
        return addInterceptor0(interceptorClassName, constructorArgs, scope, null);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, String scopeName, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scopeName, "scopeName");
        Asserts.notNull(executionPolicy, "executionPolicy");
        final InterceptorScope interceptorScope = this.pluginContext.getInterceptorScope(scopeName);
        return addInterceptor0(interceptorClassName, constructorArgs, interceptorScope, executionPolicy);
    }

    @Override
    public int addScopedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        Asserts.notNull(interceptorClassName, "interceptorClassName");
        Asserts.notNull(constructorArgs, "constructorArgs");
        Asserts.notNull(scope, "scope");
        Asserts.notNull(executionPolicy, "executionPolicy");
        return addInterceptor0(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    @Override
    public void addInterceptor(int interceptorId) throws InstrumentException {
        final Interceptor interceptor = InterceptorRegistry.getInterceptor(interceptorId);
        try {
            addInterceptor0(interceptor, interceptorId);
        } catch (Exception e) {
            throw new InstrumentException("Failed to add interceptor " + interceptor.getClass().getName() + " to " + this.methodNode.getLongName(), e);
        }
    }

    private ScopeInfo resolveScopeInfo(String interceptorClassName, InterceptorScope scope, ExecutionPolicy policy) {
        final Class<? extends Interceptor> interceptorType = this.pluginContext.injectClass(this.declaringClass.getClassLoader(), interceptorClassName);

        if (scope == null) {
            Scope interceptorScope = interceptorType.getAnnotation(Scope.class);

            if (interceptorScope != null) {
                String scopeName = interceptorScope.value();
                scope = this.pluginContext.getInterceptorScope(scopeName);
                policy = interceptorScope.executionPolicy();
            }
        }

        if (scope == null) {
            policy = null;
        } else if (policy == null) {
            policy = ExecutionPolicy.BOUNDARY;
        }

        return new ScopeInfo(scope, policy);
    }

    private static class ScopeInfo {
        private final InterceptorScope scope;
        private final ExecutionPolicy policy;

        public ScopeInfo(InterceptorScope scope, ExecutionPolicy policy) {
            this.scope = scope;
            this.policy = policy;
        }

        public InterceptorScope getScope() {
            return scope;
        }

        public ExecutionPolicy getPolicy() {
            return policy;
        }
    }

    // for internal api
    int addInterceptorInternal(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        if (interceptorClassName == null) {
            throw new NullPointerException("interceptorClassName must not be null");
        }
        return addInterceptor0(interceptorClassName, constructorArgs, scope, executionPolicy);
    }

    private int addInterceptor0(String interceptorClassName, Object[] constructorArgs, InterceptorScope scope, ExecutionPolicy executionPolicy) throws InstrumentException {
        final ScopeInfo scopeInfo = resolveScopeInfo(interceptorClassName, scope, executionPolicy);
        final Interceptor interceptor = createInterceptor(interceptorClassName, scopeInfo, constructorArgs);
        final int interceptorId = this.interceptorRegistryBinder.getInterceptorRegistryAdaptor().addInterceptor(interceptor);

        addInterceptor0(interceptor, interceptorId);
        return interceptorId;
    }

    private Interceptor createInterceptor(String interceptorClassName, ScopeInfo scopeInfo, Object[] constructorArgs) {
        final ClassLoader classLoader = this.declaringClass.getClassLoader();
        // exception handling.
        final AnnotatedInterceptorFactory factory = new AnnotatedInterceptorFactory(this.pluginContext, true);
        final Interceptor interceptor = factory.getInterceptor(classLoader, interceptorClassName, constructorArgs, scopeInfo.getScope(), scopeInfo.getPolicy(), this.declaringClass, this);

        return interceptor;
    }

    private void addInterceptor0(Interceptor interceptor, int interceptorId) {
        if (interceptor == null) {
            throw new NullPointerException("interceptor must not be null");
        }

        final InterceptorDefinition interceptorDefinition = this.interceptorDefinitionFactory.createInterceptorDefinition(interceptor.getClass());
        final Class<?> interceptorClass = interceptorDefinition.getInterceptorClass();
        final CaptureType captureType = interceptorDefinition.getCaptureType();
        if (this.methodNode.hasInterceptor()) {
            logger.warn("Skip adding interceptor. 'already intercepted method' class={}, interceptor={}", this.declaringClass.getName(), interceptorClass.getName());
            return;
        }

        if (this.methodNode.isAbstract() || this.methodNode.isNative()) {
            logger.warn("Skip adding interceptor. 'abstract or native method' class={}, interceptor={}", this.declaringClass.getName(), interceptorClass.getName());
            return;
        }

        int apiId = -1;
        if (interceptorDefinition.getInterceptorType() == InterceptorType.API_ID_AWARE) {
            apiId = this.pluginContext.getTraceContext().cacheApi(this.descriptor);
        }

        // add before interceptor.
        if (isBeforeInterceptor(captureType) && interceptorDefinition.getBeforeMethod() != null) {
            this.methodNode.addBeforeInterceptor(interceptorId, interceptorDefinition, apiId);
            this.declaringClass.setModified(true);
        } else {
            if (isDebug) {
                logger.debug("Skip adding before interceptorDefinition because the interceptorDefinition doesn't have before method: {}", interceptorClass.getName());
            }
        }

        // add after interface.
        if (isAfterInterceptor(captureType) && interceptorDefinition.getAfterMethod() != null) {
            this.methodNode.addAfterInterceptor(interceptorId, interceptorDefinition, apiId);
            this.declaringClass.setModified(true);
        } else {
            if (isDebug) {
                logger.debug("Skip adding after interceptor because the interceptor doesn't have after method: {}", interceptorClass.getName());
            }
        }
    }

    private boolean isBeforeInterceptor(CaptureType captureType) {
        return CaptureType.BEFORE == captureType || CaptureType.AROUND == captureType;
    }

    private boolean isAfterInterceptor(CaptureType captureType) {
        return CaptureType.AFTER == captureType || CaptureType.AROUND == captureType;
    }
}