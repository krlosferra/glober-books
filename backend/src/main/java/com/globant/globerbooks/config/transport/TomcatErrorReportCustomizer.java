package com.globant.globerbooks.config.transport;

import org.apache.catalina.Host;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * Installs the Tomcat error report valve with the report and the server information switched off,
 * so that an error page produced by the container names no product, version or cause (SC-02,
 * SC-11). The customizer is registered unconditionally with the web server factory.
 */
@Component
public class TomcatErrorReportCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    /**
     * Adds a context customizer that puts the configured valve on the host, before the host would
     * add its default one, and makes the connector answer {@code 100 Continue} only when the
     * application reads the body, so a rejected request never gets one (SC-06).
     *
     * @param factory the Tomcat factory
     */
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
                protocol.setContinueResponseTiming("onRead");
            }
        });
        factory.addContextCustomizers(context -> {
            if (context.getParent() instanceof Host host) {
                ErrorReportValve valve = new ErrorReportValve();
                valve.setShowReport(false);
                valve.setShowServerInfo(false);
                host.getPipeline().addValve(valve);
            }
        });
    }
}
