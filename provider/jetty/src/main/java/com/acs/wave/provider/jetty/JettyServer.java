package com.acs.wave.provider.jetty;

import com.acs.wave.provider.common.WaveServer;
import com.acs.wave.provider.common.WaveServerServlet;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import spark.embeddedserver.jetty.websocket.WebSocketCreatorFactory;
import spark.ssl.SslStores;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class JettyServer extends WaveServer<JettyServerDefinition> {

    private Server server;

    JettyServer(JettyServerDefinition definition) {
        super(definition);
    }


    @Override
    protected void startServer() throws Exception {
        server = createServer();
        WaveServerServlet servlet = new WaveServerServlet(definition.httpRouter);
        server.setHandler(new JettyHandler(servlet));
        if (definition.hasHTTP()) {
            server.addConnector(getServerConnector());
        }
        if (definition.hasHTTPS()) {
            server.addConnector(getSecureServerConnector());
        }
        server.start();
    }

    @Override
    protected void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    public ServletContextHandler create(Map<String, Class<?>> webSocketHandlers, Optional<Integer> webSocketIdleTimeoutMillis) {
        ServletContextHandler webSocketServletContextHandler = null;
        if (webSocketHandlers != null) {
            try {
                webSocketServletContextHandler = new ServletContextHandler(null, "/", true, false);
                WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configureContext(webSocketServletContextHandler);
                if (webSocketIdleTimeoutMillis.isPresent()) {
                    webSocketUpgradeFilter.getFactory().getPolicy().setIdleTimeout(webSocketIdleTimeoutMillis.get());
                }
                for (String path : webSocketHandlers.keySet()) {
                    WebSocketCreator webSocketCreator = WebSocketCreatorFactory.create(webSocketHandlers.get(path));
                    webSocketUpgradeFilter.addMapping(new ServletPathSpec(path), webSocketCreator);
                }
            } catch (Exception ex) {
                log.error("creation of websocket context handler failed.", ex);
                webSocketServletContextHandler = null;
            }
        }
        return webSocketServletContextHandler;
    }


    private ServerConnector getServerConnector() {
        ServerConnector connector = new ServerConnector(server);
        connector.setIdleTimeout(TimeUnit.HOURS.toMillis(1));
        connector.setSoLingerTime(definition.soLingerTime);
        connector.setHost(definition.host);
        connector.setPort(definition.httpPort);
        return connector;
    }

    private Server createServer() {
        Server server;

        if (definition.maxThreads > 0) {
            int max = (definition.maxThreads > 0) ? definition.maxThreads : 200;
            int min = (definition.minThreads > 0) ? definition.minThreads : 8;
            int idleTimeout = (definition.threadTimeoutMillis > 0) ? definition.threadTimeoutMillis : 60000;

            server = new Server(new QueuedThreadPool(max, min, idleTimeout));
        } else {
            server = new Server();
        }

        return server;
    }

    private ServerConnector getSecureServerConnector() {
        SslStores sslStores = definition.sslContext;

        SslContextFactory sslContextFactory = new SslContextFactory(sslStores.keystoreFile());

        if (sslStores.keystorePassword() != null) {
            sslContextFactory.setKeyStorePassword(sslStores.keystorePassword());
        }

        if (sslStores.trustStoreFile() != null) {
            sslContextFactory.setTrustStorePath(sslStores.trustStoreFile());
        }

        if (sslStores.trustStorePassword() != null) {
            sslContextFactory.setTrustStorePassword(sslStores.trustStorePassword());
        }

        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setIdleTimeout(TimeUnit.HOURS.toMillis(1));
        connector.setSoLingerTime(definition.soLingerTime);
        connector.setHost(definition.host);
        connector.setPort(definition.httpsPort);
        return connector;
    }
}
