package cn.how2j.mytomcat.util;

import cn.how2j.mytomcat.exception.WebConfigDuplicatedException;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.*;

public class ContextXMLUtil {
    //用来解析context.xml和web.xml文件内容的

    private static Map<String, String> url_servletClassName = new HashMap<>(); //访问路径对应类全名称
    private static Map<String, String> url_servletName = new HashMap<>(); //访问路径对应类的名称
    private static Map<String, String> servletName_className = new HashMap<>(); //全名称对应简单名
    private static Map<String, String> className_servletName = new HashMap<>(); //简单名对应全名称

    private static Map<String, Map<String, String>> servlet_className_init_params = new HashMap<>(); //获取初始化参数

    private static List<String> loadOnStartupServletClassNames = new ArrayList<>(); //自启动类

    //和servlet一样的属性
    private static Map<String, List<String>> url_filterClassName = new HashMap<>();
    private static Map<String, List<String>> url_FilterNames = new HashMap<>();
    private static Map<String, String> filterName_className = new HashMap<>();
    private static Map<String, String> className_filterName = new HashMap<>();
    private static Map<String, Map<String, String>> filter_className_init_params = new HashMap<>();

    //解析context.xml文件
    public static String getWatchedResource(){
        try{
            String xml = FileUtil.readUtf8String(Constant.contextFload);

            Document d = Jsoup.parse(xml);
            Element e = d.select("WatchedResource").first();

            return e.text();
        }catch(Exception e){
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }

    //解析web.xml文件
    public static void parseServletMapping(Document d){

        //url_servletName
        Elements es = d.select("servlet-mapping url-pattern");
        for(Element e : es){
            String url = e.text();
            //当前同级的元素内容
            String servletName = e.parent().select("servlet-name").text();
            //添加到集合中
            url_servletName.put(url, servletName);
        }

        //servletName_className和className_servletName
        Elements es1 = d.select("servlet servlet-class");
        for(Element e : es1){
            String servletName = e.text();
            String className = e.parent().select("servlet-name").text();

            servletName_className.put(servletName, className);
            className_servletName.put(className, servletName);
        }

        //url_className
        Set<String> urls = url_servletName.keySet();
        for(String url : urls){
            String servletName = url_servletName.get(url);
            String className = className_servletName.get(servletName);

            url_servletClassName.put(url, className);
        }
    }

    //判断是否有重复的
    public static void checkDuplicated(File contextWebXmlFile) throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);

        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }
    public static void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements es = d.select(mapping);

        //把解析出来的内容存放入集合中
        List<String> contents = new ArrayList<>();
        for(Element e : es){
            contents.add(e.text());
        }

        //进行排序一下
        Collections.sort(contents);

        //判断相邻的两个元素是否相等
        for(int i=0; i<contents.size()-1; i++){
            String contentPre = contents.get(i);
            String contentNext = contents.get(i+1);

            if(contentPre.equals(contentNext)){
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }

    //获取初始化参数
    public static void parseServletInitParams(Document d){
        Elements servletClassNameElements = d.select("servlet-class");
        for(Element servletClassNameElement : servletClassNameElements){
            //获取内容
            String servletClassName = servletClassNameElement.text();

            Elements initEments = servletClassNameElement.parent().select("init-param");
            if(initEments.isEmpty())
                continue;

            Map<String, String> initParams = new HashMap<>();
            for(Element element : initEments){
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            servlet_className_init_params.put(servletClassName, initParams);
        }
    }

    //获取那些类需要自启动
    public void parseLoadOnStartup(Document d) {
        Elements es = d.select("load-on-startup");
        for (Element e : es) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    //解析Filter
    public void parseFilterMapping(Document d) {
        // filter_url_name
        Elements mappingurlElements = d.select("filter-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String filterName = mappingurlElement.parent().select("filter-name").first().text();

            List<String> filterNames= url_FilterNames.get(urlPattern);
            if(null==filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }
        // class_name_filter_name
        Elements filterNameElements = d.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String filterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName_className.put(filterName, filterClass);
            className_filterName.put(filterClass, filterName);
        }
        // url_filterClassName

        Set<String> urls = url_FilterNames.keySet();
        for (String url : urls) {
            List<String> filterNames = url_FilterNames.get(url);
            if(null == filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if(null==filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }
    //解析参数信息
    private void parseFilterInitParams(Document d) {
        Elements filterClassNameElements = d.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;


            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            filter_className_init_params.put(filterClassName, initParams);

        }
    }

    public static Map<String, String> getUrl_servletClassName() {
        return url_servletClassName;
    }

    public static Map<String, String> getUrl_servletName() {
        return url_servletName;
    }

    public static Map<String, String> getServletName_className() {
        return servletName_className;
    }

    public static Map<String, String> getClassName_servletName() {
        return className_servletName;
    }

    public static Map<String, Map<String, String>> getServlet_className_init_params() {
        return servlet_className_init_params;
    }

    public static List<String> getLoadOnStartupServletClassNames() {
        return loadOnStartupServletClassNames;
    }

    public static Map<String, List<String>> getUrl_filterClassName() {
        return url_filterClassName;
    }

    public static Map<String, List<String>> getUrl_FilterNames() {
        return url_FilterNames;
    }

    public static Map<String, String> getFilterName_className() {
        return filterName_className;
    }

    public static Map<String, String> getClassName_filterName() {
        return className_filterName;
    }

    public static Map<String, Map<String, String>> getFilter_className_init_params() {
        return filter_className_init_params;
    }
}
