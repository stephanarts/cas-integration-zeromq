# cas-integration-zeromq #

ZeroMQ/JSON-RPC Ticket-Registry-Backend For WAN Clustering

[![Build Status](https://travis-ci.org/stephanarts/cas-integration-zeromq.svg?branch=master)](https://travis-ci.org/stephanarts/cas-integration-zeromq)
[![Coverage Status](https://coveralls.io/repos/stephanarts/cas-integration-zeromq/badge.png)](https://coveralls.io/r/stephanarts/cas-integration-zeromq)

For a full list of changes in each release, view the [ChangeLog](CHANGELOG.md).

## Design Goals ##

  -  Simple Configuration - (identical across all cluster nodes)
  -  Automatic cluster synchronisation


## Sample Configuration ##

        <?xml version="1.0" encoding="UTF-8"?>
        <beans xmlns="http://www.springframework.org/schema/beans"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:p="http://www.springframework.org/schema/p"
                xsi:schemaLocation="http://www.springframework.org/schema/beans
                http://www.springframework.org/schema/beans/spring-beans.xsd">
            <description>
            Configuration for the ZMQ TicketRegistry which stores the tickets.
            </description>
        
            <bean id="ticketRegistry"
                    class="com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry">
                <constructor-arg name="providers">
                    <list>
                        <value>tcp://192.168.0.1:5555</value>
                        <value>tcp://192.168.0.2:5555</value>
                    </list>
                </constructor-arg>
                <constructor-arg name="bindUri" value="tcp://*:5555" />
                <constructor-arg name="requestTimeout" value="1500" />
                <constructor-arg name="heartbeatInterval" value="200" />
            </bean>
        
            <!--Quartz -->
            <!-- TICKET REGISTRY CLEANER -->
            <bean id="ticketRegistryCleaner"
                    class="org.jasig.cas.ticket.registry.support.DefaultTicketRegistryCleaner"
                    p:ticketRegistry-ref="ticketRegistry"
                    p:logoutManager-ref="logoutManager" />
        
            <bean id="jobDetailTicketRegistryCleaner"
                    class="org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean"
                    p:targetObject-ref="ticketRegistryCleaner"
                    p:targetMethod="clean" />
        
            <bean id="triggerJobDetailTicketRegistryCleaner"
                    class="org.springframework.scheduling.quartz.SimpleTriggerBean"
                    p:jobDetail-ref="jobDetailTicketRegistryCleaner"
                    p:startDelay="20000"
                    p:repeatInterval="5000000" />

        </beans>

