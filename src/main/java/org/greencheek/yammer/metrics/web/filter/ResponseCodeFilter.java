/*
 * Copyright 2012 dominictootell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greencheek.yammer.metrics.web.filter;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.util.RatioGauge;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * User: dominictootell
 * Date: 18/05/2012
 * Time: 09:26
 */
public class ResponseCodeFilter implements Filter
{
    public final static String DEFAULT_FILTER_NAME = "response-code-filter";

    public static final Class<ResponseCodeFilter> RESPONSE_CODE_FILTER_CLASS = ResponseCodeFilter.class;

    private final static String GET = "GET", POST = "POST", HEAD = "HEAD", PUT = "PUT",
            DELETE = "DELETE";

    private static final String DEFAULT_PING_ADMIN_URL ="/ping";
    private static final String DEFAULT_METRIC_ADMIN_URL ="/metrics";
    private static final String DEFAULT_HEALTH_ADMIN_URL ="/healthcheck";
    private static final String DEFAULT_THREAD_ADMIN_URL ="/threads";
    private static final String DEFAULT_MONITORING_GROUP_NAME = RESPONSE_CODE_FILTER_CLASS.getPackage().getName();
    private static final String DEFAULT_MONITORING_TYPE_NAME = RESPONSE_CODE_FILTER_CLASS.getSimpleName().replaceAll("\\$$", "");

    public static final String CONFIG_PARAM_PING_ADMIN_URL = "ping-endpoint";
    public static final String CONFIG_PARAM_METRIC_ADMIN_URL = "metric-endpoint";
    public static final String CONFIG_PARAM_HEALTH_ADMIN_URL = "health-endpoint";
    public static final String CONFIG_PARAM_THREAD_ADMIN_URL = "threads-endpoint";
    public static final String CONFIG_PARAM_MONITORING_GROUP_NAME = "monitoring-group-name";
    public static final String CONFIG_PARAM_MONITORING_TYPE_NAME = "monitoring-type-name";

    public String pingUrl;
    public String metricsUrl;
    public String healthUrl;
    public String threadUrl;
    public String monitoringGroupName;
    public String monitoringTypeName;

    private volatile String filterName;



    // Requests for specifically monitoring metrics requests
    private Map<String,Meter> adminMetrics;

    // Timers for last requests.
    private Timer timeTakenForGetsPerSecond;
    private Timer timeTakenForPostPerSecond;
    private Timer timeTakenForHeadPerSecond;
    private Timer timeTakenForPutPerSecond;
    private Timer timeTakenForDeletePerSecond;
    private Timer timeTakenForOtherRequestTypesForSecond;

    // The response types being output per second
    private Meter[] responses;

    // The number of requests per second being served
    private Meter requestsPerSecond;

    // The ratio for the number of non success codes in the past 5,10,15 minutes
    private List<Gauge> nonSuccessCodes;



    public static final String METRIC_NAME_LOOKUP_REQUESTS_PER_SECOND= "requestsPerSecond";
    public static final String METRIC_NAME_LOOKUP_PING_MONITORING_REQUESTS = "pingMonitoringRequests";
    public static final String METRIC_NAME_LOOKUP_THREAD_MONITORING_REQUESTS = "threadsMonitoringRequests";
    public static final String METRIC_NAME_LOOKUP_HEALTH_MONITORING_REQUESTS = "healthMonitoringRequests";
    public static final String METRIC_NAME_LOOKUP_METRICS_MONITORING_REQUESTS = "metricsMonitoringRequests";
    public static final String METRIC_NAME_LOOKUP_1XX_RESPONSES = "1xx-responses";
    public static final String METRIC_NAME_LOOKUP_2XX_RESPONSES = "2xx-responses";
    public static final String METRIC_NAME_LOOKUP_3XX_RESPONSES = "3xx-responses";
    public static final String METRIC_NAME_LOOKUP_4XX_RESPONSES = "4xx-responses";
    public static final String METRIC_NAME_LOOKUP_5XX_RESPONSES = "5xx-responses";
    public static final String METRIC_NAME_LOOKUP_UNKNOWN_RESPONSES = "unknown-responses";
    public static final String METRIC_NAME_LOOKUP_GET_REQUEST = "get-requests";
    public static final String METRIC_NAME_LOOKUP_HEAD_REQUEST = "head-requests";
    public static final String METRIC_NAME_LOOKUP_PUT_REQUEST = "put-requests";
    public static final String METRIC_NAME_LOOKUP_DELETE_REQUEST = "delete-requests";
    public static final String METRIC_NAME_LOOKUP_POST_REQUEST = "post-requests";
    public static final String METRIC_NAME_LOOKUP_OTHER_REQUEST = "other-requests";


    private ConcurrentHashMap<String,MetricName> metricNames = new ConcurrentHashMap<String, MetricName>();

    private void createRequestBasedMetricName(String name) {
        createMetricName(name,"requests");
    }

    private void createResponseBasedMetricName(String name) {
        createMetricName(name,"responses");
    }

