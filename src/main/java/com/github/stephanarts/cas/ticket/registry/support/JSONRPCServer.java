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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONException;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Poller;

/**
 * JSONRPCServer Class.
 */
public class JSONRPCServer extends Thread {

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
     */
    private final Socket  socket;

    /**
     * ZMQ Control Socket.
     */
    private final Socket  controlSocket;


    /**
     * BindURI.
     */
    private final String  bindUri;


    /**
     * Hashmap of Methods.
     */
    private final HashMap<String, IMethod> methodMap;

    private static int NR = 0;

    private static Object NRLOCK = new Object();

    private final int nr;


    /**
     * Create a JSONRPCServer object.
     *
     * @param bindUri   The URI to listen on
     */
    public JSONRPCServer(final String bindUri) {

        this.context = ZMQ.context(1);

        this.socket = this.context.socket(ZMQ.ROUTER);
        this.controlSocket = this.context.socket(ZMQ.PULL);

        this.bindUri = bindUri;

        this.setName("JSONRPCServer");

        this.methodMap = new HashMap<String, IMethod>();
        synchronized(this.NRLOCK) {
            this.NR++;
            this.nr = this.NR;
        }
    }

    /**
     * Register a JSONRPCFunction.
     *
     * @param name                    Method Name.
     * @param method                  Method Class
     * @throws JSONRPCException       Exception
     */
    public final void registerMethod(
            final String  name,
            final IMethod method) throws JSONRPCException {

        /**
         * Check if a method with this name is already
         * registered.
         */
        if(this.methodMap.containsKey(name)) {
            throw new JSONRPCException(-1, "Method already registered");
        }

        this.methodMap.put(name, method);
    }

    /**
     * Run the server.
     */
    public final void run() {

        ZMsg   message;
        ZFrame body;
        String msg;

        String resp = null;

        Poller items = new Poller(2);

        /** Enter the main event-loop */
        items.register(this.socket, Poller.POLLIN);
        int i = items.register(this.controlSocket, Poller.POLLIN);

        logger.debug("Registered controlSocket as item: "+i);

        logger.debug("Entering main event-loop ["+this.nr+"]");
        /** Enter the main event-loop */
        while(!Thread.currentThread().isInterrupted()) {
            items.poll();
            logger.debug("["+this.nr+"] - got input");

            /**
             * TODO
             *
             * Don't assume indexes, properly use the ones returned
             * by the items.register function.
             */
            if(items.pollin(1)) {
                message = ZMsg.recvMsg(controlSocket);
                logger.debug("Received STOP message [" + this.nr + "]");
                break;
            }
            if(items.pollin(0)) {
                message = ZMsg.recvMsg(socket);
                body = message.getLast();
                byte[] d = body.getData();
                if (d.length == 1 && d[0] == 0x0) {
                    /* Send pong */
                    message.removeLast();
                    message.addLast(new byte[] {0x0});
                    message.send(this.socket);
                } else {
                    msg = new String(body.getData());

                    logger.debug("Got a message");

                    try {
                        resp = handleJSONRPC(msg);

                        logger.debug("Sent a reply");
                        message.removeLast();
                        message.addString(resp);
                        message.send(this.socket);
                    } catch(final Exception e) {
                        logger.warn(e.toString());
                    }
                }
            }

        }

        logger.debug("Closing context ["+this.nr+"]");
        this.socket.setLinger(0);
        this.controlSocket.setLinger(0);

        this.controlSocket.close();
        this.socket.close();
        this.context.close();
    }

