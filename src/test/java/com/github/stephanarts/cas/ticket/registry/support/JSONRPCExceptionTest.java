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
