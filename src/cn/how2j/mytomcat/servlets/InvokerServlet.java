package cn.how2j.mytomcat.servlets;

import cn.how2j.mytomcat.catalina.Context;
import cn.how2j.mytomcat.classloader.WebappClassLoader;
import cn.how2j.mytomcat.http.Request;
import cn.how2j.mytomcat.http.Response;
import cn.how2j.mytomcat.util.Constant;
import cn.hutool.core.util.ReflectUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvokerServlet extends HttpServlet {

    private static InvokerServlet instance = new InvokerServlet();

    public static synchronized InvokerServlet getInstance() {
        return instance;
    }

    private InvokerServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        //转换为自己创建的
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        //根据uri获取类全名
        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        //用自定义的web类加载器来加载这个servlet
        try {
            Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            //根据类对象实例化这个对象，从context中获取这个实例化servlet
            Object servletObject = context.getServlet(servletClass);
            //调用doGet方法
            ReflectUtil.invoke(servletObject, "service", request, response);

            //判断getRedirectPath 是否有值，如果有就返回 302 状态码
            if(null!= response.getRedirectPath())
                response.setStatus(Constant.CODE_302);
            else
                response.setStatus(Constant.CODE_200);
            System.out.println("servletClass:" + servletClass);
            System.out.println("servletClass'classLoader:" + servletClass.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
