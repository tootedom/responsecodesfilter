package org.greencheek.yammer.metrics.web.filter.utils;

import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;

import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 28/11/2012
 * Time: 12:59
 */
public class MetricsAsJsonViaHttpRequest implements MetricsAsJson {

    private final ServletTester tester;

    public MetricsAsJsonViaHttpRequest(ServletTester webservice) {
        tester = webservice;
    }

    public String getMetrics() {
        HttpTester statsrequest = new HttpTester();
        HttpTester statsresponse = new HttpTester();
        statsrequest.setMethod("GET");
        statsrequest.setHeader("Host","tester");
        statsrequest.setURI("/metrics");
        statsrequest.setVersion("HTTP/1.1");

        String content = "";
        try {
            statsresponse.parse(tester.getResponses(statsrequest.generate()));
            content = statsresponse.getContent();
        } catch(Exception e) {
            e.printStackTrace();
            fail("Exception parsing metrics response");
        }
        return content;
    }
}
