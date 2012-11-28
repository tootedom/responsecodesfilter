package org.greencheek.yammer.metrics.web.filter;

import com.yammer.metrics.reporting.MetricsServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.greencheek.yammer.metrics.web.filter.utils.MetricsFromJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 21/10/2012
 * Time: 13:52
 */
public class ResponseCodeFilterRequestsPerSecondTest {

    private MetricsFromJson jsonMetricsParser;
    private FilterChain mockFilterChain;
    private ResponseCodeFilter filter;
    private ServletTester tester;
    private String filterName;

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
                    Thread.sleep(400);
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
    public void testRequestsPerSecond() {
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET","http://localhost:9090/");
        MockHttpServletRequest postRequest = new MockHttpServletRequest("POST","http://localhost:9090/");

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            for(int i=0;i<10;i++) {
                filter.doFilter(getRequest,response,mockFilterChain);
                filter.doFilter(postRequest,response,mockFilterChain);
            }
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
            statsresponse.parse(tester.getResponses(statsrequest.generate()));
            content = statsresponse.getContent();

        } catch(Exception e) {
            e.printStackTrace();
            fail("Exception parsing metrics response");
        }

        assertTrue("Should have at least 2 requests per second", jsonMetricsParser.getMeterMean(content, "requestsPerSecond") > 2);
        assertEquals("Should have handled 20 filter requests", 20.0, jsonMetricsParser.getRequestMeterCount(content, "requestsPerSecond"), 0.0);
        assertEquals("Should have handled 10 GET requests", 10.0, jsonMetricsParser.getRequestTimerValue(content, "get-requests", "rate", "count"), 0.0);
        assertEquals("Should have handled 10 POST requests", 10.0, jsonMetricsParser.getRequestTimerValue(content, "post-requests", "rate", "count"), 0.0);
        assertTrue("Should have Percentile 99 of between 400 and 500, for all get requests",jsonMetricsParser.getTimerDurationValue(content, "get-requests", "p99")>=400.0);
        assertTrue("Should have Percentile 99 of between 400 and 500, for all get requests",jsonMetricsParser.getTimerDurationValue(content, "get-requests", "p99")<=500.0);

    }



}
