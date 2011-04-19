package org.fusesource.fabric.dosgi;

import java.io.IOException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.fusesource.fabric.dosgi.io.Transport;
import org.fusesource.fabric.dosgi.io.TransportAcceptListener;
import org.fusesource.fabric.dosgi.io.TransportListener;
import org.fusesource.fabric.dosgi.io.TransportServer;
import org.fusesource.fabric.dosgi.tcp.LengthPrefixedCodec;
import org.fusesource.fabric.dosgi.tcp.TcpTransport;
import org.fusesource.fabric.dosgi.tcp.TcpTransportFactory;
import org.fusesource.fabric.dosgi.util.UuidGenerator;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: 4/18/11
 * Time: 2:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class TcpTest {

    TcpTransportFactory factory = new TcpTransportFactory();
    TransportServer server;
    Transport client = null;
    DispatchQueue queue = Dispatch.createQueue();

    XStream xStream = new XStream(new StaxDriver());

    @Test
    public void testTcp() throws Exception {

        try {
            server = factory.bind("tcp://0.0.0.0:4142");
            server.setDispatchQueue(queue);
            server.setAcceptListener(new TransportAcceptListener() {
                public void onAccept(TransportServer transportServer, final TcpTransport transport) {
                    System.err.println("Server: onAccept: " + transport);
                    transport.setDispatchQueue(queue);
                    transport.setProtocolCodec(new LengthPrefixedCodec());
                    transport.setTransportListener(new TransportListener() {
                        public void onTransportCommand(Transport transport, Object command) {
                            System.err.println("Server: onTransportCommand: " + command);
                            RemoteRequest req = (RemoteRequest) xStream.fromXML(command.toString());
                            String str = xStream.toXML(new RemoteResponse(req.correlation, "The response"));
                            transport.offer(str);
                        }

                        public void onRefill(Transport transport) {
                            System.err.println("Server: onRefill");
                        }

                        public void onTransportFailure(Transport transport, IOException error) {
                            System.err.println("Server: onTransportFailure: " + error);
                            error.printStackTrace();
                        }

                        public void onTransportConnected(Transport transport) {
                            System.err.println("Server: onTransportConnected");
                            transport.resumeRead();
                        }

                        public void onTransportDisconnected(Transport transport) {
                            System.err.println("Server: onTransportDisconnected");
                        }
                    });
                    transport.start();
                }
                public void onAcceptError(TransportServer transportServer, Exception error) {
                    System.err.println("Server: onAcceptError: " + error);
                    error.printStackTrace();
                }
            });

            server.start(new Runnable() {
                public void run() {
                    try {
                        System.err.println("Server ready at: " + server.getConnectAddress());

                        client = factory.connect(server.getConnectAddress());
                        client.setDispatchQueue(queue);
                        client.setProtocolCodec(new LengthPrefixedCodec());
                        client.setTransportListener(new TransportListener() {
                            public void onTransportCommand(Transport transport, Object command) {
                                System.err.println("Client: onTransportCommand: " + command);
                                Object obj = xStream.fromXML(command.toString());
                            }

                            public void onRefill(Transport transport) {
                                System.err.println("Client: onRefill");
                            }

                            public void onTransportFailure(Transport transport, IOException error) {
                                System.err.println("Client: onTransportFailure: " + error);
                                error.printStackTrace();
                            }

                            public void onTransportConnected(Transport transport) {
                                System.err.println("Client: onTransportConnected");
                                client.resumeRead();
                                String str = xStream.toXML(new RemoteRequest("id", "method", new Object[]{"foo", "bar"}));
                                client.offer(str);
                            }

                            public void onTransportDisconnected(Transport transport) {
                                System.err.println("Client: onTransportDisconnected");
                            }
                        });
                        client.start();
//                        client.resumeRead();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


            Thread.sleep(1000000);


        } finally {
            try {
                server.stop();
            } catch (Throwable t) {
            }
            try {
                client.stop();
            } catch (Throwable t) {
            }
        }
    }

    static public class RemoteRequest {
        public String correlation;
        public String serviceId;
        public String method;
        public Object[] args;

        public RemoteRequest() {
        }

        public RemoteRequest(String serviceId, String method, Object[] args) {
            this.correlation = UuidGenerator.getUUID();
            this.serviceId = serviceId;
            this.method = method;
            this.args = args;
        }
    }

    static public class RemoteResponse {
        public String correlation;
        public Object value;

        public RemoteResponse() {
        }

        public RemoteResponse(String correlation, Object value) {
            this.correlation = correlation;
            this.value = value;
        }
    }

}
