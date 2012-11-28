package org.greencheek.yammer.metrics.web.filter.utils;

import com.google.gson.GsonBuilder;
import org.greencheek.yammer.metrics.web.filter.ResponseCodeFilter;

import java.util.Map;

/**
 * User: dominictootell
 * Date: 28/11/2012
 * Time: 13:18
 */
public class MetricsFromJson {

    ResponseCodeFilter filter;

    enum HttpMetricType {
        REQUEST ("requests"),
        RESPONSE ("responses");

        private String type;

        HttpMetricType(String type) {
            this.type = type;
        }

        public String toString(){
            return type;
        }
    }

    public MetricsFromJson(ResponseCodeFilter filter) {
        this.filter = filter;
    }

    public double getTimerDurationValue(String json, String timerName, String value) {
        return getRequestTimerValue(json, timerName, "duration", value);
    }

    public double getRequestTimerValue(String json, String timerName, String timerSection, String value) {
        return getValue(json,timerName,timerSection,value,HttpMetricType.REQUEST);
    }

    private double getValue(String json, String timerName, String timerSection,String value, HttpMetricType type)
    {
        Map m = new GsonBuilder().create().fromJson(json,Map.class);
        Map<String,Map> requests =  getRequestOrResponseMap(m,type.toString()).get(timerName);
        Double val = (Double)requests.get(timerSection).get(value);
        return val;
    }

    private Map<String,Map> getRequestOrResponseMap(Map m, String type) {
        return ((Map<String,Map>)m.get(
                filter.getMetricsGroupName()
                        + "." + type));
    }

    public double getMeterValue(String json, String meterName, String value) {
        return getRequestMeterValue(json,meterName,value);
    }

    public double getRequestMeterCount(String json, String meterName) {
        return getMeterValue(json, meterName, "count");
    }

    public double getResponseMeterCount(String json, String meterName) {
        return getResponseMeterValue(json, meterName, "count");
    }

    public double getMeterMean(String json, String meterName) {
        return getMeterValue(json, meterName, "mean");
    }

    public double getRequestMeterValue(String json, String meterName, String dataPoint) {
        return getMeterValue(json,meterName,dataPoint,HttpMetricType.REQUEST);
//        Map m = new GsonBuilder().create().fromJson(json,Map.class);
//        Map<String,Map> requests = (Map<String,Map>)m.get(
//                        filter.getMetricsGroupName()
//                        + ".requests");
//
//        Double val = (Double)requests.get(meterName).get(dataPoint);
//        return val;
    }

    public double getResponseMeterValue(String json, String meterName, String dataPoint) {
        return getMeterValue(json,meterName,dataPoint,HttpMetricType.RESPONSE);
    }

    private double getMeterValue(String json, String meterName, String dataPoint,HttpMetricType type) {

        Map m = new GsonBuilder().create().fromJson(json,Map.class);
        Map<String,Map> requests = getRequestOrResponseMap(m,type.toString());

        Double val = (Double)requests.get(meterName).get(dataPoint);
        return val;
    }
}
