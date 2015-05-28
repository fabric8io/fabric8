package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.watch.WatchEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractWatcher<T extends HasMetadata> implements Watcher<T> {

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesClient.class);

    private ObjectMapper objectMapper;

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        LOG.debug("Connection closed: {} - {}", statusCode, reason);
        objectMapper = null;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.debug("Got connect: {}", session);
        objectMapper = KubernetesFactory.createObjectMapper();
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        LOG.trace("Received message: {}", msg);
        if (msg != null && msg.length() > 0) {
            try {
                WatchEvent event = objectMapper.reader(WatchEvent.class).readValue(msg);
                T obj = (T) event.getObject();
                Action action = Action.valueOf(event.getType());
                eventReceived(action, obj);
            } catch (IOException e) {
                LOG.error("Could not deserialize watch event: {}", msg, e);
            } catch (ClassCastException e) {
                LOG.error("Received wrong type of object for watch", e);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid event type", e);
            }
        }
    }

    @OnWebSocketError
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof UpgradeException) {
            LOG.error("WebSocketError: Could not upgrade connection: {}", (((UpgradeException) cause).getResponseStatusCode()), cause);
        } else {
            LOG.error("WebSocketError: {}", cause);
        }
    }

}
