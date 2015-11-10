Camel Router Spring Project
===========================

To build this project use

    mvn install

To run this project with Maven use

    mvn camel:run

For more help see the Apache Camel documentation

    http://camel.apache.org/
    

OV added:
Starting with the possibility to attach JConsole:
1) start the VM with remote monitoring enabled:
   F:\veits\Kunden\252_CCS_Support (offer accepted)\Alpha3\CloudCdcConverter\micro-services>"C:\Program Files\Java\jdk1.7.0_09\bin\java.exe" -Dorg.apache.camel.jmx.createRmiConnector=true -jar camel-spring2.jar

2) start JConsole or better jvisualvm.exe: 
   C:\Program Files\Java\jdk1.7.0_09\bin\jconsole.exe
   or better
   C:\Program Files\Java\jdk1.7.0_09\bin\jvisualvm.exe
   
3) connect to remote:
   in jconsole, upon startup, select remove and paste the following URI into the input field:
   service:jmx:rmi:///jndi/rmi://DE04058W:1099/jmxrmi/camel (or the corresponding URI shown during startup in 1)
   in jvisualvm.exe, menu -> File -> Add JMX connection and add the URI
   ervice:jmx:rmi:///jndi/rmi://DE04058W:1099/jmxrmi/camel (or the corresponding URI shown during startup in 1)
     
