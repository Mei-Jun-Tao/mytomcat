package cn.how2j.mytomcat.util;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import java.util.HashMap;
import java.util.Map;

public class ServletPool {

    private static Map<String, HttpServlet> servletMap; //存放servlet的池子
    private static Map<String, Filter> filterMap; //存放filter的池子

    private ServletPool(){

    }

    public static Map<String, HttpServlet> getServletPool(){
        if(servletMap == null)
            servletMap = new HashMap<>();

        return servletMap;
    }

    public static Map<String, Filter> getFilterMap(){
        if(filterMap == null)
            filterMap = new HashMap<>();

        return filterMap;
    }
}
