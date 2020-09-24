package cn.how2j.mytomcat.http;

import cn.how2j.mytomcat.catalina.Context;

import java.io.File;
import java.util.*;

public class ApplicationContext extends BaseServletContext {
    //ServletContext是关于servlet的一系列的处理工作的

    private Map<String, Object> attributesMap; //属性值
    private Context context;

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

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

    //获取文件的路径
    public String getRealPath(String path) {
        return new File(context.getDocBase(), path).getAbsolutePath();
    }
}
