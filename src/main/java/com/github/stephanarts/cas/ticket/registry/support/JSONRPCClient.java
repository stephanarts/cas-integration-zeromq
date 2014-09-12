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

import org.json.JSONObject;
import org.json.JSONException;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMsg;

/**
 * JSONRPCClient Class.
 */
public class JSONRPCClient {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * ZMQ Context.
     */
    private final Context context;

    /**
     * ZMQ Socket.
    private final Socket  socket;
     */

    /**
     * ConnectUri.
     */
    private final String  connectUri;

    /**
     * Request ID nr.
     */
    private int           id;

    /**
     * RequestTimeout.
     */
    private final int requestTimeout;

    /**
     * Create a JSONRPCClient object.
     *
     * @param connectUri   The URI to connect to
     */
    public JSONRPCClient(final String connectUri) {

        this.context = ZMQ.context(1);

        //this.socket = this.context.socket(ZMQ.REQ);
        this.connectUri = connectUri;

        this.requestTimeout = 1500;
    }

    /**
     * Call a JSON-RPC method.
     *
     * @param method      String containing method-name
     * @param params      JSONObject containing call-parameters.
     *
     * @return            JSONObject containing call-result
     *
     * @throws JSONRPCException Throws JSONRPCException if an error occurs
     *                          these can be related to JSONRPC, or the application.
     */
    public final JSONObject call(final String method, final JSONObject params)
            throws JSONRPCException {

        JSONObject request = new JSONObject();
        JSONObject error;
        JSONObject result;
        JSONObject response;
        this.id++;

        Socket socket = this.context.socket(ZMQ.REQ);

        request.put("jsonrpc", "2.0");
        request.put("id", this.id);
        request.put("method", method);
        request.put("params", params);

        socket.connect(this.connectUri);
        socket.send(request.toString(), 0);

        PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
        int rc = ZMQ.poll(items, this.requestTimeout);
        if(rc == -1) {
            socket.close();
            throw new JSONRPCException(1, "AAA");
            //return null;
        }

        if(items[0].isReadable()) {
            // We got a reply from the server, must match sequence
            ZMsg message = ZMsg.recvMsg(socket);

            try {
                response = new JSONObject(new String(message.getLast().getData()));
            } catch(final JSONException e) {
                socket.close();
                throw new JSONRPCException(-32500, "Parse error");
            }
            if (response.has("result")) {
                result = response.getJSONObject("result");
                socket.close();
                return result;
            }
            if (response.has("error")) {
                error = response.getJSONObject("error");
                socket.close();
                throw new JSONRPCException(error.getInt("code"), error.getString("message"));
            }

            socket.close();
            throw new JSONRPCException(-32603, "Internal error");
        } else {
            logger.debug("Failed to get reply from {}", this.connectUri);
            socket.close();
        }

        throw new JSONRPCException(-10, "Connection not readable.");
        //return null;
    }

}
