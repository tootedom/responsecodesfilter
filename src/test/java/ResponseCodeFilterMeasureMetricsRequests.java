import com.google.gson.GsonBuilder;
import com.yammer.metrics.reporting.MetricsServlet;
import com.yammer.metrics.reporting.PingServlet;
import com.yammer.metrics.reporting.ThreadDumpServlet;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 21/10/2012
 * Time: 13:52
 */
public class ResponseCodeFilterMeasureMetricsRequests {

    ServletTester tester;

    @Before
    public void setUp() throws Exception {

        tester = new ServletTester();
        tester.setContextPath("/distribution");
        FilterHolder holder = tester.addFilter(ResponseCodeFilter.class, "/*", 0);
        holder.setInitParameter(ResponseCodeFilter.HEALTH_ADMIN_URL_PARAM,"/admin/healthcheck");
        holder.setInitParameter(ResponseCodeFilter.PING_ADMIN_URL_PARAM,"/admin/ping");
        holder.setInitParameter(ResponseCodeFilter.THREAD_ADMIN_URL_PARAM,"/admin/threads");
        holder.setInitParameter(ResponseCodeFilter.METRIC_ADMIN_URL_PARAM,"/admin/metrics");

        ServletHolder metricsServlet = tester.addServlet(MetricsServlet.class.getName(),"/admin/metrics");
        tester.addServlet(PingServlet.class.getName(),"/admin/ping");
        tester.addServlet(ThreadDumpServlet.class.getName(),"/admin/threads");
        tester.addServlet(MetricsServlet.class.getName(),"/admin/healthcheck");
        tester.addServlet(DefaultServlet.class, "/");


        metricsServlet.setInitParameter(MetricsServlet.SHOW_JVM_METRICS,"false");

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

    private HttpTester getGetRequest() {
        HttpTester statsrequest = new HttpTester();
        statsrequest.setMethod("GET");
        statsrequest.setHeader("Host", "tester");
        statsrequest.setVersion("HTTP/1.1");
        return statsrequest;
    }

    private HttpTester getMetricsRequest() {
        HttpTester statsrequest = getGetRequest();
        statsrequest.setURI("/distribution/admin/metrics");
        return statsrequest;
    }

    private HttpTester getPingRequest() {
        HttpTester statsrequest = getGetRequest();
        statsrequest.setURI("/distribution/admin/ping");
        return statsrequest;
    }

    private HttpTester getThreadsRequest() {
        HttpTester statsrequest = getGetRequest();
        statsrequest.setURI("/distribution/admin/threads");
        return statsrequest;
    }

    private HttpTester getHealthRequest() {
        HttpTester statsrequest = getGetRequest();
        statsrequest.setURI("/distribution/admin/healthcheck");
        return statsrequest;
    }

    private HttpTester get404Request() {
        HttpTester statsrequest = getGetRequest();
        statsrequest.setURI("/distribution/"+ UUID.randomUUID().toString());
        return statsrequest;
    }




    @Test
    public void testMetricsEndPointsAreRecorded() {


        HttpTester metricsRequest = getMetricsRequest();
        HttpTester pingRequest = getPingRequest();
        HttpTester healthRequest = getHealthRequest();
        HttpTester threadsRequest = getThreadsRequest();

        HttpTester notFoundRequest = get404Request();
        try {
            tester.getResponses(pingRequest.generate());
            tester.getResponses(healthRequest.generate());
            tester.getResponses(threadsRequest.generate());
            tester.getResponses(metricsRequest.generate());
            tester.getResponses(notFoundRequest.generate());
        } catch(Exception e) {
            fail("Exception calling the requests");
        }


        HttpTester statsresponse = new HttpTester();

        try {

            statsresponse.parse(tester.getResponses(metricsRequest.generate()));
            assertEquals("one get request should have been filtered", 1.0, getMeterValue(statsresponse.getContent(), "pingMetricRequests", "count"),0.0);
            assertEquals("one get request should have been filtered", 1.0, getMeterValue(statsresponse.getContent(), "threadsMetricRequests","count"),0.0);
            assertEquals("one get request should have been filtered", 1.0, getMeterValue(statsresponse.getContent(), "healthMetricRequests","count"),0.0);
            assertTrue("one get request should have been filtered", 1.0 >= getMeterValue(statsresponse.getContent(), "metricsMetricRequests", "count"));
        } catch(Exception e) {
            e.printStackTrace();;
        }
    }


    private double getMeterValue(String json, String meterName, String dataPoint) {
        Map m = new GsonBuilder().create().fromJson(json,Map.class);
        Double val = ((Map<String,Map<String,Double>>)m.get("org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter")).get(meterName).get(dataPoint);
        return val;
    }
}
