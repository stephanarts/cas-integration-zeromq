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

package com.github.stephanarts.cas.ticket.registry;

import org.junit.After;
import org.junit.Before;
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

import com.github.stephanarts.cas.ticket.registry.BootstrapException;

/*
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
*/

/**
 * Unit test for BootstrapException.
 */
@RunWith(JUnit4.class)
public class BootstrapExceptionTest
{

    /**
     * testBootstrapException().
     *
     * testBootstrapException throws a BootstrapException and catches it.
     * It then validates the Exception-message.
     *
     * This test fails when either the Exception is not thrown,
     * or the message does not correspond to the expected default.
     *
     * @throws Exception when a test-case fails, an exception is thrown.
     */
    @Test
    public void testBootstrapException() throws Exception {
        boolean exception_thrown = false;

        try {
            throw new BootstrapException();
        } catch (BootstrapException e) {
            exception_thrown = true;
            Assert.assertTrue(e.getMessage().equals("Bootstrapping Failed (unknown cause)"));
        }

        Assert.assertTrue(exception_thrown);
    }

    /**
     * testBootstrapExceptionCustomMessage().
     *
     * testBootstrapExceptionCustomMessage throws a BootstrapException
     * and catches it. It then validates the Exception-message.
     *
     * This test fails when either the Exception is not thrown,
     * or the message does not correspond to the value provided
     * through the constructor.
     *
     * @throws Exception when a test-case fails, an exception is thrown.
     */
    @Test
    public void testBootstrapExceptionCustomMessage() throws Exception {
        boolean exception_thrown = false;

        try {
            throw new BootstrapException("It failed -- really");
        } catch (BootstrapException e) {
            exception_thrown = true;
            Assert.assertTrue(e.getMessage().equals("It failed -- really"));
        }

        Assert.assertTrue(exception_thrown);
    }
}
