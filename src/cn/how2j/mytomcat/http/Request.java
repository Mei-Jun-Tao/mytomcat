package cn.how2j.mytomcat.http;

import cn.how2j.mytomcat.catalina.Connector;
import cn.how2j.mytomcat.catalina.Context;
import cn.how2j.mytomcat.catalina.Service;
import cn.how2j.mytomcat.util.MiniBrowser;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public class Request extends BaseRequest {

    private String uri; //访问的路径
    private String requestString; //用来保存请求信息的
    private Socket socket;

    private Connector connector;//应用对象
    private Context context;

    private String method; //请求的方法

    private String queryString; //查询字符串
    private Map<String, String[]> parameterMap; //存放参数

    private Map<String, String> headMap; //获取头部信息

    private Cookie[] cookies; //接收浏览器传过来的Cookie

    private HttpSession session; //创建session属性

    private boolean forwarded; //支持服务端跳转

    private Map<String, Object> attributesMap; //用来存放服务端传过来的参数

    public Request(Socket socket, Connector connector) throws Exception {
        this.socket = socket;
        this.connector = connector;
        this.parameterMap = new HashMap<>();
        this.headMap = new HashMap<>();
        this.attributesMap = new HashMap<>();

        parseHttpRequest();
        //如果为空就返回了
        if(requestString.isEmpty())
            return;
        parseUri();
        parseContext();
        parseMethod();
        parseParameters();
        parseHeaders();
        parseCookie();

        if(!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri, context.getPath());
            //为了不让uri为空
            if(uri.equals(""))
                uri = "/";
        }
    }

    //解析出请求信息
    private void parseHttpRequest() throws IOException {
        InputStream is = socket.getInputStream();
        byte[] bytes = MiniBrowser.requestByte(is, false);
        requestString = new String(bytes, "utf-8");
    }
    //解析出uri
    private void parseUri(){
        //其实就是获取两个空格之间的数据
        String temp;

        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }
    //从uri中解析出Context对象
    private void parseContext() throws Exception {
        //如果根据访问路径就能匹配Context对象的就直接返回了,目地是为了不让一直去ROOT目录找
        Service service = connector.getService();
        context = service.getEngine(). getHost().getContext(uri);
        if(context != null)
            return;

        //获取两个/之间的字符串
        String path;

        path = StrUtil.subBetween(uri, "/", "/");
        if(path == null)
            path = "/";
        else
            path = "/" + path;

        System.out.println("path：" + path);
        context = service.getEngine().getHost().getContext(path);
        System.out.println("contextPath：" + context.getPath());

        //如果没有就默认获取
        if(context == null)
            context = service.getEngine().getHost().getContext("/");
    }

    //解析出请求的方法
    private void parseMethod(){
        //就是去第一行空格前的单词
        method = StrUtil.subBefore(requestString, " ", false);
    }

    //根据提交的方法来获取参数
    private void parseParameters(){
        //如果是GET就从uri中获取
        if("GET".equals(method)){
            String uri = StrUtil.subBetween(requestString, " ", " ");
            if(StrUtil.contains(uri, '?'))
                queryString = StrUtil.subAfter(uri, '?', false);
        }
        //如果是POST就从请求体里获取
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString)
            return;

        queryString = URLUtil.decode(queryString);
        //以字符数组的形式获取&两边的数
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                //获取参数名和值
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];

                String values[] = parameterMap.get(name);
                if (null == values) {
                    values = new String[] { value };
                    parameterMap.put(name, values);
                } else {
                    //如果之前有参数值就追加上去
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    //获取头部信息
    private void parseHeaders() {
        //基于返回的的请求信息创建字符输入流
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        //把信息输入到这个集合中
        IoUtil.readLines(stringReader, lines);
        System.out.println("lines: " + lines);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            //获取 : 两边的内容
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headMap.put(headerName, headerValue);
        }
    }

    //获取Cookie上的信息
    private void parseCookie(){
        List<Cookie> cookieList = new ArrayList<>();
        //获取Cookie
        String cookies = headMap.get("cookie");

        if (null != cookies) {
            //获取‘;’ 两边的字符,有多个cookie的时候
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                //获取=两边的值
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    //从Cookie中获取sessionid
    public String getJsessionidFromCookie(){
        if(null == cookies){
            return null;
        }
        for(Cookie cookie : cookies){
            if("JSESSIONID".equals(cookie.getName())){
                return cookie.getValue();
            }
        }
        return null;
    }

    //返回 ApplicationRequestDispatcher 对象
    public RequestDispatcher getRequestDispatcher(String uri){
        return new ApplicationRequestDispatcher(uri);
    }

    //获取参数
    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }
    public Map getParameterMap() {
        return parameterMap;
    }
    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    //获取头部信息
    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headMap.get(name);
    }
    public Enumeration getHeaderNames() {
        Set keys = headMap.keySet();
        return Collections.enumeration(keys);
    }
    public int getIntHeader(String name) {
        String value = headMap.get(name);
        return Convert.toInt(value, 0);
    }

    //服务端传参
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    public Context getContext() {
        return context;
    }

    public ServletContext getServletContext(){
        return context.getServletContext();
    }

    public String getRealPath(String path){
        return getServletContext().getRealPath(path);
    }

    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    public String getProtocol() {
        return "HTTP:/1.1";
    }
    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }
    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }
    public int getRemotePort() {
        return socket.getPort();
    }
    public String getScheme() {
        return "http";
    }
    public String getServerName() {
        return getHeader("host").trim();
    }
    public int getServerPort() {
        return getLocalPort();
    }
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }
    public String getRequestURI() {
        return uri;
    }
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }
    public String getServletPath() {
        return uri;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isForwarded() {
        return forwarded;
    }
    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

}
