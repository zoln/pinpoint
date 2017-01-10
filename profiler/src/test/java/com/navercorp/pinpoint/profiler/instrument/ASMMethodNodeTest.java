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

import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.profiler.instrument.mock.ArgsArrayInterceptor;
import com.navercorp.pinpoint.profiler.interceptor.registry.DefaultInterceptorRegistryBinder;
import com.navercorp.pinpoint.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.navercorp.pinpoint.profiler.plugin.DefaultProfilerPluginContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author jaehong.kim
 */
public class ASMMethodNodeTest {

    private final static InterceptorRegistryBinder interceptorRegistryBinder = new DefaultInterceptorRegistryBinder();

    private final DefaultProfilerPluginContext pluginContext = mock(DefaultProfilerPluginContext.class);
    private final TraceContext traceContext = mock(TraceContext.class);

    @Before
    public void setUp() {
        reset(traceContext);
        when(pluginContext.getTraceContext()).thenReturn(traceContext);
        when(pluginContext.injectClass(any(ClassLoader.class), any(String.class))).thenAnswer(new Answer<Class<?>>() {

            @Override
            public Class<?> answer(InvocationOnMock invocation) throws Throwable {
                ClassLoader loader = (ClassLoader) invocation.getArguments()[0];
                String name = (String) invocation.getArguments()[1];

                return loader.loadClass(name);
            }

        });
    }

    @Test
    public void getter() throws Exception {
        final String targetClassName = "com.navercorp.pinpoint.profiler.instrument.mock.NormalClass";
        final String methodName = "sum";

        ASMClass declaringClass = mock(ASMClass.class);
        when(declaringClass.getName()).thenReturn(targetClassName);

        final MethodNode methodNode = ASMClassNodeLoader.get(targetClassName, methodName);

        ASMMethod method = new ASMMethod(pluginContext, interceptorRegistryBinder, declaringClass, methodNode);
        assertEquals(methodName, method.getName());
        assertEquals(1, method.getParameterTypes().length);
        assertEquals("int", method.getParameterTypes()[0]);
        assertEquals("int", method.getReturnType());
        assertEquals(1, method.getModifiers());
        assertEquals(false, method.isConstructor());
        assertNotNull(method.getDescriptor());

    }

    @Test
    public void addInterceptor() throws Exception {
        final int interceptorId = interceptorRegistryBinder.getInterceptorRegistryAdaptor().addInterceptor(new ArgsArrayInterceptor());
        final String targetClassName = "com.navercorp.pinpoint.profiler.instrument.mock.NormalClass";
        final ASMClass declaringClass = mock(ASMClass.class);
        when(declaringClass.getName()).thenReturn(targetClassName);

        ASMClassNodeLoader.TestClassLoader classLoader = ASMClassNodeLoader.getClassLoader();

        classLoader.setTrace(false);
        classLoader.setVerify(false);
        classLoader.setTargetClassName(targetClassName);
        classLoader.setCallbackHandler(new ASMClassNodeLoader.CallbackHandler() {
            @Override
            public void handle(ClassNode classNode) {
                List<MethodNode> methodNodes = classNode.methods;
                for(MethodNode methodNode : methodNodes) {
                    ASMMethod method = new ASMMethod(pluginContext, interceptorRegistryBinder, declaringClass, methodNode);
                    try {
                        method.addInterceptor(interceptorId);
                    } catch (InstrumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Class<?> clazz = classLoader.loadClass(targetClassName);
        Method method = clazz.getDeclaredMethod("sum", int.class);

        assertEquals(55, method.invoke(clazz.newInstance(), 10));
    }
}