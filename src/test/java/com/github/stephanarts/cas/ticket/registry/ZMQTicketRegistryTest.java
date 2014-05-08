/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.stephanarts.cas.ticket.registry;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Assert;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for ZMQTicketRegistry.
 */
public class ZMQTicketRegistryTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ZMQTicketRegistryTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ZMQTicketRegistryTest.class );
    }

    public void testWriteGetDelete() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);
        String[] addresses = {"tcp://localhost:5555"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(addresses,"tcp://localhost:5555", 30, 30);

        registry.addTicket(ticket);

        final ServiceTicket ticketFromRegistry = (ServiceTicket) registry.getTicket(ticketId);
        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(ticketId, ticketFromRegistry.getId());
        registry.deleteTicket(ticketId);
        Assert.assertNull(registry.getTicket(ticketId));
    }
}
