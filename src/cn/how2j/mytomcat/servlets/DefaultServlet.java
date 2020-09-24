package cn.how2j.mytomcat.servlets;

import cn.how2j.mytomcat.catalina.Context;
import cn.how2j.mytomcat.http.Request;
import cn.how2j.mytomcat.http.Response;
import cn.how2j.mytomcat.util.Constant;
import cn.how2j.mytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class DefaultServlet extends HttpServlet {

    private static DefaultServlet defaultServlet = new DefaultServlet();

    public static DefaultServlet getDefaultServlet(){
        return defaultServlet;
    }

    private DefaultServlet(){

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        Context context = request.getContext();
        String uri = request.getUri();

        //响应内容
        if ("/".equals(uri))
            uri = WebXMLUtil.getWelcomeFile(context);

        //如果是jsp就交给JspServlet
        if(uri.endsWith(".jsp")){
            JspServlet.getInstance().service(request, response);
            return;
        }

        if ("/time.html".equals(uri)) {
            ThreadUtil.sleep(1000);
        }
        if ("/500.html".equals(uri))
            response.setStatus(Constant.CODE_500);

        //去掉“/”
        String uriFile = StrUtil.removePrefix(uri, "/");
        System.out.println("uriFile: " + uriFile);
        File file = FileUtil.file(request.getRealPath(uriFile));
        System.out.println("filedocBase: " + file.getAbsolutePath());
        //文件存在就获取内容
        if (file.exists()) {
            //获取文件的后缀名
            String exName = FileUtil.extName(file);
            response.setContentType(WebXMLUtil.getMimeType(exName));
            byte[] fileString = FileUtil.readBytes(file);
            response.setBody(fileString);

            response.setStatus(Constant.CODE_200);
        } else {
            //向浏览器响应404信息
            response.setStatus(Constant.CODE_404);
        }
    }
}
