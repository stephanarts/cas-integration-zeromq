/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.stephanarts.cas.ticket.registry.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMsg;
import org.zeromq.ZFrame;

import com.github.stephanarts.cas.ticket.registry.support.PaceMaker;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
 * Unit test for PaceMaker.
 */
@RunWith(JUnit4.class)
public class PaceMakerTest 
{

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeClass
    public static void beforeTest() {
    }

    @AfterClass
    public static void afterTest() {
    }


    @Test
    public void testAddClient() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:1234");

        PaceMaker p = new PaceMaker();
        p.addClient(c);

        Assert.assertEquals(1, p.getClientCount());

        p.removeClient(c);

        Assert.assertEquals(0, p.getClientCount());
    }

}
