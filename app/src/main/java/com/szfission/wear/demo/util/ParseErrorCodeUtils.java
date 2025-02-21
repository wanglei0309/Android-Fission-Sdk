package com.szfission.wear.demo.util;

import com.baidu.mapapi.search.core.SearchResult;

import java.util.HashMap;

/**
 * describe:
 * author: wl
 * createTime: 2024/2/29
 */
public class ParseErrorCodeUtils {
    private static ParseErrorCodeUtils sInstance;
    private static HashMap<SearchResult.ERRORNO, Integer> errorCodeMap;

    public static ParseErrorCodeUtils getInstance() {
        if (null == sInstance) {
            sInstance = new ParseErrorCodeUtils();
            putErrorCode();
        }
        return sInstance;
    }

    private static void putErrorCode() {
        errorCodeMap = new HashMap<>();
        errorCodeMap.put(SearchResult.ERRORNO.NO_ERROR, 0);
        errorCodeMap.put(SearchResult.ERRORNO.AMBIGUOUS_KEYWORD, 1);
        errorCodeMap.put(SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR, 2);
        errorCodeMap.put(SearchResult.ERRORNO.NOT_SUPPORT_BUS, 3);
        errorCodeMap.put(SearchResult.ERRORNO.NOT_SUPPORT_BUS_2CITY, 4);
        errorCodeMap.put(SearchResult.ERRORNO.RESULT_NOT_FOUND, 5);
        errorCodeMap.put(SearchResult.ERRORNO.ST_EN_TOO_NEAR, 6);
        errorCodeMap.put(SearchResult.ERRORNO.KEY_ERROR, 7);
        errorCodeMap.put(SearchResult.ERRORNO.NETWORK_ERROR, 8);
        errorCodeMap.put(SearchResult.ERRORNO.NETWORK_TIME_OUT, 9);
        errorCodeMap.put(SearchResult.ERRORNO.PERMISSION_UNFINISHED, 10);
        errorCodeMap.put(SearchResult.ERRORNO.POIINDOOR_BID_ERROR, 11);
        errorCodeMap.put(SearchResult.ERRORNO.POIINDOOR_FLOOR_ERROR, 12);
        errorCodeMap.put(SearchResult.ERRORNO.INDOOR_ROUTE_NO_IN_BUILDING, 13);
        errorCodeMap.put(SearchResult.ERRORNO.INDOOR_ROUTE_NO_IN_SAME_BUILDING, 14);
        errorCodeMap.put(SearchResult.ERRORNO.SEARCH_OPTION_ERROR, 15);
        errorCodeMap.put(SearchResult.ERRORNO.SEARCH_SERVER_INTERNAL_ERROR, 16);
        errorCodeMap.put(SearchResult.ERRORNO.NO_ADVANCED_PERMISSION, 18);
        errorCodeMap.put(SearchResult.ERRORNO.NO_DATA_FOR_LATLNG, 19);
        errorCodeMap.put(SearchResult.ERRORNO.INVALID_DISTRICT_ID, 20);
        errorCodeMap.put(SearchResult.ERRORNO.POIINDOOR_SERVER_ERROR, 21);
        errorCodeMap.put(SearchResult.ERRORNO.MASS_TRANSIT_SERVER_ERROR, 22);
        errorCodeMap.put(SearchResult.ERRORNO.MASS_TRANSIT_OPTION_ERROR, 23);
        errorCodeMap.put(SearchResult.ERRORNO.MASS_TRANSIT_NO_POI_ERROR, 24);
        errorCodeMap.put(SearchResult.ERRORNO.REQUEST_ERROR, 25);
    }

    public int getErrorCode(SearchResult.ERRORNO errorCode) {

        return errorCodeMap.get(errorCode);
    }
}
