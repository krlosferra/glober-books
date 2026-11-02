package com.globant.globerbooks.config.transport;

import org.apache.catalina.Host;
import org.apache.catalina.valves.ErrorReportValve;
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
     * add its default one.
     *
     * @param factory the Tomcat factory
     */
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
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
