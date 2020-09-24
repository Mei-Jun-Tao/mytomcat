package cn.how2j.mytomcat.catalina;

import cn.how2j.mytomcat.util.ServerXMLUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;

import java.util.List;

public class Service {

    private String name;
    private Server server; //父级元素对象
    private Engine engine; //子级元素对象

    private List<Connector> connectors;

    public Service(Server server){
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    public void start() {
        init();
    }

    //启动线程
    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector c : connectors)
            c.init();
        LogFactory.get().info("Initialization processed in {} ms",timeInterval.intervalMs());
        for (Connector c : connectors)
            c.start();
    }

    public String getName() {
        return name;
    }

    public Server getServer() {
        return server;
    }

    public Engine getEngine() {
        return engine;
    }
}