    private void createMetricName(String name,String type) {
        if(getFilterName()!=null && getFilterName().length()!=0)  type = "."+type;
        metricNames.put(name,new MetricName(monitoringGroupName,monitoringTypeName, name, getFilterName()+type));
    }

    private void createMetricNames() {
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_REQUESTS_PER_SECOND);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_PING_MONITORING_REQUESTS);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_THREAD_MONITORING_REQUESTS);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_HEALTH_MONITORING_REQUESTS);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_METRICS_MONITORING_REQUESTS);

        createRequestBasedMetricName(METRIC_NAME_LOOKUP_GET_REQUEST);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_HEAD_REQUEST);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_PUT_REQUEST);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_DELETE_REQUEST);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_POST_REQUEST);
        createRequestBasedMetricName(METRIC_NAME_LOOKUP_OTHER_REQUEST);
        
        createResponseBasedMetricName(METRIC_NAME_LOOKUP_1XX_RESPONSES);
        createResponseBasedMetricName(METRIC_NAME_LOOKUP_2XX_RESPONSES);
        createResponseBasedMetricName(METRIC_NAME_LOOKUP_3XX_RESPONSES);
        createResponseBasedMetricName(METRIC_NAME_LOOKUP_4XX_RESPONSES);
        createResponseBasedMetricName(METRIC_NAME_LOOKUP_5XX_RESPONSES);
        createResponseBasedMetricName(METRIC_NAME_LOOKUP_UNKNOWN_RESPONSES);

    }


    public ResponseCodeFilter() {
    }

    public void setFilterName(FilterConfig filterConfig)  {
        String name = filterConfig.getFilterName();
        if(name == null || name.length()==0) this.filterName = DEFAULT_FILTER_NAME;
        else this.filterName = name;
    }

    public String getFilterName() {
        return this.filterName;
    }

    private void setMonitoringGrouping(FilterConfig filterConfig) {
        String groupName = filterConfig.getInitParameter(CONFIG_PARAM_MONITORING_GROUP_NAME);
        if(groupName == null) monitoringGroupName = DEFAULT_MONITORING_GROUP_NAME;
        else monitoringGroupName = groupName;

        String typeName = filterConfig.getInitParameter(CONFIG_PARAM_MONITORING_TYPE_NAME);
        if(typeName == null) monitoringTypeName = DEFAULT_MONITORING_TYPE_NAME;
        else monitoringTypeName = typeName;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Record the filter's name
        setFilterName(filterConfig);

        // Setup the monitoring grouping under which metrics are recorded
        setMonitoringGrouping(filterConfig);

        // Create the metrics when the filter is initialised
        createMetrics(filterConfig);
    }


    public synchronized void createMetrics(FilterConfig filterConfig) {
        // setup the metric names
        createMetricNames();

        // create metrics that monitor the number of times an metric admin endpoint has
        // been hit
        createAdminEndPointMetrics(filterConfig);

        // Requests Handled Per Second
        requestsPerSecond = Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_REQUESTS_PER_SECOND),"requests", TimeUnit.SECONDS);

        // The XXX responses per second
        this.responses = new Meter[]{
                Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_1XX_RESPONSES), "responses", TimeUnit.SECONDS), // 1xx
                Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_2XX_RESPONSES), "responses", TimeUnit.SECONDS), // 2xx
                Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_3XX_RESPONSES), "responses", TimeUnit.SECONDS), // 3xx
                Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_4XX_RESPONSES), "responses", TimeUnit.SECONDS), // 4xx
                Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_5XX_RESPONSES), "responses", TimeUnit.SECONDS), // 5xx
                Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_UNKNOWN_RESPONSES), "responses", TimeUnit.SECONDS) // unknown
        };

        nonSuccessCodes = new ArrayList<Gauge>(6);

        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[2], "3xx"));
        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[3], "4xx"));
        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[4], "5xx"));

        timeTakenForGetsPerSecond = Metrics.newTimer(metricNames.get(METRIC_NAME_LOOKUP_GET_REQUEST), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForPostPerSecond = Metrics.newTimer(metricNames.get(METRIC_NAME_LOOKUP_POST_REQUEST), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForHeadPerSecond = Metrics.newTimer(metricNames.get(METRIC_NAME_LOOKUP_HEAD_REQUEST), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForPutPerSecond = Metrics.newTimer(metricNames.get(METRIC_NAME_LOOKUP_PUT_REQUEST), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForDeletePerSecond = Metrics.newTimer(metricNames.get(METRIC_NAME_LOOKUP_DELETE_REQUEST),TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForOtherRequestTypesForSecond = Metrics.newTimer(metricNames.get(METRIC_NAME_LOOKUP_OTHER_REQUEST), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
    }

    private synchronized void createAdminEndPointMetrics(FilterConfig filterConfig) {
        readAdminConfigUrls(filterConfig);
        adminMetrics = new HashMap<String, Meter>(4);
        adminMetrics.put(pingUrl,Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_PING_MONITORING_REQUESTS),"requests", TimeUnit.SECONDS));
        adminMetrics.put(threadUrl,Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_THREAD_MONITORING_REQUESTS), "requests", TimeUnit.SECONDS));
        adminMetrics.put(metricsUrl,Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_METRICS_MONITORING_REQUESTS), "requests", TimeUnit.SECONDS));
        adminMetrics.put(healthUrl,Metrics.newMeter(metricNames.get(METRIC_NAME_LOOKUP_HEALTH_MONITORING_REQUESTS), "requests", TimeUnit.SECONDS));
    }


    private synchronized void readAdminConfigUrls(FilterConfig filterConfig) {
        pingUrl = getInitParam(CONFIG_PARAM_PING_ADMIN_URL, DEFAULT_PING_ADMIN_URL,filterConfig);
        metricsUrl = getInitParam(CONFIG_PARAM_METRIC_ADMIN_URL, DEFAULT_METRIC_ADMIN_URL,filterConfig);
        healthUrl = getInitParam(CONFIG_PARAM_HEALTH_ADMIN_URL, DEFAULT_HEALTH_ADMIN_URL,filterConfig);
        threadUrl = getInitParam(CONFIG_PARAM_THREAD_ADMIN_URL, DEFAULT_THREAD_ADMIN_URL,filterConfig);
    }


    private String getInitParam(String paramName,String defaultValue, FilterConfig filterConfig) {
        String parameterValue = filterConfig.getInitParameter(paramName);
        if(parameterValue==null || parameterValue.length()==0) {
            return defaultValue;
        } else {
            return parameterValue;
        }
    }

    private Gauge[] createNonSuccessRatio(Meter requestsPerSecond,Meter responseCodeMeter, String responseCode) {
        String oneMin = "percent-" + responseCode + "-1m";
        String fiveMin = "percent-" + responseCode + "-5m";


        createRequestBasedMetricName(oneMin);
        createRequestBasedMetricName(fiveMin);

        return new Gauge[] {
                Metrics.newGauge(metricNames.get(oneMin),createOneMinRatio(requestsPerSecond, responseCodeMeter)),
                Metrics.newGauge(metricNames.get(fiveMin),createFiveMinRatio(requestsPerSecond, responseCodeMeter))
        };


    }

    private RatioGauge createOneMinRatio(final Meter requestsPerSecond,final Meter responseCodeMeter) {
        return new RatioGauge() {
            @Override
            protected double getNumerator() {
                return responseCodeMeter.oneMinuteRate();
            }

            @Override
            protected double getDenominator() {
                return requestsPerSecond.oneMinuteRate();
            }
        };
    }

    private RatioGauge createFiveMinRatio(final Meter requestsPerSecond,final Meter responseCodeMeter) {
        return new RatioGauge() {
            @Override
            protected double getNumerator() {
                return responseCodeMeter.fiveMinuteRate();
            }

            @Override
            protected double getDenominator() {
                return requestsPerSecond.fiveMinuteRate();
            }
        };
    }

    private void addRatioGauge(List<Gauge> listToAddGaugesTo,Gauge[] gaugesToAdd) {
        listToAddGaugesTo.addAll(Arrays.asList(gaugesToAdd));
    }


    @Override
    public void destroy() {
        MetricsRegistry reg = Metrics.defaultRegistry();
        for(MetricName name : metricNames.values()) {
            reg.removeMetric(name);
        }

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final StatusExposingServletResponse wrappedResponse =
                new StatusExposingServletResponse((HttpServletResponse) response);

        final TimerContext context = getTimerForCurrentRequestMethodType(((HttpServletRequest)request).getMethod()).time();
        try {
            chain.doFilter(request, wrappedResponse);
        } finally {
            context.stop();
            updateResponseRate(wrappedResponse.getStatus());
            updateAdminMetricsIfRequestMatched(((HttpServletRequest) request).getServletPath());
        }
    }

    private void updateAdminMetricsIfRequestMatched(String path) {
        if(adminMetrics.containsKey(path)) {
            adminMetrics.get(path).mark();
        }
    }

    private Timer getTimerForCurrentRequestMethodType(String method) {
        if (GET.equalsIgnoreCase(method)) {
            return timeTakenForGetsPerSecond;
        } else if (POST.equalsIgnoreCase(method)) {
            return timeTakenForPostPerSecond;
        } else if (PUT.equalsIgnoreCase(method)) {
            return timeTakenForPutPerSecond;
        } else if (HEAD.equalsIgnoreCase(method)) {
            return timeTakenForHeadPerSecond;
        } else if (DELETE.equalsIgnoreCase(method)) {
            return timeTakenForDeletePerSecond;
        } else {
            return timeTakenForOtherRequestTypesForSecond;
        }
    }

    private void updateResponseRate(int responseCode) {
        final int response = responseCode / 100;
        if (response >= 1 && response <= 5) {
            responses[response - 1].mark();
        } else {
            responses[5].mark();
        }
        requestsPerSecond.mark();
    }



}