    /**
     * Handle JSONRPC call.
     *
     * @param msg     String message of the request.
     *
     * @return        String message of the response.
     */
    protected final String handleJSONRPC(final String msg) {
        JSONObject response = new JSONObject();
        JSONObject result;
        JSONObject error = null;

        JSONObject request;

        String     methodName;
        String     methodId = null;

        IMethod    method;

        JSONObject params;

        response.put("jsonrpc", "2.0");

        try {
            request = new JSONObject(msg);

            validateJSONRPC(request);

            /**
             * Get the methodId, required for sending a response.
             */
            methodId = request.optString("id");

            methodName = request.getString("method");
            if (!this.methodMap.containsKey(methodName)) {
                /**
                 * code = -32601
                 * msg = Method not Found
                 */
                throw new JSONRPCException(
                        -32601,
                        "Method not Found");
            }

            method = this.methodMap.get(methodName);
            if (method == null) {
                /**
                 * code = -32601
                 * msg = Method not Found
                 */
                throw new JSONRPCException(
                        -32601,
                        "Method not Found");
            }

            params = request.getJSONObject("params");

            result = method.execute(params);

            if(methodId != null) {
                response.put("id", methodId);
                response.put("result", result);
            }
        } catch (final JSONException e) {
            response.put("id", methodId);

            error = new JSONObject();
            response.put("error", error);
            error.put("code", -32700);
            error.put("message", "Parse error");
            logger.warn("Parse error");
            logger.warn(e.getMessage());
        } catch (final JSONRPCException e) {
            response.put("id", methodId);

            error = new JSONObject();
            response.put("error", error);
            error.put("code", e.getCode());
            error.put("message", e.getMessage());
            logger.warn(e.getMessage());
        } catch (final Exception e) {
            response.put("id", methodId);

            error = new JSONObject();
            response.put("error", error);
            error.put("code", -32603);
            error.put("message", "Internal error");
            logger.warn("Internal error");
        }

        if (methodId == null && error == null) {
            return null;
        }

        return response.toString();
    }
    

    /**
     * Validate JSONRPC call.
     *
     * @param  request    JSONRPC request object
     *
     * @throws JSONRPCException Throws exception if request object contains
     * malformed or unsupported json-rpc
     *
     * Batch calls or array-style parameters are not supported.
     *
     */
    protected final void validateJSONRPC(final JSONObject request)
            throws JSONRPCException {

        JSONObject params;

        JSONObject method;

        if(!request.has("jsonrpc")) {
            /**
             * code = -32600
             * msg = Invalid Request
             */
            throw new JSONRPCException(
                    -32600,
                    "Invalid Request");
        } else {
            if(!request.getString("jsonrpc").equals("2.0")) {
                /**
                 * code = -32600
                 * msg = Invalid Request
                 */
                throw new JSONRPCException(
                        -32600,
                        "Invalid Request");
            }
        }

        if(!request.has("params")) {
            /**
             * code = -32600
             * msg = Invalid Request
             */
            throw new JSONRPCException(
                    -32600,
                    "Invalid Request");
        }

        /**
         * We only support named params at the moment.
         */
        params = request.getJSONObject("params");
        if(params == null) {
            /**
             * code = -32600
             * msg = Invalid Request
             */
            throw new JSONRPCException(
                    -32600,
                    "Invalid Request");
        }

        if(!request.has("method")) {
            /**
             * code = -32600
             * msg = Invalid Request
             */
            throw new JSONRPCException(
                    -32600,
                    "Invalid Request");
        }

    }


    /**
     * Send a 'stop' message to the control socket.
     *
     */
    public final void interrupt() {
        byte[] msg = new byte[1];
        Socket s = this.context.socket(ZMQ.PUSH);
        s.connect("inproc://jsonrpc-"+this.nr);
        s.send(msg, ZMQ.NOBLOCK);
        s.close(); 
        logger.debug("Sent a STOP Message to inproc://jsonrpc-"+this.nr);
        //super.interrupt();
    }

    /**
     * Start the JSONRPCServer thread.
     *
     * Binds the sockets before calling Thread.start(), this way
     * they are already bound before the first run() is executed.
     */
    public final void start() {
        /** Bind Socket */
        logger.debug("Binding Sockets ["+this.nr+"]");
        this.socket.bind(this.bindUri);
        this.controlSocket.bind("inproc://jsonrpc-"+this.nr);

        logger.debug("Starting JSONRPCServer ["+this.nr+"]");
        super.start();
    }
}
