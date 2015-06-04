package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.watch.WatchEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractWatcher<T extends HasMetadata> extends WebSocketAdapter implements Watcher<T> {

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesClient.class);

    private ObjectMapper objectMapper;

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        LOG.debug("Got connect: {}", sess);
        objectMapper = KubernetesFactory.createObjectMapper();
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        LOG.debug("Connection closed: {} - {}", statusCode, reason);
        objectMapper = null;
    }

    @Override
    public void onWebSocketText(String message) {
        LOG.trace("Received message: {}", message);
        if (message != null && message.length() > 0) {
            try {
                WatchEvent event = objectMapper.reader(WatchEvent.class).readValue(message);
                T obj = (T) event.getObject();
                Action action = Action.valueOf(event.getType());
                eventReceived(action, obj);
            } catch (IOException e) {
                LOG.error("Could not deserialize watch event: {}", message, e);
            } catch (ClassCastException e) {
                LOG.error("Received wrong type of object for watch", e);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid event type", e);
            }
        }
    }

    public void onWebSocketError(Throwable cause) {
        if (cause instanceof UpgradeException) {
            LOG.error("WebSocketError: Could not upgrade connection: {}", (((UpgradeException) cause).getResponseStatusCode()), cause);
        } else {
            LOG.error("WebSocketError: {}", cause);
        }
    }

}
