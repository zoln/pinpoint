## Pinpoint JBoss plugin configuration

### Known Issue
There is a bug in our ASM engine in 1.6.0. In order to trace jboss in 1.6.0, **you must set `profiler.instrument.engine=JAVASSIST` in pinpoint.config**.

###  Standalone mode <br/>
 Add following configuration in __standalone.conf__ :- <br/>
```bash 
JAVA_OPTS="$JAVA_OPTS -Djboss.modules.system.pkgs=org.jboss.byteman,com.navercorp.pinpoint.bootstrap,
com.navercorp.pinpoint.common,com.navercorp.pinpoint.exception"
JAVA_OPTS="$JAVA_OPTS -javaagent:$PINPOINT_AGENT_HOME/pinpoint-bootstrap-$PINPOINT_VERSION.jar"
JAVA_OPTS="$JAVA_OPTS -Dpinpoint.applicationName=APP-APPLICATION-NAME" 
JAVA_OPTS="$JAVA_OPTS -Dpinpoint.agentId=APP-AGENTID"
```

###  Domain mode <br/>

* Add below configuration in __domain.xml__ :- <br/>
```xml 
 <system-properties>
     ...
    <property name="jboss.modules.system.pkgs" value="com.navercorp.pinpoint.bootstrap,
com.navercorp.pinpoint.common,com.navercorp.pinpoint.exception" boot-time="true"/>
    ...
</system-properties>
```
* Add below configuration in __host.xml__ :- <br/>

```xml 
<servers>
    ...
    <server name="server-one" group="main-server-group">
        ...
        <jvm name="default">
            ...
            <jvm-options>
                ...
                <option value="-javaagent:$PINPOINT_AGENT_HOME/pinpoint-bootstrap-$PINPOINT_VERSION.jar"/>
                <option value="-Dpinpoint.applicationName=APP-APPLICATION-NAME"/>
                <option value="-Dpinpoint.agentId=APP-AGENT-1"/>
            </jvm-options>
        </jvm>
        ...
    </server>
    
    <server name="server-two" group="main-server-group" auto-start="true">
            ...
            <jvm name="default">
                ...
                <jvm-options>
                    ...
                    <option value="-javaagent:$PINPOINT_AGENT_HOME/pinpoint-bootstrap-$PINPOINT_VERSION.jar"/>
                    <option value="-Dpinpoint.applicationName=APP-APPLICATION-NAME"/>
                    <option value="-Dpinpoint.agentId=APP-AGENT-2"/>
                </jvm-options>
            </jvm>
            ...
        </server>
        
    
</servers> 

```

#### Set ```profiler.jboss.traceEjb=true``` for remote ejb based application in pinpoint.config file
#### Set ```profiler.jboss.traceEjb=false``` for non-ejb based application in pinpoint.config file
