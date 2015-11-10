package com.mycompany.camel.spring2;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test the cahce when reloading .tm files in the classpath
 */
public class VelocityTemplateContentCacheTest extends CamelTestSupport {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create a tm file in the classpath as this is the tricky reloading stuff
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Hello ${headers.name}", Exchange.FILE_NAME, "hello.tm");
    }
    
    @Override
    public boolean useJmx() {
        return true;
    }

    @Test
    public void testNotCached_static() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello London");

        template.sendBodyAndHeader("direct:static_a", "Body", "name", "London");
        mock.assertIsSatisfied();

        // now change content in the file in the classpath and try again
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");

        mock.reset();
        mock.expectedBodiesReceived("Bye Paris");

        template.sendBodyAndHeader("direct:static_a", "Body", "name", "Paris");
        mock.assertIsSatisfied();
    }
    
    @Test
    public void testNotCached_CamelVelocityResourceUri() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello London");

        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_a", "Body", "name", "London");
        mock.assertIsSatisfied();

        // now change content in the file in the classpath and try again
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");

        mock.reset();
        mock.expectedBodiesReceived("Bye Paris");

        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_a", "Body", "name", "Paris");
        mock.assertIsSatisfied();
    }

    @Test
    public void testCached_static() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello London");

        template.sendBodyAndHeader("direct:static_b", "Body", "name", "London");
        mock.assertIsSatisfied();

        // now change content in the file in the classpath and try again
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");

        mock.reset();
        // we must expected the original filecontent as the cache is enabled, so its Hello and not Bye
        mock.expectedBodiesReceived("Hello Paris");

        template.sendBodyAndHeader("direct:static_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
    }
    
    @Test
    public void testCached_CamelVelocityResourceUri() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello London");

        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_b", "Body", "name", "London");
        mock.assertIsSatisfied();

        // now change content in the file in the classpath and try again
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");

        mock.reset();
        // we must expected the original filecontent as the cache is enabled, so its Hello and not Bye
        mock.expectedBodiesReceived("Hello Paris");

        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
    }

    @Test
    public void testClearCacheViaJmx_static_endpoint() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello London");

        template.sendBodyAndHeader("direct:static_b", "Body", "name", "London");
        mock.assertIsSatisfied();

        // now change content in the file in the classpath and try again
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");

        mock.reset();
        // we must expected the original filecontent as the cache is enabled, so its Hello and not Bye
        //mock.expectedBodiesReceived("Hello Paris");

        template.sendBodyAndHeader("direct:static_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
        
        // clear the cache using jmx
        MBeanServer mbeanServer = context.getManagementStrategy().getManagementAgent().getMBeanServer();
        Set<ObjectName> objNameSet = mbeanServer.queryNames(new ObjectName("org.apache.camel:type=endpoints,name=\"velocity:*contentCache=true*\",*"), null);
//        Set<ObjectName> objNameSet = mbeanServer.queryNames(new ObjectName("org.apache.camel:type=endpoints,name=\"velocity:*enabled*\",*"), null);
        ObjectName managedObjName = new ArrayList<ObjectName>(objNameSet).get(0);
        mbeanServer.invoke(managedObjName, "clearContentCache", null, null);
           
        mock.reset();
        // we expect that the new resource will be set as the cached value, since the cache has been cleared
        mock.expectedBodiesReceived("Bye Paris");
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");
        template.sendBodyAndHeader("direct:static_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
        
        mock.reset();
        // we expect that the cached value will not be replaced by a different resource since the cache is now re-established
        mock.expectedBodiesReceived("Bye Paris");
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Hello ${headers.name}", Exchange.FILE_NAME, "hello.tm");
        template.sendBodyAndHeader("direct:static_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
    }
    
    @Test
    public void testClearCacheViaJmx_CamelVelocityResourceUri() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived("Hello London");

        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_b", "Body", "name", "London");
        mock.assertIsSatisfied();

        // now change content in the file in the classpath and try again
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");

        mock.reset();
        // we must expected the original filecontent as the cache is enabled, so its Hello and not Bye
        //mock.expectedBodiesReceived("Hello Paris");

        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
        
        // clear the cache using jmx
        MBeanServer mbeanServer = context.getManagementStrategy().getManagementAgent().getMBeanServer();
        // does not find the right endpoint:
//        Set<ObjectName> objNameSet = mbeanServer.queryNames(new ObjectName("org.apache.camel:type=endpoints,name=\"velocity:*contentCache=true*\",*"), null);
        // seems to find the endpoint, but cache is not cleared:
        Set<ObjectName> objNameSet = mbeanServer.queryNames(new ObjectName("org.apache.camel:type=endpoints,name=\"velocity:*dummy_with_contentCache_enabled\",*"), null);
        // does not find any endpoint:
//        Set<ObjectName> objNameSet = mbeanServer.queryNames(new ObjectName("org.apache.camel:type=endpoints,name=\"velocity:dummy_with_contentCache_enabled\",*"), null);
        ObjectName managedObjName = new ArrayList<ObjectName>(objNameSet).get(0);
        mbeanServer.invoke(managedObjName, "clearContentCache", null, null);
           
        mock.reset();
        // we expect that the new resource will be set as the cached value, since the cache has been cleared
        mock.expectedBodiesReceived("Bye Paris");
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Bye ${headers.name}", Exchange.FILE_NAME, "hello.tm");
        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
        
        mock.reset();
        // we expect that the cached value will not be replaced by a different resource since the cache is now re-established
        mock.expectedBodiesReceived("Bye Paris");
        template.sendBodyAndHeader("file://target/test-classes/org/apache/camel/component/stringtemplate", "Hello ${headers.name}", Exchange.FILE_NAME, "hello.tm");
        template.sendBodyAndHeader("direct:CamelVelocityResourceUri_b", "Body", "name", "Paris");
        mock.assertIsSatisfied();
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
            	
// works fine (static velocity URI):
                from("direct:static_a").to("velocity://org/apache/camel/component/stringtemplate/hello.tm?contentCache=false").to("mock:result");

                from("direct:static_b").to("velocity://org/apache/camel/component/stringtemplate/hello.tm?contentCache=true").to("mock:result");

// does not work with recipientlists (cannot find object in JMX):           	
//                from("direct:a").setHeader(
//						"recipientList",
//						simple("velocity://org/apache/camel/component/stringtemplate/hello.tm?contentCache=false"))
//				.recipientList(header("recipientList"), ",")
//				.to("mock:result");
//
//                from("direct:b").setHeader(
//						"recipientList",
//						simple("velocity://org/apache/camel/component/stringtemplate/hello.tm?contentCache=true"))
//				.recipientList(header("recipientList"), ",")
//				.to("mock:result");
            	
// trying with dynamic velocity URI w/o recipient list:
              from("direct:CamelVelocityResourceUri_a")
                .setHeader("CamelVelocityResourceUri").constant("//org/apache/camel/component/stringtemplate/hello.tm?contentCache=false")
              	.to("velocity:dummy_with_contentCache_disabled").to("mock:result");

              from("direct:CamelVelocityResourceUri_b")
              	.setHeader("CamelVelocityResourceUri").constant("//org/apache/camel/component/stringtemplate/hello.tm?contentCache=true")
              	.to("velocity:dummy_with_contentCache_enabled").to("mock:result");
            	
            }
        };
    }
}