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

import com.github.stephanarts.cas.ticket.registry.support.WatchDog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Matchers.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for WatchDog.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(WatchDog.class)
public class WatchDogTest 
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
    public void testConstructor() throws Exception {
        WatchDog w = new WatchDog();
    }

    @Test
    public void testLifeCycle() throws Exception {
        WatchDog w = new WatchDog();
        w.start();
        Assert.assertTrue(w.isAlive());
        w.cleanup();
        Assert.assertFalse(w.isAlive());
    }

    @Test
    @Ignore
    public void testSleepInterrupt() throws Exception {
        WatchDog w = new WatchDog();

        //PowerMockito.mockStatic(Thread.class);
        //PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        PowerMockito.spy(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(anyLong());

        w.start();
        w.cleanup();

        Assert.assertFalse(w.isAlive());
    }

    @Test
    public void testDefaultHeartbeatInterval() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(5000, w.getHeartbeatInterval());

        w.cleanup();
    }

    @Test
    public void testDefaultHeartbeatTimeout() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(200, w.getHeartbeatTimeout());

        w.cleanup();
    }

    @Test
    public void testHeartbeatInterval() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(5000, w.getHeartbeatInterval());

        w.setHeartbeatInterval(2000);

        Assert.assertEquals(2000, w.getHeartbeatInterval());

        w.cleanup();
    }

    @Test
    public void testHeartbeatTimeout() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(200, w.getHeartbeatTimeout());

        w.setHeartbeatTimeout(400);

        Assert.assertEquals(400, w.getHeartbeatTimeout());

        w.cleanup();
    }

    @Test
    public void testFailedHeartbeat() throws Exception {
        boolean available;

        WatchDog w = new WatchDog();
        JSONRPCClient[] c = { new JSONRPCClient("tcp://localhost:6666", null)};

        w.setClients(c);
        

        w.setHeartbeatTimeout(100);
        w.setHeartbeatInterval(100);

        w.start();

        Thread.sleep(1000);
        available = c[0].getAvailable();
        if (available == true) {
            w.cleanup();
            Assert.fail("Watchdog did not set availability to false");
        }

        w.cleanup();
    }

}
