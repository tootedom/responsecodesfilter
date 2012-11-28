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

import com.yammer.metrics.core.TimerContext;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Extension of ResponseCodeFilter for Async Servlet 3 spec
 *
 * User: dominictootell
 * Date: 18/05/2012
 * Time: 09:26
 */
public class AsyncResponseCodeFilter extends ResponseCodeFilter
{
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final StatusExposingServletResponse wrappedResponse =
                new StatusExposingServletResponse((HttpServletResponse) response);

        HttpServletRequest servletRequest = (HttpServletRequest)request;
        try {
            AsyncContext asyncContext = servletRequest.startAsync(request,response);
            asyncContext.addListener(new AsyncMetricsRequestResponseListener(servletRequest));
            chain.doFilter(request, wrappedResponse);
        } finally {
//
        }
    }


    class AsyncMetricsRequestResponseListener implements AsyncListener {

        private final TimerContext timer;
        public AsyncMetricsRequestResponseListener(HttpServletRequest servletRequest) {
            timer = getTimerForCurrentRequestMethodType(servletRequest.getMethod()).time();
        }

        @Override
        public void onComplete(AsyncEvent asyncEvent) throws IOException {
            timer.stop();
            updateResponseRate(((HttpServletResponse) asyncEvent.getAsyncContext().getResponse()).getStatus());
            updateAdminMetricsIfRequestMatched(((HttpServletRequest) asyncEvent.getAsyncContext().getRequest()).getServletPath());
        }

        @Override
        public void onTimeout(AsyncEvent asyncEvent) throws IOException {
            onComplete(asyncEvent);
        }

        @Override
        public void onError(AsyncEvent asyncEvent) throws IOException {
            onComplete(asyncEvent);
        }

        @Override
        public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
        }
    }
}
