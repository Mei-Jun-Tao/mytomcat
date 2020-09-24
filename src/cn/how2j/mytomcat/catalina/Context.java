package cn.how2j.mytomcat.catalina;

import cn.how2j.mytomcat.classloader.WebappClassLoader;
import cn.how2j.mytomcat.http.ApplicationContext;
import cn.how2j.mytomcat.http.StandardServletConfig;
import cn.how2j.mytomcat.util.ContextXMLUtil;
import cn.how2j.mytomcat.util.ServletPool;
import cn.how2j.mytomcat.watchar.ContextFileChangeWatcher;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

public class Context {
    //server.xml中的一种位置元素，用来存放指定文件的位置和访问路径的

    private String path; //访问的路径
    private String docBase; //文件所在的目录

    private Host host; //父类对象
    private boolean reloadable; //判断是否需要热加载

    private File contextWebXmlFile; //WEB-INF/web.xml文件

    private WebappClassLoader webappClassLoader; //web应用加载器
    private ContextFileChangeWatcher contextFileChangeWatcher; //文件监听器

    private ServletContext servletContext; //jsp的获取数据属性

    public Context(String path, String docBase, Host host, boolean reloadable){
        this.path = path;
        this.docBase = docBase;
        this.host = host;
        this.reloadable = reloadable;
        this.servletContext = new ApplicationContext(this);

        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        deploy();
    }

    //初始化方法
    private void init(){
        //先判断这个文件是否存在
        if(!contextWebXmlFile.exists())
            return;
        //存在就判断是否有重复的
        try{
            ContextXMLUtil.checkDuplicated(contextWebXmlFile);
        }catch(Exception e){
            e.printStackTrace();
            return;
        }

        //解析web.xml文件
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        ContextXMLUtil.parseServletMapping(d);
        ContextXMLUtil.parseServletInitParams(d);

        //自启动
        handleLoadOnStartup();

        //解析Filter
        ContextXMLUtil.parseServletMapping(d);
        ContextXMLUtil.parseServletInitParams(d);

        //初始化Filter
        initFilter();

        System.out.println("文件1：" + ContextXMLUtil.getServletName_className());
        System.out.println("文件2：" + ContextXMLUtil.getClassName_servletName());
        System.out.println("文件3：" + ContextXMLUtil.getUrl_servletName());
        System.out.println("文件4：" + ContextXMLUtil.getUrl_servletClassName());
        System.out.println("文件5：" + ContextXMLUtil.getServlet_className_init_params());
    }

    //打印日志
    private void deploy() {
        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);

        init();

        //看是否要启动
        if(reloadable){
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }

        //处理jsp的
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);

        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms",this.getDocBase(),timeInterval.intervalMs());
    }

    //根据uri获取类名
    public String getServletClassName(String uri){
        return ContextXMLUtil.getUrl_servletClassName().get(uri);
    }

    //把监听器和web类加载器停止了
    public void stop(){
        contextFileChangeWatcher.stop();
        webappClassLoader.stop();
        destroyServlets();
    }

    //context重加载
    public void reload(){
        host.reload(this);
    }

    //获取servlet对象
    public synchronized HttpServlet getServlet(Class<?> clazz) throws Exception {
        String className = clazz.getName();
        HttpServlet servlet = ServletPool.getServletPool().get(className);
        if (null == servlet) {
            servlet = (HttpServlet) clazz.newInstance();
            //在放入池子前初始化参数
            ServletContext servletContext = this.getServletContext();
            Map<String, String> initParameters = ContextXMLUtil.getServlet_className_init_params().get(className);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, className, initParameters);
            servlet.init(servletConfig);

            ServletPool.getServletPool().put(className, servlet);
        }

        return servlet;
    }

    //自启动
    public void handleLoadOnStartup() {
        List<String> loadOnStartupServletClassNames = ContextXMLUtil.getLoadOnStartupServletClassNames();
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //用于销毁所有的servlets
    private void destroyServlets(){
        //获取Map集合的的值
        Collection<HttpServlet> servlets = ServletPool.getServletPool().values();
        for(HttpServlet servlet : servlets){
            servlet.destroy();
        }
    }

    //初始化Filter
    private void initFilter() {
        Set<String> classNames = ContextXMLUtil.getClassName_filterName().keySet();
        for (String className : classNames) {
            try {
                Class clazz =  this.getWebappClassLoader().loadClass(className);
                Map<String,String> initParameters = ContextXMLUtil.getFilter_className_init_params().get(className);
                String filterName = ContextXMLUtil.getClassName_filterName().get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, filterName, initParameters);
                Filter filter = ServletPool.getFilterMap().get(clazz);
                if(null==filter) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    ServletPool.getFilterMap().put(className, filter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //Filter的匹配方式
    private boolean match(String pattern, String uri) {
        // 完全匹配
        if(StrUtil.equals(pattern, uri))
            return true;
        // /* 模式
        if(StrUtil.equals(pattern, "/*"))
            return true;
        // 后缀名 /*.jsp
        if(StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            if(StrUtil.equals(patternExtName, uriExtName))
                return true;
        }
        // 其他模式就懒得管了
        return false;
    }
    //获取匹配了的Filter集合
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();

        //获取url的键值
        Set<String> patterns = ContextXMLUtil.getUrl_filterClassName().keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for (String pattern : patterns) {
            //看这个键值是否和uri匹配
            if(match(pattern,uri)) {
                matchedPatterns.add(pattern);
            }
        }

        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            //根据键值获取filter类名
            List<String> filterClassName = ContextXMLUtil.getUrl_filterClassName().get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for (String filterClassName : matchedFilterClassNames) {
            //根据类名获取filer
            Filter filter = ServletPool.getFilterMap().get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    public String getPath() {
        return path;
    }

    public String getDocBase() {
        return docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }
}
