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
package com.navercorp.pinpoint.profiler.plugin.xml.transformer;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;

/**
 * @author Jongho Moon
 *
 */
public class ClassWeaver implements ClassRecipe {
    private final String aspectClassName;

    public ClassWeaver(String aspectClassName) {
        this.aspectClassName = aspectClassName;
    }

    @Override
    public void edit(ClassLoader classLoader, InstrumentClass target) throws InstrumentException {
        target.weave(aspectClassName);
    }

    @Override
    public String toString() {
        return "ClassWeaver[aspect=" + aspectClassName + "]";
    }
}
