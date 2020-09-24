package cn.how2j.mytomcat.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CommonClassLoader extends URLClassLoader {
    //公共类加载器，用于加载lib目录下的jar文件

    public CommonClassLoader(){
        super(new URL[]{});

        try{
            //根据当前的工作目录获取lib目录
            File libFolder = new File(System.getProperty("user.dir"), "lib");
            //获取lib目录下的所有文件
            File[] jarFiles = libFolder.listFiles();
            for(File file : jarFiles){
                //如果是jar文件就添加到这个类加载器中
                if(file.getName().endsWith("jar")){
                    URL urls = new URL("file:" + file.getAbsolutePath());
                    this.addURL(urls);
                }
            }
        }catch(MalformedURLException e){
            e.printStackTrace();
        }
    }
}
