package cn.how2j.mytomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Response extends BaseResponse {

    private StringWriter stringWriter; //用于存放返回的内容的输出流
    private PrintWriter writer; //用来接收数据的
    private String contentType; //处理的类型
    private byte[] body; //用于获取字节型的内容

    private int status; //返回响应的常量

    private List<Cookie> cookies; //用来存放Cookie的

    private String redirectPath; //用来保存客户访问的路径

    public Response() {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.cookies = new ArrayList<>();
        this.contentType = "text/html";
    }

    public byte[] getBody() throws UnsupportedEncodingException {
        String conte = stringWriter.toString();
        if (body == null) {
            body = conte.getBytes("utf-8");
        }
        return body;
    }

    //把Cookie传过来的信息转换为字符串，响应给浏览器
    public String getCookiesHeader() {
        if (null == cookies)
            return "";
        //时间格式
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        //创建时间格式
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);

        StringBuffer sb = new StringBuffer();
        sb.append("\r\n");
        sb.append("Set-Cookie: ");
        for (Cookie cookie : getCookies()) {
            sb.append(cookie.getName() + "=" + cookie.getValue() + "; ");

            //如果设置的时间为无就跳过
            if (-1 != cookie.getMaxAge()) {
                //设置存在的时间
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append("; ");
            }
            if (null != cookie.getPath()) {
                //设置路径
                sb.append("Path=" + cookie.getPath());
            }
        }

        return sb.toString();
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }

    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }
}

