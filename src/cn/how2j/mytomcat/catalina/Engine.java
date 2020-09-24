package cn.how2j.mytomcat.catalina;

import cn.how2j.mytomcat.util.ServerXMLUtil;

import java.util.List;

public class Engine {

    private String defaultHost;
    private Service service; //父级元素对象

    public Engine(Service service){
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
    }

    //获取Host对象
    public Host getHost() throws Exception {
        List<Host> hosts = ServerXMLUtil.getHost(this);

        Host host = null;
        //根据属性获取相应的Host对象
        for(Host h : hosts){
            if(defaultHost.equals(h.getName()))
                host = h;
            else
                throw new Exception("NullException");
        }
        return host;
    }


    public String getDefaultHost() {
        return defaultHost;
    }

    public Service getService() {
        return service;
    }
}
