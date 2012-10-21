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
import com.yammer.metrics.util.RatioGauge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        createMetrics();

    }

    private Gauge[] createNonSuccessRatio(Meter requestsPerSecond,Meter responseCodeMeter, String responseCode) {
        metricNames.add("percent-" + responseCode + "-1m");
        metricNames.add("percent-" + responseCode + "-5m");

        return new Gauge[] {
            Metrics.newGauge(responseCodeFilterClass,"percent-" + responseCode + "-1m",createOneMinRatio(requestsPerSecond,responseCodeMeter)),
            Metrics.newGauge(responseCodeFilterClass,"percent-" + responseCode + "-5m",createFiveMinRatio(requestsPerSecond, responseCodeMeter))
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
        addRatioGauge(nonSuccessCodes,createNonSuccessRatio(requestsPerSecond, responses[3], "5xx"));

        timeTakenForGetsPerSecond = Metrics.newTimer(responseCodeFilterClass, "get-requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForPostPerSecond = Metrics.newTimer(responseCodeFilterClass, "post-requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForHeadPerSecond = Metrics.newTimer(responseCodeFilterClass, "head-requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForPutPerSecond = Metrics.newTimer(responseCodeFilterClass, "put-requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForDeletePerSecond = Metrics.newTimer(responseCodeFilterClass, "delete-requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        timeTakenForOtherRequestTypesForSecond = Metrics.newTimer(responseCodeFilterClass, "other-requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

        metricNames.add("get-requests");
        metricNames.add("post-requests");
        metricNames.add("head-requests");
        metricNames.add("put-requests");
        metricNames.add("delete-requests");
        metricNames.add("other-requests");

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
