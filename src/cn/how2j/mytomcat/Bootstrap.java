package cn.how2j.mytomcat;

import cn.how2j.mytomcat.classloader.CommonClassLoader;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Method;

public class Bootstrap {

    //批处理文件就是把当前的类和自定义的类加载器类保留下来在OUT目录下，其它得类做成jar放到lib目录下，这样就只有这两个类是应用加载器加载，其它的类都是自定义的类加载

    public static void main(String[] args) throws Exception {

        //创建类加载器,把后续所有的类都交给这个来加载
        CommonClassLoader commonClassLoader = new CommonClassLoader();

        //后面的线程都归于这个类加载器处理
        Thread.currentThread().setContextClassLoader(commonClassLoader);

        //把server也交给这个类加载器
        String className = "cn.how2j.mytomcat.catalina.Server";
        Class serverClazz = commonClassLoader.loadClass(className);

        Object serverObject = serverClazz.newInstance();

        Method m = serverClazz.getMethod("start");

        m.invoke(serverObject);
    }
}
