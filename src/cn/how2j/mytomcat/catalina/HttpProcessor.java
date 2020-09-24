package cn.how2j.mytomcat.catalina;

import cn.how2j.mytomcat.http.Request;
import cn.how2j.mytomcat.http.Response;
import cn.how2j.mytomcat.servlets.DefaultServlet;
import cn.how2j.mytomcat.servlets.InvokerServlet;
import cn.how2j.mytomcat.servlets.JspServlet;
import cn.how2j.mytomcat.util.Constant;
import cn.how2j.mytomcat.util.SessionManager;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class HttpProcessor {
    //专门用来处理请求的

    public static void execute(Socket s, Request request, Response response){
        try{
            String uri = request.getUri();
            System.out.println("uri：" + uri);
            System.out.println("浏览器发出的请求：" + request.getRequestString());

            prepareSession(request, response);

            Context context = request.getContext();

            String servletClassName = context.getServletClassName(uri);
            HttpServlet workingServlet;
            if(servletClassName != null){
                //专门用来处理Servlet的
                workingServlet = InvokerServlet.getInstance();
            }else if(uri.endsWith(".jsp")){
                //专门用来处理Jsp的
                workingServlet = JspServlet.getInstance();
            }else {
                //专门用来处理静态页面的
                workingServlet = DefaultServlet.getDefaultServlet();
            }

            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            if(request.isForwarded())
                return;

            if(response.getStatus() == Constant.CODE_500){
                throw new Exception("the is mytomcat!");
            }
            if(response.getStatus() == Constant.CODE_404) {
                handle404(s, uri);
                return;
            }
            if(response.getStatus() == Constant.CODE_200){
                handle200(s, response, request);
                return;
            }
            if(Constant.CODE_302 == response.getStatus()){
                handle302(s, response);
                return;
            }
        }catch(Exception e){
            e.printStackTrace();
            handle500(s, e);
        }finally{
            try{
                if(!s.isClosed())
                    s.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    //向浏览器响应200信息
    private static void handle200(Socket s, Response response, Request request) throws IOException {
        OutputStream os = s.getOutputStream();

        //获取响应头部
        String contentType = response.getContentType();
        //获取内容字节数组
        byte[] body = response.getBody();
        String cookieheader = response.getCookiesHeader();

        //是否需要解压
        boolean gzip = isGzip(request, body, contentType);
        String headText;
        if(gzip)
            headText = Constant.response_head_200_gzip;
        else
            headText = Constant.response_head_200;

        headText = StrUtil.format(headText, contentType, cookieheader);

        if(gzip)
            body = ZipUtil.gzip(body);

        //创建响应头部字节数组
        byte[] head = headText.getBytes();
        //创建一个用于输出给浏览器的字节数组
        byte[] responseBytes = new byte[head.length + body.length];



        //将两个字节数组中的内容拷贝到这个字节数组中
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        //发给浏览器
        os.write(responseBytes,0, responseBytes.length);
        os.flush();
        os.close();
    }

    //向浏览器响应404信息
    private static void handle404(Socket s, String uri) throws IOException {
        OutputStream os = s.getOutputStream();

        //获取响应头部
        String headTextString = StrUtil.format(Constant.textFormat_404, uri, uri);
        String headText = Constant.response_head_404;
        headText = headText + headTextString;

        //发给浏览器
        os.write(headText.getBytes());
    }

    //向浏览器响应500信息
    private static void handle500(Socket s, Exception e){
        try {
            OutputStream os = s.getOutputStream();

            //获取异常的栈内容
            StackTraceElement stacks[] = e.getStackTrace();
            //字符串拼接
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement stack : stacks) {
                sb.append("\t");
                sb.append(stack.toString());
                sb.append("\r\n");
            }

            //获取异常的备注信息
            String message = e.getMessage();

            //获取响应头部
            String headTextString = StrUtil.format(Constant.textFormat_500, message, e.toString(), sb.toString());
            String headText = Constant.response_head_500;
            headText = headText + headTextString;
            System.out.println("haha: " + sb.toString());
            System.out.println("hana: " + headText);

            //发给浏览器
            os.write(headText.getBytes());
        }catch(IOException e1){
            e1.printStackTrace();
        }
    }

    //向浏览器响应302
    private static void handle302(Socket s, Response response) throws IOException {
        OutputStream os = s.getOutputStream();
        //客户访问的路径
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        System.out.println("幕后发：" + header);
        byte[] responseBytes = header.getBytes("utf-8");
        os.write(responseBytes);
    }

    //创建session
    public static void prepareSession(Request request, Response response) {
        //从cookie中获取id
        String jsessionid = request.getJsessionidFromCookie();
        //再根据id创建session
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        //设置在request上
        request.setSession(session);
        System.out.println("梅俊涛H哈：" + SessionManager.getSessionMap());
    }

    //判断是否需要解压
    private static boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings = request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;

        Connector connector = request.getConnector();

        if (mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType, ";", false);

        if (!"on".equals(connector.getCompression()))
            return false;

        if (body.length < connector.getCompressionMinSize())
            return false;

        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }

        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if (mimeType.equals(eachMimeType))
                return true;
        }

        return false;
    }
}
