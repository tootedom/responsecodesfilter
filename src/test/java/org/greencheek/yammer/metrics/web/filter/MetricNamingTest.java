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

import javax.servlet.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: dominictootell
 * Date: 28/11/2012
 * Time: 12:52
 */
public class MetricNamingTest {

    private FilterChain mockFilterChain;
    private ResponseCodeFilter filter;
    private ServletTester tester;
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
                try {
                    Thread.sleep(400);
                } catch (Exception e) {
                }
            }
        };

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


    private void executeRequestAndCheckForGetMetric(ResponseCodeFilter filter) {
        MockHttpServletRequest getRequest = new MockHttpServletRequest("GET","http://localhost:9090/");

        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(getRequest,response,mockFilterChain);
        } catch(Exception e) {
            fail("failed with exception during filter request");
        }

        String json = metricsService.getMetrics();

        MetricsFromJson jsonMetricsParser = new MetricsFromJson(filter);
        assertEquals("Should have handled 1 GET requests", 1.0, jsonMetricsParser.getRequestTimerValue(json, "get-requests", "rate", "count"), 1.0);

    }

    @Test
    public void testChangeGroupName() {

        final String METRICS_PACKAGE_NAME = "org.greencheek.async.typeahead";
        filter = new ResponseCodeFilter();

        MockFilterConfig config = new MockFilterConfig(new MockServletContext());

        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_GROUP_NAME,METRICS_PACKAGE_NAME);

        try {
            filter.init(config);
        } catch (ServletException e) {
            fail("Exception during filter initialisation");
        }


        assertEquals("Expecting metrics grouping name to be " + METRICS_PACKAGE_NAME +"." + filter.getFilterName(),METRICS_PACKAGE_NAME +"." + filter.getFilterName(),filter.getMetricsGroupName());

        executeRequestAndCheckForGetMetric(filter);
    }

    @Test
    public void testChangeGroupNameAndMontoringClassName() {

        final String METRICS_PACKAGE_NAME = "org.greencheek.async.typeahead";
        final String METRICS_GROUPING_NAME = "all-requests-filter";
        filter = new ResponseCodeFilter();

        MockFilterConfig config = new MockFilterConfig(new MockServletContext());

        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_GROUP_NAME,METRICS_PACKAGE_NAME);
        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_TYPE_NAME,METRICS_GROUPING_NAME);

        try {
            filter.init(config);
        } catch (ServletException e) {
            fail("Exception during filter initialisation");
        }


        assertEquals("Expecting metrics grouping name to be " + METRICS_PACKAGE_NAME +"." + METRICS_GROUPING_NAME,METRICS_PACKAGE_NAME +"." + METRICS_GROUPING_NAME,filter.getMetricsGroupName());

        executeRequestAndCheckForGetMetric(filter);

    }


    @Test
    public void testChangeFilterNameDoesNotAffectGroupNameAndMontoringClassNameSettings() {

        final String METRICS_PACKAGE_NAME = "org.greencheek.async.typeahead";
        final String METRICS_GROUPING_NAME = "all-requests-filter";
        final String FILTER_NAME = "AllRequests";
        filter = new ResponseCodeFilter();

        MockFilterConfig config = new MockFilterConfig(new MockServletContext(),FILTER_NAME);

        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_GROUP_NAME,METRICS_PACKAGE_NAME);
        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_TYPE_NAME,METRICS_GROUPING_NAME);

        try {

            filter.init(config);
        } catch (ServletException e) {
            fail("Exception during filter initialisation");
        }


        assertEquals("Expecting metrics grouping name to be " + METRICS_PACKAGE_NAME +"." + METRICS_GROUPING_NAME,METRICS_PACKAGE_NAME +"." + METRICS_GROUPING_NAME,filter.getMetricsGroupName());

        executeRequestAndCheckForGetMetric(filter);

    }


    @Test
    public void testFilterNameIsUsedAsMontoringClassNameWhenNotSet() {

        final String METRICS_PACKAGE_NAME = "org.greencheek.async.typeahead";
        final String METRICS_GROUPING_NAME = "";
        final String FILTER_NAME = "AllRequests";
        filter = new ResponseCodeFilter();

        MockFilterConfig config = new MockFilterConfig(new MockServletContext(),FILTER_NAME);

        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_GROUP_NAME,METRICS_PACKAGE_NAME);
        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_TYPE_NAME,METRICS_GROUPING_NAME);

        try {

            filter.init(config);
        } catch (ServletException e) {
            fail("Exception during filter initialisation");
        }


        assertEquals("Expecting metrics grouping name to be " + METRICS_PACKAGE_NAME +"." + FILTER_NAME,METRICS_PACKAGE_NAME +"." + FILTER_NAME,filter.getMetricsGroupName());

        executeRequestAndCheckForGetMetric(filter);

    }


    @Test
    public void testEmptyMonitoringGroupAndFilterName() {

        final String METRICS_PACKAGE_NAME = "org.greencheek.async.typeahead";
        final String METRICS_GROUPING_NAME = "response-code-filter";
        final String FILTER_NAME = "";
        filter = new ResponseCodeFilter();

        MockFilterConfig config = new MockFilterConfig(new MockServletContext(),FILTER_NAME);

        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_GROUP_NAME,METRICS_PACKAGE_NAME);
        config.addInitParameter(ResponseCodeFilter.CONFIG_PARAM_MONITORING_TYPE_NAME,"");

        try {

            filter.init(config);
        } catch (ServletException e) {
            fail("Exception during filter initialisation");
        }


        assertEquals("Expecting metrics grouping name to be " + METRICS_PACKAGE_NAME +"." + METRICS_GROUPING_NAME,METRICS_PACKAGE_NAME +"." + METRICS_GROUPING_NAME,filter.getMetricsGroupName());

        executeRequestAndCheckForGetMetric(filter);

    }

}
