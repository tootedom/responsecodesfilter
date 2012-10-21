import com.google.gson.GsonBuilder;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.MetricsServlet;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.*;

import javax.servlet.*;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 21/10/2012
 * Time: 13:52
 */
public class ResponseCodeFilterTest {


    private FilterChain mockFilterChain;
    ResponseCodeFilter filter;
    ServletTester tester;

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
    }

    @After
    public void tearDown() {
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

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(getRequest,response,mockFilterChain);
            filter.doFilter(postRequest,response,mockFilterChain);
        } catch(Exception e) {
            fail("failed with exception during filter request");
        }


        HttpTester statsrequest = new HttpTester();
        HttpTester statsresponse = new HttpTester();
        statsrequest.setMethod("GET");
        statsrequest.setHeader("Host","tester");
        statsrequest.setURI("/metrics");
        statsrequest.setVersion("HTTP/1.1");

        try {
            statsresponse.parse(tester.getResponses(statsrequest.generate()));
            assertEquals("one get request should have been filtered", 1.0, getTimerCount(statsresponse.getContent(), "get-requests"),0.0);
        } catch(Exception e) {
            e.printStackTrace();;
        }
    }


    private double getTimerCount(String json, String timerName) {
        Map m = new GsonBuilder().create().fromJson(json,Map.class);
        Double val = ((Map<String,Map<String,Map<String,Double>>>)m.get("org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter")).get(timerName).get("rate").get("count");
        return val;
    }
}
