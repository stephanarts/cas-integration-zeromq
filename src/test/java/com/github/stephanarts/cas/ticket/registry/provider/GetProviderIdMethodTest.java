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

package com.github.stephanarts.cas.ticket.registry.provider;

import java.util.UUID;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.json.JSONObject;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.provider.GetProviderIdMethod;
import com.github.stephanarts.cas.ticket.registry.support.IMethod;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for GetMethod.
 */
@RunWith(JUnit4.class)
public class GetProviderIdMethodTest
{
    @Test
    public void testValidInput() throws Exception {
        final JSONObject params = new JSONObject();
        final String providerId = UUID.randomUUID().toString();

        final IMethod method = new GetProviderIdMethod(providerId);
        final JSONObject result;

        try {
            result = method.execute(params);

            Assert.assertTrue(result.has("provider-id"));

            Assert.assertTrue(
                    providerId.equals(
                            result.getString("provider-id")
                    ));
        } catch (final JSONRPCException e) {
            Assert.fail(e.getMessage());
        } catch (final Exception e) {
            throw new Exception(e);
        }
    }

    @Test
    public void testInvalidParameters() throws Exception {
        final JSONObject params = new JSONObject();
        final String providerId = UUID.randomUUID().toString();

        final IMethod method = new GetProviderIdMethod(providerId);
        final JSONObject result;

        params.put("invalid-param", "MUST_FAIL"); 

        try {
            result = method.execute(params);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(-32602, e.getCode());
            Assert.assertTrue(e.getMessage().equals("Invalid Params"));
            return;
        }

        Assert.fail("No Exception Thrown");
    }

}
