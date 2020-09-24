package cn.how2j.mytomcat.util;

import cn.how2j.mytomcat.catalina.Context;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class WebXMLUtil {

    private static Map<String, String> mimeTypeMapping = new HashMap<>();

    //获取web.xml中的默认访问文件格式
    public static String getWelcomeFile(Context context){
        String xml = FileUtil.readUtf8String(Constant.webXMLFload);

        Document d = Jsoup.parse(xml);
        Elements es = d.select("welcome-file-list").select("welcome-file");

        for(Element e : es){
            String welcomeFileName = e.text();
            //根据目录和文件名去创建文件对象
            File f = new File(context.getDocBase(), welcomeFileName);
            //如果有这类文件那就返回这个文件名
            if(f.exists())
                return f.getName();
        }

        return "index.html";
    }

    //处理文件格式,为了防止两个线程同时访问
    public static synchronized String getMimeType(String extName) {
        if (mimeTypeMapping.isEmpty())
            initMimeType();

        String mimeType = mimeTypeMapping.get(extName);
        if (null == mimeType)
            return "text/html";

        return mimeType;
    }

    private static void initMimeType() {
        String xml = FileUtil.readUtf8String(Constant.webXMLFload);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("mime-mapping");
        for (Element e : es) {
            String extName = e.select("extension").first().text();
            String mimeType = e.select("mime-type").first().text();
            mimeTypeMapping.put(extName, mimeType);
        }
    }
}
