package cn.how2j.mytomcat.util;

import cn.how2j.mytomcat.catalina.*;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ServerXMLUtil {

    //获取Context对象
    public static List<Context> getContext(Host host){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFload);

        Document d = Jsoup.parse(xml);

        String hostName = "[name='{}']";
        Element element = d.select("Host").select(StrUtil.format(hostName, host.getName())).first();
        Elements es = element.select("Context");
        List<Context> result = new ArrayList<>();
        for(Element e : es){
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            boolean reloadable = Convert.toBool(e.attr("reloadable"));
            Context context = new Context(path, docBase, host, reloadable);
            result.add(context);
        }
        return result;
    }

    //获取Host属性naem的值
    public static List<Host> getHost(Engine engine){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFload);

        Document d = Jsoup.parse(xml);
        Elements es = d.select("Engine").select("Host");
        List<Host> result = new ArrayList<>();
        for(Element e : es){
            String name = e.attr("name");
            Host host = new Host(name, engine);
            result.add(host);
        }
        return result;
    }

    //获取Enging属性值
    public static String getEngineDefaultHost(){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFload);

        Document d = Jsoup.parse(xml);
        Element e = d.select("Engine").first();

        return e.attr("defaultHost");
    }

    //获取service的name属性值
    public static String getServiceName(){
        String xml = FileUtil.readUtf8String(Constant.serverXMLFload);

        Document d = Jsoup.parse(xml);
        Element e = d.select("Service").first();

        return e.attr("name");
    }

    //获取端口号
    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXMLFload);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Connector");
        for (Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            Connector c = new Connector(service, port);
            result.add(c);
        }
        return result;
    }

    //解析这四个属性
    public static List<Connector> getConnector(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXMLFload);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");
        for (Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");
            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = e.attr("noCompressionUserAgents");
            String compressableMimeType = e.attr("compressableMimeType");

            Connector c = new Connector(service, port);
            c.setCompression(compression);
            c.setCompressableMimeType(compressableMimeType);
            c.setNoCompressionUserAgents(noCompressionUserAgents);
            c.setCompressableMimeType(compressableMimeType);
            c.setCompressionMinSize(compressionMinSize);
            result.add(c);
        }
        return result;
    }
}
