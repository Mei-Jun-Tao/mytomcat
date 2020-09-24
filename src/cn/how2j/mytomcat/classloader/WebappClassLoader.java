package cn.how2j.mytomcat.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[]{}, commonClassLoader);

        try{
            File webinffolder = new File(docBase, "WEB-INF");
            File classesFolder = new File(webinffolder, "classes");
            File libFolder = new File(webinffolder, "lib");

            System.out.println("文件：" + webinffolder.getAbsolutePath());
            System.out.println("文件1：" + classesFolder.getAbsolutePath());
            System.out.println("文件2：" + libFolder.getAbsolutePath());
            URL url;
            url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);

            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for(File file : jarFiles){
                url = new URL("file:" + file.getAbsolutePath());
                this.addURL(url);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //关闭类加载器
    public void stop() {
        try {
            close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
