package cn.how2j.mytomcat.catalina;

import cn.how2j.mytomcat.util.Constant;
import cn.how2j.mytomcat.util.ServerXMLUtil;
import cn.hutool.core.io.FileUtil;

import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {

    private String name; //name属性值
    private Engine engine; //父级元素对象

    //用来存放Context对象的
    private Map<String, Context> contextMap = new HashMap<>();

    public Host(String name, Engine engine){
        this.name = name;
        this.engine = engine;

        //扫描webapps文件
        scanContextsOnWebAppsFolder();
        //配置文件
        scanContextsInServerXML();
    }

    //用于扫描webapps目录下的目录
    private void scanContextsOnWebAppsFolder(){
        File file = FileUtil.file(Constant.webappsFload);
        //以数组的形式获取这个目录下的所有文件
        File[] files = file.listFiles();
        for(File f : files){
            //如果是目录就进行处理否则继续遍历
            if(f.isDirectory())
                loadContext(f);
        }
    }
    private void loadContext(File file){
        String path;
        if(file.getName().equals("ROOT"))
            path = "/";
        else
            path = "/" + file.getName();

        //获取当前文件的位置
        String docBase = file.getAbsolutePath();
        Context context = new Context(path, docBase, this, true);

        contextMap.put(context.getPath(), context);
    }
    private void scanContextsInServerXML(){
        List<Context> contexts = ServerXMLUtil.getContext(this);
        for(Context c : contexts){
            contextMap.put(c.getPath(), c);
        }
    }

    //重加载
    public void reload(Context context){
        //先保存context的数据
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();

        //停止加载器和监听器
        context.stop();

        //把原来的删除
        contextMap.remove(path);

        //在重新添加
        Context c = new Context(path, docBase, this, reloadable);

        contextMap.put(path, c);
    }

    //根据路径获取Context对象
    public Context getContext(String path){
        return contextMap.get(path);
    }

    public String getName() {
        return name;
    }

    public Engine getEngine() {
        return engine;
    }
}
