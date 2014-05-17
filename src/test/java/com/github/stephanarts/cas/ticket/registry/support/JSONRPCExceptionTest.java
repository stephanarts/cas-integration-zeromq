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
package com.github.stephanarts.cas.ticket.registry.support;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Assert;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMsg;

import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/*
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
*/

/**
 * Unit test for JSONRPCException.
 */
public class JSONRPCExceptionTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public JSONRPCExceptionTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( JSONRPCExceptionTest.class );
    }

    public void testJSONRPCException() throws Exception {
        boolean exception_thrown = false;

        try {
            throw new JSONRPCException(-31600, "Invalid Request");
        } catch (JSONRPCException e) {
            exception_thrown = true;
            Assert.assertEquals(e.getCode(), -31600);
            Assert.assertTrue(e.getMessage().equals("Invalid Request"));
        }

        Assert.assertTrue(exception_thrown);
    }
}
