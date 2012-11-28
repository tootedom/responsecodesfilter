package org.greencheek.yammer.metrics.web.filter;

import com.yammer.metrics.reporting.MetricsServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.ServletTester;
import org.greencheek.yammer.metrics.web.filter.utils.MetricsAsJson;
import org.greencheek.yammer.metrics.web.filter.utils.MetricsAsJsonViaHttpRequest;
import org.greencheek.yammer.metrics.web.filter.utils.MetricsFromJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 21/10/2012
 * Time: 13:52
 */
public class StatusCodesAreByFilterTest {


    private FilterChain mockFilterChain;
    private ResponseCodeFilter filter;
    private ServletTester tester;
    private MetricsFromJson jsonMetricsParser;
    private MetricsAsJson metricsService;
    @Before
    public void setUp() throws Exception {

        ServletContext context = new MockServletContext();
        final MockRequestDispatcher requestDispatcher = new MockRequestDispatcher("");

        // http servlet mocked for mocked dispatcher
        MockFilterConfig config = new MockFilterConfig(context);
        mockFilterChain = new MockFilterChain()  {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                HttpServletRequest hreq = (HttpServletRequest)req;
                HttpServletResponse hres = (HttpServletResponse)res;

                String val = hreq.getHeader("SEND_TYPE");

                try {
                    if(val == null || val.equals("404")) {
                        hres.sendError(404);
                    }
                    else if(val.equals("500")) {
                        hres.sendError(500);
                    }
                    else if(val.equals("OTHER")) {
                        hres.setStatus(600);
                    }
                    else if(val.equals("200")) {
                        hres.setStatus(HttpServletResponse.SC_OK);
                    }
                    else {
                        hres.sendError(500,val);
                    }
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

        metricsService = new MetricsAsJsonViaHttpRequest(tester);

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
    public void test404Recorded() {

        testErrorRecorded("404","4xx-responses");
    }

    @Test
    public void test500Recorded() {

        testErrorRecorded("500","5xx-responses");
    }

    @Test
    public void test500WithMessageRecorded() {

        testErrorRecorded("500xxx","5xx-responses");
    }

    @Test
    public void testOtherStatusCodeRegistered() {

        testErrorRecorded("OTHER","unknown-responses");
    }

    @Test
    public void test200StatusCodeRegistered() {

        testErrorRecorded("200","2xx-responses");
    }


    private void testErrorRecorded(String headerValue,String metricName) {
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET","http://localhost:9090/error");
        getRequest.addHeader("SEND_TYPE",headerValue);

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(getRequest,response,mockFilterChain);

        } catch(Exception e) {
            fail("failed with exception during filter request");
        }


        String json = metricsService.getMetrics();
        System.out.println(json);

        assertEquals("one GET request should have been filtered", 1.0, jsonMetricsParser.getRequestTimerValue(json, "get-requests", "rate", "count"), 1.0);

        assertEquals("one "+headerValue+" request should have been filtered", 1.0, jsonMetricsParser.getResponseMeterCount(json, "4xx-responses"),1.0);

    }

}
