package cn.how2j.mytomcat.servlets;

import cn.how2j.mytomcat.catalina.Context;
import cn.how2j.mytomcat.classloader.JspClassLoader;
import cn.how2j.mytomcat.http.Request;
import cn.how2j.mytomcat.http.Response;
import cn.how2j.mytomcat.util.Constant;
import cn.how2j.mytomcat.util.JspUtil;
import cn.how2j.mytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class JspServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static JspServlet instance = new JspServlet();

    public static synchronized JspServlet getInstance() {
        return instance;
    }

    private JspServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            String uri = request.getRequestURI();

            if ("/".equals(uri))
                uri = WebXMLUtil.getWelcomeFile(request.getContext());

            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName));
            System.out.println("梅俊涛4：" + file);

            File jspFile = file;
            if (jspFile.exists()) {
                Context context = request.getContext();
                String path = context.getPath();
                System.out.println("梅俊涛：" + path);
                String subFolder;
                if("/".equals(path))
                    subFolder = "_";
                else
                    subFolder = StrUtil.subAfter(path, '/', false);

                System.out.println("梅俊涛1：" + subFolder);

                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);

                System.out.println("梅俊涛2：" + servletClassPath);

                File jspServletClassFile = new File(servletClassPath);

                System.out.println("梅俊涛3：" + jspServletClassFile);

                //然后通过 JspUtil 获取 servlet 路径，看看是否存在
                if(!jspServletClassFile.exists()){
                    //转义为class文件
                    JspUtil.compileJsp(context, jspFile);
                }else if(jspFile.lastModified() > jspServletClassFile.lastModified()){
                    //如果存在，再看看最后修改时间与 jsp 文件的最后修改时间谁早谁晚
                    JspUtil.compileJsp(context, jspFile);
                    //把之前的jsp与jsp类加载器脱钩
                    JspClassLoader.invalidJspClassLoader(uri, context);
                }

                //设置文件格式
                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);

                //获取这个类加载器
                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                //获取jsp类名
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                //把这个servlet交给这个类加载器
                Class jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request,response);

                //判断getRedirectPath 是否有值，如果有就返回 302 状态码
                if(null!= response.getRedirectPath())
                    response.setStatus(Constant.CODE_302);
                else
                    response.setStatus(Constant.CODE_200);

            } else {
                response.setStatus(Constant.CODE_404);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}