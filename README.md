## How to obtain the library


```xml
    <dependency>
       <groupId>org.greencheek.yammer.metrics</groupId>
       <artifactId>metrics-filter-responsecodes</artifactId>
       <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

The maven repositories are located at:

```
   https://raw.github.com/tootedom/tootedom-mvn-repo/master/releases/
   https://raw.github.com/tootedom/tootedom-mvn-repo/master/snapshots/
```

## Overview

A filter that is an extension of "**com.yammer.metrics.web.DefaultWebappMetricsFilter**" (http://metrics.codahale.com/manual/webapps/)
that returns the following metrics:

### Timers
- delete-requests
- get-requests
- post-requests
- put-requests
- head-requests
- other-requests

### Meters
- requestsPerSecond
- healthMonitoringRequests
- metricsMonitoringRequests
- pingMonitoringRequests
- threadsMonitoringRequests
- 1xx-responses
- 2xx-responses
- 3xx-responses
- 4xx-responses
- 5xx-responses
- unknown-responses

### Gauges
- percent-3xx-1m
- percent-3xx-5m
- percent-4xx-1m
- percent-4xx-5m
- percent-5xx-1m
- percent-5xx-5m

You can scroll to the bottom to see example output from the metrics servlet, with the monitoring data in Json format.


## Usage

There are two filters.  One for Synchronous usage (call it for a normal servlet 2.5 and below), one for Asynchronous usage (i.e. Async servlet 3).
To use the filter, you just need to pull in the library as a run time dependency; and then setup the filter in your **web.xml**.

### Async:

```xml
    <filter>
        <filter-name>response-code-filter</filter-name>
        <filter-class>org.greencheek.yammer.metrics.web.filter.AsyncResponseCodeFilter</filter-class>
        <async-supported>true</async-supported>
    </filter>

    <filter-mapping>
        <filter-name>response-code-filter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
```

### Sync:

```xml
    <filter>
        <filter-name>response-code-filter</filter-name>
        <filter-class>org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter</filter-class>
        <async-supported>true</async-supported>
    </filter>

    <filter-mapping>
        <filter-name>response-code-filter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
```

### Changing the name under which the metrics are registered.

By default the "group" is **org.greencheek.yammer.metrics.web** and the "type" is the name of the filter, i.e.
**response-code-filter**.  These can be changed by the following filter init parameters:

```xml
    <init-param>
        <param-name>monitoring-group-name</param-name>
        <param-value>com.blogspot.asynctesting</param-value>
    </init-param>
    <init-param>
        <param-name>monitoring-type-name</param-name>
        <param-value>requestresponse-metrics</param-value>
    </init-param>
```

## Async Usage Info

Just a hint, if using the Async Filter.  The AsyncResponseCodeFilter will start the AsyncContext; so in your
Async Servlet 3 servlet, make sure you check if the HttpServletRequest is already in a Async.  For example:

```java
    final AsyncContext asyncContext;
    if(request.isAsyncStarted()) {
        asyncContext = request.getAsyncContext();
    }
    else {
        asyncContext = request.startAsync(request, response);
    }
