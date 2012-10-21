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
    private final static String GET = "GET", POST = "POST", HEAD = "HEAD", PUT = "PUT",
            DELETE = "DELETE";

    private static final String PING_ADMIN_URL ="/ping";
    private static final String METRIC_ADMIN_URL ="/metrics";
    private static final String HEALTH_ADMIN_URL ="/healthcheck";
    private static final String THREAD_ADMIN_URL ="/threads";

    public static final String PING_ADMIN_URL_PARAM = "ping-endpoint";
    public static final String METRIC_ADMIN_URL_PARAM = "metric-endpoint";
    public static final String HEALTH_ADMIN_URL_PARAM = "health-endpoint";
    public static final String THREAD_ADMIN_URL_PARAM = "threads-endpoint";

    public String pingUrl;
    public String metricsUrl;
    public String healthUrl;
    public String threadUrl;


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

    private final Class<ResponseCodeFilter> responseCodeFilterClass;

    private List<String> metricNames = new ArrayList<String>();

    public ResponseCodeFilter() {
        responseCodeFilterClass = ResponseCodeFilter.class;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Create the metrics when the filter is initialised
        createAdminMetrics(filterConfig);
        createMetrics();

    }

    private synchronized void createAdminMetrics(FilterConfig filterConfig) {
        readAdminConfigUrls(filterConfig);
        adminMetrics = new HashMap<String, Meter>(4);
        adminMetrics.put(pingUrl,Metrics.newMeter(responseCodeFilterClass, "pingMetricRequests", "requests", TimeUnit.SECONDS));
        adminMetrics.put(threadUrl,Metrics.newMeter(responseCodeFilterClass, "threadsMetricRequests", "requests", TimeUnit.SECONDS));
        adminMetrics.put(metricsUrl,Metrics.newMeter(responseCodeFilterClass, "metricsMetricRequests", "requests", TimeUnit.SECONDS));
        adminMetrics.put(healthUrl,Metrics.newMeter(responseCodeFilterClass, "healthMetricRequests", "requests", TimeUnit.SECONDS));
    }

    private String getInitParam(String paramName,String defaultValue, FilterConfig filterConfig) {
        String parameterValue = filterConfig.getInitParameter(paramName);
        if(parameterValue==null || parameterValue.length()==0) {
            return defaultValue;
        } else {
            return parameterValue;
        }
    }

    private synchronized void readAdminConfigUrls(FilterConfig filterConfig) {
        pingUrl = getInitParam(PING_ADMIN_URL_PARAM,PING_ADMIN_URL,filterConfig);
        metricsUrl = getInitParam(METRIC_ADMIN_URL_PARAM,METRIC_ADMIN_URL,filterConfig);
        healthUrl = getInitParam(HEALTH_ADMIN_URL_PARAM,HEALTH_ADMIN_URL,filterConfig);
        threadUrl = getInitParam(THREAD_ADMIN_URL_PARAM,THREAD_ADMIN_URL,filterConfig);
    }

    private Gauge[] createNonSuccessRatio(Meter requestsPerSecond,Meter responseCodeMeter, String responseCode) {
        metricNames.add("percent-" + responseCode + "-1m");
        metricNames.add("percent-" + responseCode + "-5m");

        return new Gauge[] {
            Metrics.newGauge(responseCodeFilterClass,"percent-" + responseCode + "-1m","requests",createOneMinRatio(requestsPerSecond,responseCodeMeter)),
            Metrics.newGauge(responseCodeFilterClass,"percent-" + responseCode + "-5m","requests",createFiveMinRatio(requestsPerSecond, responseCodeMeter))
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

    private synchronized void createMetrics() {
        metricNames = new ArrayList<String>();

        // Requests Handled Per Second
        requestsPerSecond = Metrics.newMeter(responseCodeFilterClass, "requestsPerSecond", "requests", TimeUnit.SECONDS);
        metricNames.add("requests");
        // The XXX responses per second
        this.responses = new Meter[]{
                Metrics.newMeter(responseCodeFilterClass, "1xx-responses", "responses", TimeUnit.SECONDS), // 1xx
                Metrics.newMeter(responseCodeFilterClass, "2xx-responses", "responses", TimeUnit.SECONDS), // 2xx
                Metrics.newMeter(responseCodeFilterClass, "3xx-responses", "responses", TimeUnit.SECONDS), // 3xx
                Metrics.newMeter(responseCodeFilterClass, "4xx-responses", "responses", TimeUnit.SECONDS), // 4xx
                Metrics.newMeter(responseCodeFilterClass, "5xx-responses", "responses", TimeUnit.SECONDS), // 5xx
                Metrics.newMeter(responseCodeFilterClass, "unknown-responses", "responses", TimeUnit.SECONDS) // 5xx
        };
        metricNames.add("1xx-responses");
        metricNames.add("2xx-responses");
        metricNames.add("3xx-responses");
        metricNames.add("4xx-responses");
        metricNames.add("5xx-responses");
        metricNames.add("unknown-responses");

        nonSuccessCodes = new ArrayList<Gauge>(6);

        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[2], "3xx"));
        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[3], "4xx"));
        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[4], "5xx"));

        timeTakenForGetsPerSecond = Metrics.newTimer(responseCodeFilterClass, "get-requests","requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForPostPerSecond = Metrics.newTimer(responseCodeFilterClass, "post-requests","requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForHeadPerSecond = Metrics.newTimer(responseCodeFilterClass, "head-requests","requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForPutPerSecond = Metrics.newTimer(responseCodeFilterClass, "put-requests","requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForDeletePerSecond = Metrics.newTimer(responseCodeFilterClass, "delete-requests","requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForOtherRequestTypesForSecond = Metrics.newTimer(responseCodeFilterClass, "other-requests","requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

        metricNames.add("get-requests");
        metricNames.add("post-requests");
        metricNames.add("head-requests");
        metricNames.add("put-requests");
        metricNames.add("delete-requests");
        metricNames.add("other-requests");

        metricNames.add("ping-admin-requests");
        metricNames.add("metrics-admin-requests");
        metricNames.add("threads-admin-requests");
        metricNames.add("health-admin-requests");

    }

    @Override
    public void destroy() {
        MetricsRegistry reg = Metrics.defaultRegistry();
        for(String c : metricNames) {
            reg.removeMetric(responseCodeFilterClass,c);
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
