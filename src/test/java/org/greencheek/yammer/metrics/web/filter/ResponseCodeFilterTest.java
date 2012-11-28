package org.greencheek.yammer.metrics.web.filter;

import com.yammer.metrics.reporting.MetricsServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.greencheek.yammer.metrics.web.filter.utils.MetricsFromJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.*;

import javax.servlet.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 21/10/2012
 * Time: 13:52
 */
public class ResponseCodeFilterTest {


    private FilterChain mockFilterChain;
    private ResponseCodeFilter filter;
    private ServletTester tester;
    private MetricsFromJson jsonMetricsParser;
    private int port;

    @Before
    public void setUp() throws Exception {

        ServletContext context = new MockServletContext();
        final MockRequestDispatcher requestDispatcher = new MockRequestDispatcher("");

        // http servlet mocked for mocked dispatcher
        MockFilterConfig config = new MockFilterConfig(context);
        mockFilterChain = new MockFilterChain()  {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        };

        filter = new ResponseCodeFilter();

        filter.init(config);


        tester = new ServletTester();
        ServletHolder sh = tester.addServlet(MetricsServlet.class.getName(),"/metrics");
        sh.setInitParameter(MetricsServlet.SHOW_JVM_METRICS,"false");

        try {
            tester.start();
        } catch (Exception e) {
            e.printStackTrace();;
            fail("Failed to start Jetty Context");
        }

        jsonMetricsParser = new MetricsFromJson(filter);
    }

    @After
    public void tearDown() {

        filter.destroy();
        try {
            tester.stop();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to stop jetty context");
        }
    }

    @Test
    public void testMetricRecorded() {
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET","http://localhost:9090/");
        MockHttpServletRequest postRequest = new MockHttpServletRequest("POST","http://localhost:9090/");
        MockHttpServletRequest optionsRequest = new MockHttpServletRequest("OPTIONS","http://localhost:9090/");
        MockHttpServletRequest deleteRequest = new MockHttpServletRequest("DELETE","http://localhost:9090/");
        MockHttpServletRequest putRequest = new MockHttpServletRequest("PUT","http://localhost:9090/");
        MockHttpServletRequest headRequest = new MockHttpServletRequest("HEAD","http://localhost:9090/");
        MockHttpServletRequest adminRequest = new MockHttpServletRequest("NONE","http://localhost:9090/");
        adminRequest.setServletPath("/metrics");

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(getRequest,response,mockFilterChain);
            filter.doFilter(postRequest,response,mockFilterChain);
            filter.doFilter(optionsRequest,response,mockFilterChain);
            filter.doFilter(deleteRequest,response,mockFilterChain);
            filter.doFilter(putRequest,response,mockFilterChain);
            filter.doFilter(headRequest,response,mockFilterChain);
            filter.doFilter(adminRequest,response,mockFilterChain);
        } catch(Exception e) {
            fail("failed with exception during filter request");
        }


        HttpTester statsrequest = new HttpTester();
        HttpTester statsresponse = new HttpTester();
        statsrequest.setMethod("GET");
        statsrequest.setHeader("Host","tester");
        statsrequest.setURI("/metrics");
        statsrequest.setVersion("HTTP/1.1");

        String content = "";
        try {
            tester.getResponses(statsrequest.generate());
            statsresponse.parse(tester.getResponses(statsrequest.generate()));

            content = statsresponse.getContent();
            System.out.println(content);
        } catch(Exception e) {
            e.printStackTrace();;
        }

        assertEquals("one GET request should have been filtered", 1.0, jsonMetricsParser.getRequestTimerValue(content, "get-requests", "rate", "count"),0.0);
        assertEquals("one PUT request should have been filtered", 1.0, jsonMetricsParser.getRequestTimerValue(content, "put-requests", "rate", "count"),0.0);
        assertEquals("one POST request should have been filtered", 1.0, jsonMetricsParser.getRequestTimerValue(content, "post-requests", "rate", "count"),0.0);
        assertEquals("one DELETE request should have been filtered", 1.0, jsonMetricsParser.getRequestTimerValue(content, "delete-requests", "rate", "count"),0.0);
        assertEquals("one HEAD request should have been filtered", 1.0, jsonMetricsParser.getRequestTimerValue(content, "head-requests", "rate", "count"),0.0);
        assertEquals("two OTHER request should have been filtered", 2.0, jsonMetricsParser.getRequestTimerValue(content, "other-requests", "rate", "count"),0.0);
        assertEquals("one admin request should have been filtered", 1.0, jsonMetricsParser.getRequestMeterCount(content, "metricsMonitoringRequests"),0.0);


    }


}
