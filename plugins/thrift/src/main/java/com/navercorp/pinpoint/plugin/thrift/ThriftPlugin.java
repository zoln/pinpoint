/*
 * Copyright 2015 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.thrift;

import java.security.ProtectionDomain;
import java.util.List;

import com.navercorp.pinpoint.bootstrap.async.AsyncTraceIdAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author HyunGil Jeong
 */
public class ThriftPlugin implements ProfilerPlugin, TransformTemplateAware {

    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ThriftPluginConfig config = new ThriftPluginConfig(context.getConfig());

        boolean traceClient = config.traceThriftClient();
        boolean traceClientAsync = config.traceThriftClientAsync();
        boolean traceProcessor = config.traceThriftProcessor();
        boolean traceProcessorAsync = config.traceThriftProcessorAsync();
        boolean traceCommon = traceClient || traceProcessor;

        if (traceClient) {
            addInterceptorsForSynchronousClients(config);
            if (traceClientAsync) {
                addInterceptorsForAsynchronousClients();
            }
        }

        if (traceProcessor) {
            addInterceptorsForSynchronousProcessors();
            if (traceProcessorAsync) {
                addInterceptorsForAsynchronousProcessors();
            }
        }

        if (traceCommon) {
            addInterceptorsForRetrievingSocketAddresses();
            addTProtocolEditors(config);
        }
    }

    // Client - synchronous

    private void addInterceptorsForSynchronousClients(ThriftPluginConfig config) {
        addTServiceClientEditor(config);
    }

    private void addTServiceClientEditor(ThriftPluginConfig config) {
        final boolean traceServiceArgs = config.traceThriftServiceArgs();
        final boolean traceServiceResult = config.traceThriftServiceResult();

        final String targetClassName = "org.apache.thrift.TServiceClient";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                // TServiceClient.sendBase(String, TBase)
                final InstrumentMethod sendBase = target.getDeclaredMethod("sendBase", "java.lang.String", "org.apache.thrift.TBase");
                if (sendBase != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.client.TServiceClientSendBaseInterceptor";
                    sendBase.addInterceptor(interceptor, va(traceServiceArgs));
                }

                // TServiceClient.receiveBase(TBase, String)
                final InstrumentMethod receiveBase = target.getDeclaredMethod("receiveBase", "org.apache.thrift.TBase", "java.lang.String");
                if (receiveBase != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.client.TServiceClientReceiveBaseInterceptor";
                    receiveBase.addInterceptor(interceptor, va(traceServiceResult));
                }

                return target.toBytecode();
            }
        });
    }

    // Client - asynchronous

    private void addInterceptorsForAsynchronousClients() {
        addTAsyncClientManagerEditor();
        addTAsyncMethodCallEditor();
    }

    private void addTAsyncClientManagerEditor() {
        final String targetClassName = "org.apache.thrift.async.TAsyncClientManager";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                // TAsyncClientManager.call(TAsyncMethodCall)
                final InstrumentMethod call = target.getDeclaredMethod("call", "org.apache.thrift.async.TAsyncMethodCall");
                if (call != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.client.async.TAsyncClientManagerCallInterceptor";
                    call.addInterceptor(interceptor);
                }

                return target.toBytecode();
            }

        });
    }

    private void addTAsyncMethodCallEditor() {
        final String targetClassName = "org.apache.thrift.async.TAsyncMethodCall";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(AsyncTraceIdAccessor.class.getName());
                target.addField(ThriftConstants.FIELD_ACCESSOR_SOCKET_ADDRESS);
                target.addGetter(ThriftConstants.FIELD_GETTER_T_NON_BLOCKING_TRANSPORT, ThriftConstants.T_ASYNC_METHOD_CALL_FIELD_TRANSPORT);

                // TAsyncMethodCall(TAsyncClient, TProtocolFactory, TNonblockingTransport, AsyncMethodCallback<T>, boolean)
                final InstrumentMethod constructor = target.getConstructor("org.apache.thrift.async.TAsyncClient",
                        "org.apache.thrift.protocol.TProtocolFactory", "org.apache.thrift.transport.TNonblockingTransport",
                        "org.apache.thrift.async.AsyncMethodCallback", "boolean");
                if (constructor != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.client.async.TAsyncMethodCallConstructInterceptor";
                    constructor.addInterceptor(interceptor);
                }

                // TAsyncMethodCall.cleanUpAndFireCallback(SelectionKey)
                final InstrumentMethod cleanUpAndFireCallback = target.getDeclaredMethod("cleanUpAndFireCallback", "java.nio.channels.SelectionKey");
                if (cleanUpAndFireCallback != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.client.async.TAsyncMethodCallCleanUpAndFireCallbackInterceptor";
                    cleanUpAndFireCallback.addInterceptor(interceptor);
                }

                // TAsyncMethodCall.onError(Exception)
                final InstrumentMethod onError = target.getDeclaredMethod("onError", "java.lang.Exception");
                if (onError != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.client.async.TAsyncMethodCallOnErrorInterceptor";
                    onError.addInterceptor(interceptor);
                }

                return target.toBytecode();
            }

        });
    }

    // Processor - synchronous

    private void addInterceptorsForSynchronousProcessors() {
        addTBaseProcessorEditor();
        addProcessFunctionEditor();
    }

    private void addTBaseProcessorEditor() {
        final String targetClassName = "org.apache.thrift.TBaseProcessor";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                // TBaseProcessor.process(TProtocol, TProtocol)
                final InstrumentMethod process = target.getDeclaredMethod("process", "org.apache.thrift.protocol.TProtocol",
                        "org.apache.thrift.protocol.TProtocol");
                if (process != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.server.TBaseProcessorProcessInterceptor";
                    process.addInterceptor(interceptor);
                }

                return target.toBytecode();
            }

        });
    }

    private void addProcessFunctionEditor() {
        final String targetClassName = "org.apache.thrift.ProcessFunction";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SERVER_MARKER_FLAG);

                // ProcessFunction.process(int, TProtocol, TProtocol, I)
                final InstrumentMethod process = target.getDeclaredMethod("process", "int", "org.apache.thrift.protocol.TProtocol",
                        "org.apache.thrift.protocol.TProtocol", "java.lang.Object");
                if (process != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.server.ProcessFunctionProcessInterceptor";
                    process.addInterceptor(interceptor);
                }

                return target.toBytecode();
            }

        });
    }

    // Processor - asynchronous

    private void addInterceptorsForAsynchronousProcessors() {
        addTBaseAsyncProcessorEditor();
    }

    private void addTBaseAsyncProcessorEditor() {
        final String targetClassName = "org.apache.thrift.TBaseAsyncProcessor";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SERVER_MARKER_FLAG);
                target.addField(ThriftConstants.FIELD_ACCESSOR_ASYNC_MARKER_FLAG);

                // TBaseAsyncProcessor.process(AbstractNonblockingServer$AsyncFrameBuffer)
                final InstrumentMethod process = target.getDeclaredMethod("process", "org.apache.thrift.server.AbstractNonblockingServer$AsyncFrameBuffer");
                if (process != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.server.async.TBaseAsyncProcessorProcessInterceptor";
                    process.addInterceptor(interceptor);
                }

                return target.toBytecode();
            }

        });
    }

    // Common

    private void addInterceptorsForRetrievingSocketAddresses() {
        // injector TTranports
        // TSocket(Socket), TSocket(String, int, int)
        addTTransportEditor("org.apache.thrift.transport.TSocket",
                "com.navercorp.pinpoint.plugin.thrift.interceptor.transport.TSocketConstructInterceptor", new String[]{"java.net.Socket"}, new String[]{
                        "java.lang.String", "int", "int"});

        // wrapper TTransports
        // TFramedTransport(TTransport), TFramedTransport(TTransport, int)
        addTTransportEditor("org.apache.thrift.transport.TFramedTransport",
                "com.navercorp.pinpoint.plugin.thrift.interceptor.transport.wrapper.TFramedTransportConstructInterceptor",
                new String[]{"org.apache.thrift.transport.TTransport"}, new String[]{"org.apache.thrift.transport.TTransport", "int"});
        // TFastFramedTransport(TTransport, int, int)
        addTTransportEditor("org.apache.thrift.transport.TFastFramedTransport",
                "com.navercorp.pinpoint.plugin.thrift.interceptor.transport.wrapper.TFastFramedTransportConstructInterceptor", new String[]{
                        "org.apache.thrift.transport.TTransport", "int", "int"});
        // TSaslClientTransport(TTransport), TSaslClientTransport(SaslClient, TTransport)
        addTTransportEditor("org.apache.thrift.transport.TSaslClientTransport",
                "com.navercorp.pinpoint.plugin.thrift.interceptor.transport.wrapper.TSaslTransportConstructInterceptor",
                new String[]{"org.apache.thrift.transport.TTransport"}, new String[]{"javax.security.sasl.SaslClient",
                        "org.apache.thrift.transport.TTransport"});

        // TMemoryInputTransport - simply add socket field
        addTTransportEditor("org.apache.thrift.transport.TMemoryInputTransport");
        // TIOStreamTransport - simply add socket field
        addTTransportEditor("org.apache.thrift.transport.TIOStreamTransport");

        // nonblocking
        addTNonblockingSocketEditor();
        // AbstractNonblockingServer$FrameBuffer(TNonblockingTransport, SelectionKey, AbstractSelectThread)
        addFrameBufferEditor();
    }

    // Common - transports

    private void addTTransportEditor(String tTransportFqcn) {
        final String targetClassName = tTransportFqcn;
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SOCKET);
                return target.toBytecode();
            }

        });
    }

    private void addTTransportEditor(String tTransportClassName, final String tTransportInterceptorFqcn,
                                     final String[]... parameterTypeGroups) {
        final String targetClassName = tTransportClassName;
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SOCKET);

                for (String[] parameterTypeGroup : parameterTypeGroups) {
                    final InstrumentMethod constructor = target.getConstructor(parameterTypeGroup);
                    if (constructor != null) {
                        constructor.addInterceptor(tTransportInterceptorFqcn);
                    }
                }

                return target.toBytecode();
            }

        });
    }

    private void addTNonblockingSocketEditor() {
        final String targetClassName = "org.apache.thrift.transport.TNonblockingSocket";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SOCKET);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SOCKET_ADDRESS);

                // TNonblockingSocket(SocketChannel, int, SocketAddress)
                final InstrumentMethod constructor = target.getConstructor("java.nio.channels.SocketChannel", "int", "java.net.SocketAddress");
                if (constructor != null) {
                    String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.transport.TNonblockingSocketConstructInterceptor";
                    constructor.addInterceptor(interceptor);
                }

                return target.toBytecode();
            }

        });
    }

    private void addFrameBufferEditor() {
        final String targetClassName = "org.apache.thrift.server.AbstractNonblockingServer$FrameBuffer";
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(ThriftConstants.FIELD_ACCESSOR_SOCKET);
                target.addGetter(ThriftConstants.FIELD_GETTER_T_NON_BLOCKING_TRANSPORT, ThriftConstants.FRAME_BUFFER_FIELD_TRANS_);

                // [THRIFT-1972] - 0.9.1 added a field for the wrapper around trans_ field, while getting rid of getInputTransport() method
                if (target.hasField(ThriftConstants.FRAME_BUFFER_FIELD_IN_TRANS_)) {
                    target.addGetter(ThriftConstants.FIELD_GETTER_T_TRANSPORT, ThriftConstants.FRAME_BUFFER_FIELD_IN_TRANS_);
                    // AbstractNonblockingServer$FrameBuffer(TNonblockingTransport, SelectionKey, AbstractSelectThread)
                    final InstrumentMethod constructor = target.getConstructor(
                            "org.apache.thrift.server.AbstractNonblockingServer", // inner class - implicit reference to outer class instance
                            "org.apache.thrift.transport.TNonblockingTransport", "java.nio.channels.SelectionKey",
                            "org.apache.thrift.server.AbstractNonblockingServer$AbstractSelectThread");
                    if (constructor != null) {
                        String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.server.nonblocking.FrameBufferConstructInterceptor";
                        constructor.addInterceptor(interceptor);
                    }
                }

                // 0.8.0, 0.9.0 doesn't have a separate trans_ field - hook getInputTransport() method
                if (target.hasMethod("getInputTransport", "org.apache.thrift.transport.TTransport")) {
                    // AbstractNonblockingServer$FrameBuffer.getInputTransport(TTransport)
                    final InstrumentMethod getInputTransport = target.getDeclaredMethod("getInputTransport", "org.apache.thrift.transport.TTransport");
                    if (getInputTransport != null) {
                        String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.server.nonblocking.FrameBufferGetInputTransportInterceptor";
                        getInputTransport.addInterceptor(interceptor);
                    }
                }

                return target.toBytecode();
            }

        });
    }

    // Common - protocols

    private void addTProtocolEditors(ThriftPluginConfig config) {
        addTProtocolInterceptors(config, "org.apache.thrift.protocol.TBinaryProtocol");
        addTProtocolInterceptors(config, "org.apache.thrift.protocol.TCompactProtocol");
        addTProtocolInterceptors(config, "org.apache.thrift.protocol.TJSONProtocol");
        addTProtocolDecoratorEditor();
    }

    private void addTProtocolInterceptors(ThriftPluginConfig config, String tProtocolClassName) {
        final boolean traceThriftClient = config.traceThriftClient();
        final boolean traceThriftProcessor = config.traceThriftProcessor();

        final String targetClassName = tProtocolClassName;
        transformTemplate.transform(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                // client
                if (traceThriftClient) {
                    // TProtocol.writeFieldStop()
                    final InstrumentMethod writeFieldStop = target.getDeclaredMethod("writeFieldStop");
                    if (writeFieldStop != null) {
                        String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.tprotocol.client.TProtocolWriteFieldStopInterceptor";
                        writeFieldStop.addInterceptor(interceptor);
                    }
                }

                // processor
                if (traceThriftProcessor) {
                    target.addField(ThriftConstants.FIELD_ACCESSOR_SERVER_MARKER_FLAG);
                    // TProtocol.readFieldBegin()
                    final InstrumentMethod readFieldBegin = target.getDeclaredMethod("readFieldBegin");
                    if (readFieldBegin != null) {
                        String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.tprotocol.server.TProtocolReadFieldBeginInterceptor";
                        readFieldBegin.addInterceptor(interceptor);
                    }
                    // TProtocol.readBool, TProtocol.readBinary, TProtocol.readI16, TProtocol.readI64
                    final List<InstrumentMethod> readTTypes = target.getDeclaredMethods(MethodFilters.name("readBool", "readBinary", "readI16", "readI64"));
                    if (readTTypes != null) {
                        String tTypeCommonInterceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.tprotocol.server.TProtocolReadTTypeInterceptor";
                        for (InstrumentMethod readTType : readTTypes) {
                            if (readTType != null) {
                                readTType.addInterceptor(tTypeCommonInterceptor);
                            }
                        }
                    }
                    // TProtocol.readMessageEnd()
                    final InstrumentMethod readMessageEnd = target.getDeclaredMethod("readMessageEnd");
                    if (readMessageEnd != null) {
                        String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.tprotocol.server.TProtocolReadMessageEndInterceptor";
                        readMessageEnd.addInterceptor(interceptor);
                    }

                    // for async processors
                    target.addField(ThriftConstants.FIELD_ACCESSOR_ASYNC_MARKER_FLAG);
                    // TProtocol.readMessageBegin()
                    final InstrumentMethod readMessageBegin = target.getDeclaredMethod("readMessageBegin");
                    if (readMessageBegin != null) {
                        String interceptor = "com.navercorp.pinpoint.plugin.thrift.interceptor.tprotocol.server.TProtocolReadMessageBeginInterceptor";
                        readMessageBegin.addInterceptor(interceptor);
                    }
                }

                return target.toBytecode();
            }

        });
    }

    private void addTProtocolDecoratorEditor() {
        transformTemplate.transform("org.apache.thrift.protocol.TProtocolDecorator", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined,
                                        ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                target.addGetter(ThriftConstants.FIELD_GETTER_T_PROTOCOL, "concreteProtocol");
                return target.toBytecode();
            }
        });
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
