package com.adobe.aem.support.contentbackflow.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HttpHelperTest {

    @Test
    public void testQueryStringParamwithNull() {
        HttpHelper helper = new HttpHelper(null);
        assertEquals(null, helper.getQueryStringByKey("test"));
    }

    
    @Test
    public void testQueryStringParamwithString() {
        HttpHelper helper = new HttpHelper("test=100");
        assertEquals("100", helper.getQueryStringByKey("test"));
    }
}
