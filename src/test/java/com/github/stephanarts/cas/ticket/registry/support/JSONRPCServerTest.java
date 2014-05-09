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

import com.github.stephanarts.cas.ticket.registry.support.JSONRPCServer;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/*
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
*/

/**
 * Unit test for JSONRPCServer.
 */
public class JSONRPCServerTest
    extends TestCase
{

    private JSONRPCServer server;

    private Context context;

    private class TestMethod implements IMethod {
        public void execute(JSONObject params) {
            
        }
    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public JSONRPCServerTest( String testName ) {
        super( testName );
        this.context = ZMQ.context(1);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( JSONRPCServerTest.class );
    }

    protected void setUp() {
        this.server = new JSONRPCServer("tcp://localhost:6789");
        this.server.start();
    }

    protected void tearDown() {
        this.server.stop();
    }

    public void testRegisterMethod() throws Exception {
        boolean testcase_1 = false;
        boolean testcase_2 = false;
        boolean testcase_3 = false;

        try {
            this.server.registerMethod("test", new TestMethod());
            testcase_1 = true;
        } catch (JSONRPCException e) {
            testcase_1 = false;
        }

        Assert.assertTrue(testcase_1);

        try {
            this.server.registerMethod("test", new TestMethod());
            testcase_2 = true;
        } catch (JSONRPCException e) {
            testcase_2 = false;
        }

        Assert.assertFalse(testcase_2);

        try {
            this.server.registerMethod("test-a", new TestMethod());
            testcase_3 = true;
        } catch (JSONRPCException e) {
            testcase_3 = false;
        }

        Assert.assertTrue(testcase_3);
    }

    public void testNotification() throws Exception {
    }
}
