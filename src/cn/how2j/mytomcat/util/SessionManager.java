package cn.how2j.mytomcat.util;

import cn.how2j.mytomcat.http.Request;
import cn.how2j.mytomcat.http.Response;
import cn.how2j.mytomcat.http.StandardSession;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

public class SessionManager {

    private static Map<String, StandardSession> sessionMap = new HashMap<>();

    private static int defaultTimeout = getTimeout(); //默认失效时间

    //获取默认失效时间
    private static int getTimeout(){
        int defaultResult = 30;

        try{
            Document d = Jsoup.parse(Constant.webXMLFload, "utf-8");
            Elements es = d.select("session-config session-timeout");
            if(es.isEmpty())
                return defaultResult;
            return Convert.toInt(es.get(0).text());
        }catch(IOException e){
            return defaultResult;
        }
    }

    //默认启动检测Session失效的线程
    static {
        startSessionOutdateCheckThread();
    }
    //每隔30秒就检查一下
    private static void startSessionOutdateCheckThread(){
        new Thread(){
            public void run(){
                while(true){
                    checkOutDateSession();
                    ThreadUtil.sleep(1000*30);
                }
            }
        }.start();
    }
    //检查是否失效
    private static void checkOutDateSession(){
        //获取所有的Session的键
        Set<String> jsessionids = sessionMap.keySet();
        //用来存放失效的Session
        List<String> outdateJessionids = new ArrayList<>();
        //遍历
        for(String jsessionid : jsessionids){
            //根据键获取Session
            StandardSession session = sessionMap.get(jsessionid);
            //创建时间减去最后一次访问的时间
            long interval = System.currentTimeMillis() - session.getLastAccessedTime();
            //判断时间是否大于最大默认时间
            if(interval > session.getMaxInactiveInterval() * 1000)
                outdateJessionids.add(jsessionid);
        }

        for(String jsessionid : outdateJessionids){
            sessionMap.remove(jsessionid);
        }
    }

    //创建sessionid
    public static synchronized String generateSessionId(){
        String result = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();
        return result;
    }

    //获取Session
    public static HttpSession getSession(String jessionid, Request request, Response response){
        //如果为空就新建一个Session
        if(null == jessionid) {
            return newSession(request, response);
        }else{
            //如果是失效的也新建
            StandardSession currentSession = sessionMap.get(jessionid);
            if(null == currentSession){
                return newSession(request, response);
            }else{
                //使用现成的session, 并且修改它的lastAccessedTime， 以及创建对应的 cookie
                currentSession.setLastAccessedTime(System.currentTimeMillis());
                return currentSession;
            }
        }
    }
    //创建Session
    private static HttpSession newSession(Request request, Response response) {
        ServletContext servletContext = request.getServletContext();
        String sid = generateSessionId();
        StandardSession session = new StandardSession(sid, servletContext);
        //设置持续最大的时间
        session.setMaxInactiveInterval(defaultTimeout);
        sessionMap.put(sid, session);
        //创建cookie
        createCookieBySession(session, request, response);
        return session;
    }

    //创建cookie
    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    public static Map<String, StandardSession> getSessionMap() {
        return sessionMap;
    }
}
