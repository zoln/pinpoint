/*
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.arcus;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.async.AsyncTraceIdAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.plugin.arcus.filter.ArcusMethodFilter;
import com.navercorp.pinpoint.plugin.arcus.filter.FrontCacheMemcachedMethodFilter;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * 
 * @author jaehong.kim
 *
 */
public class ArcusPlugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ArcusPluginConfig config = new ArcusPluginConfig(context.getConfig());

        boolean arcus = config.isArcus();
        boolean memcached = config.isMemcached();

        if (arcus) {
            addArcusClientEditor(config);
            addCollectionFutureEditor();
            addFrontCacheGetFutureEditor();
            addFrontCacheMemcachedClientEditor(config);
            addCacheManagerEditor();

            // add none operation future. over 1.5.4
            addBTreeStoreGetFutureEditor();
            addCollectionGetBulkFutureEditor();
            addSMGetFutureFutureEditor();
        }

        if (arcus || memcached) {
            addMemcachedClientEditor(config);

            addBaseOperationImplEditor();
            addGetFutureEditor();
            addOperationFutureEditor();
            // add none operation future.
            addImmediateFutureEditor();
            addBulkGetFutureEditor();
        }
    }

    private void addArcusClientEditor(final ArcusPluginConfig config) {
        transformTemplate.transform("net.spy.memcached.ArcusClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                if (target.hasMethod("addOp", "java.lang.String", "net.spy.memcached.ops.Operation")) {
                    boolean traceKey = config.isArcusKeyTrace();

                    target.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.SetCacheManagerInterceptor");

                    for (InstrumentMethod m : target.getDeclaredMethods(new ArcusMethodFilter())) {
                        try {
                            m.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.ApiInterceptor", va(traceKey));
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Unsupported method " + className + "." + m.getName(), e);
                            }
                        }
                    }

                    return target.toBytecode();
                } else {
                    return null;
                }
            }
        });
    }

    private void addCacheManagerEditor() {
        transformTemplate.transform("net.spy.memcached.CacheManager", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField("com.navercorp.pinpoint.plugin.arcus.ServiceCodeAccessor");
                target.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.CacheManagerConstructInterceptor");
                return target.toBytecode();
            }

        });
    }

    private void addBaseOperationImplEditor() {
        transformTemplate.transform("net.spy.memcached.protocol.BaseOperationImpl", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addField("com.navercorp.pinpoint.plugin.arcus.ServiceCodeAccessor");
                return target.toBytecode();
            }

        });
    }

    private void addFrontCacheGetFutureEditor() {
        transformTemplate.transform("net.spy.memcached.plugin.FrontCacheGetFuture", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                target.addField("com.navercorp.pinpoint.plugin.arcus.CacheNameAccessor");
                target.addField("com.navercorp.pinpoint.plugin.arcus.CacheKeyAccessor");
                target.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.FrontCacheGetFutureConstructInterceptor");

                InstrumentMethod get0 = target.getDeclaredMethod("get", new String[]{"long", "java.util.concurrent.TimeUnit"});
                get0.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.FrontCacheGetFutureGetInterceptor");

                InstrumentMethod get1 = target.getDeclaredMethod("get", new String[0]);
                get1.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.FrontCacheGetFutureGetInterceptor");

                return target.toBytecode();
            }

        });
    }

    private void addFrontCacheMemcachedClientEditor(final ArcusPluginConfig config) {
        transformTemplate.transform("net.spy.memcached.plugin.FrontCacheMemcachedClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                boolean traceKey = config.isMemcachedKeyTrace();

                for (InstrumentMethod m : target.getDeclaredMethods(new FrontCacheMemcachedMethodFilter())) {
                    try {
                        m.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.ApiInterceptor", va(traceKey));
                    } catch (Exception e) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unsupported method " + className + "." + m.getName(), e);
                        }
                    }
                }

                return target.toBytecode();
            }

        });
    }

    private void addMemcachedClientEditor(final ArcusPluginConfig config) {
        transformTemplate.transform("net.spy.memcached.MemcachedClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                if (target.hasDeclaredMethod("addOp", new String[]{"java.lang.String", "net.spy.memcached.ops.Operation"})) {
                    target.addField("com.navercorp.pinpoint.plugin.arcus.ServiceCodeAccessor");
                    target.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.AddOpInterceptor");
                }

                boolean traceKey = config.isMemcachedKeyTrace();

                for (InstrumentMethod m : target.getDeclaredMethods(new FrontCacheMemcachedMethodFilter())) {
                    try {
                        m.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.ApiInterceptor", va(traceKey));
                    } catch (Exception e) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unsupported method " + className + "." + m.getName(), e);
                        }
                    }
                }

                return target.toBytecode();
            }

        });
    }

    private static final TransformCallback FUTURE_TRANSFORMER = new TransformCallback() {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            target.addField("com.navercorp.pinpoint.plugin.arcus.OperationAccessor");
            target.addField(AsyncTraceIdAccessor.class.getName());

            // setOperation
            InstrumentMethod setOperation = target.getDeclaredMethod("setOperation", new String[] { "net.spy.memcached.ops.Operation" });
            if (setOperation != null) {
                setOperation.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.FutureSetOperationInterceptor");
            }

            // cancel, get, set
            for (InstrumentMethod m : target.getDeclaredMethods(MethodFilters.name("cancel", "get", "set", "signalComplete"))) {
                m.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.FutureGetInterceptor");
            }

            return target.toBytecode();
        }
    };

    private static final TransformCallback INTERNAL_FUTURE_TRANSFORMER = new TransformCallback() {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            target.addField(AsyncTraceIdAccessor.class.getName());
            
            // cancel, get, set
            for (InstrumentMethod m : target.getDeclaredMethods(MethodFilters.name("cancel", "get"))) {
                m.addInterceptor("com.navercorp.pinpoint.plugin.arcus.interceptor.FutureInternalMethodInterceptor");
            }

            return target.toBytecode();
        }
    };

    
    private void addCollectionFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.CollectionFuture", FUTURE_TRANSFORMER);
    }

    private void addGetFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.GetFuture", FUTURE_TRANSFORMER);
    }

    private void addOperationFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.OperationFuture", FUTURE_TRANSFORMER);
    }

    private void addImmediateFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.ImmediateFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addBulkGetFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.BulkGetFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addBTreeStoreGetFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.BTreeStoreAndGetFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addCollectionGetBulkFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.CollectionGetBulkFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addSMGetFutureFutureEditor() {
        transformTemplate.transform("net.spy.memcached.internal.SMGetFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}