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

package com.navercorp.pinpoint.plugin.commons.dbcp2;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

import java.security.ProtectionDomain;

public class CommonsDbcp2Plugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    public static final ServiceType DBCP2_SERVICE_TYPE = ServiceTypeFactory.of(6052, "DBCP2");
    public static final String DBCP2_SCOPE = "DBCP2_SCOPE";

    private CommonsDbcp2Config config;

    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        config = new CommonsDbcp2Config(context.getConfig());
        if (!config.isPluginEnable()) {
            logger.info("Disable commons dbcp option. 'profiler.jdbc.dbcp2=false'");
            return;
        }

        addBasicDataSourceTransformer();
        if (config.isProfileClose()) {
            addPoolGuardConnectionWrapperTransformer();
        }
    }

    private void addPoolGuardConnectionWrapperTransformer() {
        transformTemplate.transform("org.apache.commons.dbcp2.PoolingDataSource$PoolGuardConnectionWrapper", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addInterceptor("com.navercorp.pinpoint.plugin.commons.dbcp2.interceptor.DataSourceCloseInterceptor");
                return target.toBytecode();
            }
        });
    }

    private void addBasicDataSourceTransformer() {
        transformTemplate.transform("org.apache.commons.dbcp2.BasicDataSource", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
                target.addInterceptor("com.navercorp.pinpoint.plugin.commons.dbcp2.interceptor.DataSourceGetConnectionInterceptor");
                return target.toBytecode();
            }
        });
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }

}
