import com.google.gson.GsonBuilder;
import com.yammer.metrics.reporting.MetricsServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 21/10/2012
 * Time: 13:52
 */
public class ResponseCodeFilterRequestsPerSecondTest {


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

        assertTrue("Should have at least 2 requests per second", getMeterMean(content, "requestsPerSecond") > 2);
        assertEquals("Should have handled 20 filter requests", 20.0, getMeterCount(content, "requestsPerSecond"), 0.0);
    }




    @Ignore
    @Test
    public void testRequestsPerSecondViaHttpGetRequests() {

    }

    private double getMeterCount(String json, String meterName) {
        return getRequestMeterValue(json,meterName,"count");
    }
    private double getMeterMean(String json, String meterName) {
        return getRequestMeterValue(json,meterName,"mean");
    }

    private double getRequestMeterValue(String json, String meterName, String dataPoint) {
        Map m = new GsonBuilder().create().fromJson(json,Map.class);
        Map<String,Map> requests = (Map<String,Map>)m.get(
               ResponseCodeFilter.RESPONSE_CODE_FILTER_CLASS.getName()
                        + "." + ResponseCodeFilter.DEFAULT_FILTER_NAME
                        + ".requests");

        Double val = (Double)requests.get(meterName).get(dataPoint);
        return val;
    }
}