```

## Example Json Output

```json
    {
       "org.greencheek.yammer.metrics.web.filter.response-code-filter.requests":{
          "delete-requests":{
             "type":"timer",
             "duration":{
                "unit":"milliseconds",
                "min":0.0,
                "max":0.0,
                "mean":0.0,
                "std_dev":0.0,
                "median":0.0,
                "p75":0.0,
                "p95":0.0,
                "p98":0.0,
                "p99":0.0,
                "p999":0.0
             },
             "rate":{
                "unit":"seconds",
                "count":0,
                "mean":0.0,
                "m1":0.0,
                "m5":0.0,
                "m15":0.0
             }
          },
          "get-requests":{
             "type":"timer",
             "duration":{
                "unit":"milliseconds",
                "min":0.48,
                "max":5652.043,
                "mean":183.66684660992,
                "std_dev":589.0787743990034,
                "median":22.401,
                "p75":379.95625,
                "p95":2742.5903,
                "p98":2793.5307199999997,
                "p99":2851.9766900000004,
                "p999":3207.598006
             },
             "rate":{
                "unit":"seconds",
                "count":1154840,
                "mean":1529.9297596455096,
                "m1":194.05488014233828,
                "m5":713.5257499615096,
                "m15":701.1966428751969
             }
          },
          "head-requests":{
             "type":"timer",
             "duration":{
                "unit":"milliseconds",
                "min":0.0,
                "max":0.0,
                "mean":0.0,
                "std_dev":0.0,
                "median":0.0,
                "p75":0.0,
                "p95":0.0,
                "p98":0.0,
                "p99":0.0,
                "p999":0.0
             },
             "rate":{
                "unit":"seconds",
                "count":0,
                "mean":0.0,
                "m1":0.0,
                "m5":0.0,
                "m15":0.0
             }
          },
          "healthMonitoringRequests":{
             "type":"meter",
             "event_type":"requests",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "metricsMonitoringRequests":{
             "type":"meter",
             "event_type":"requests",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "other-requests":{
             "type":"timer",
             "duration":{
                "unit":"milliseconds",
                "min":0.0,
                "max":0.0,
                "mean":0.0,
                "std_dev":0.0,
                "median":0.0,
                "p75":0.0,
                "p95":0.0,
                "p98":0.0,
                "p99":0.0,
                "p999":0.0
             },
             "rate":{
                "unit":"seconds",
                "count":0,
                "mean":0.0,
                "m1":0.0,
                "m5":0.0,
                "m15":0.0
             }
          },
          "percent-3xx-1m":{
             "type":"gauge",
             "value":0.0
          },
          "percent-3xx-5m":{
             "type":"gauge",
             "value":0.0
          },
          "percent-4xx-1m":{
             "type":"gauge",
             "value":0.0
          },
          "percent-4xx-5m":{
             "type":"gauge",
             "value":0.0
          },
          "percent-5xx-1m":{
             "type":"gauge",
             "value":0.0
          },
          "percent-5xx-5m":{
             "type":"gauge",
             "value":0.0
          },
          "pingMonitoringRequests":{
             "type":"meter",
             "event_type":"requests",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "post-requests":{
             "type":"timer",
             "duration":{
                "unit":"milliseconds",
                "min":0.0,
                "max":0.0,
                "mean":0.0,
                "std_dev":0.0,
                "median":0.0,
                "p75":0.0,
                "p95":0.0,
                "p98":0.0,
                "p99":0.0,
                "p999":0.0
             },
             "rate":{
                "unit":"seconds",
                "count":0,
                "mean":0.0,
                "m1":0.0,
                "m5":0.0,
                "m15":0.0
             }
          },
          "put-requests":{
             "type":"timer",
             "duration":{
                "unit":"milliseconds",
                "min":0.0,
                "max":0.0,
                "mean":0.0,
                "std_dev":0.0,
                "median":0.0,
                "p75":0.0,
                "p95":0.0,
                "p98":0.0,
                "p99":0.0,
                "p999":0.0
             },
             "rate":{
                "unit":"seconds",
                "count":0,
                "mean":0.0,
                "m1":0.0,
                "m5":0.0,
                "m15":0.0
             }
          },
          "requestsPerSecond":{
             "type":"meter",
             "event_type":"requests",
             "unit":"seconds",
             "count":1154840,
             "mean":1529.8738166344756,
             "m1":194.05782975994873,
             "m5":713.5378876164044,
             "m15":701.2008586582516
          },
          "threadsMonitoringRequests":{
             "type":"meter",
             "event_type":"requests",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          }
       },
       "org.greencheek.yammer.metrics.web.filter.response-code-filter.responses":{
          "1xx-responses":{
             "type":"meter",
             "event_type":"responses",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "2xx-responses":{
             "type":"meter",
             "event_type":"responses",
             "unit":"seconds",
             "count":1154840,
             "mean":1529.8738652752702,
             "m1":194.05782302443814,
             "m5":713.5375722232069,
             "m15":701.200739181918
          },
          "3xx-responses":{
             "type":"meter",
             "event_type":"responses",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "4xx-responses":{
             "type":"meter",
             "event_type":"responses",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "5xx-responses":{
             "type":"meter",
             "event_type":"responses",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          },
          "unknown-responses":{
             "type":"meter",
             "event_type":"responses",
             "unit":"seconds",
             "count":0,
             "mean":0.0,
             "m1":0.0,
             "m5":0.0,
             "m15":0.0
          }
       }
    }
```
