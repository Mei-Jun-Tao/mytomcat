package cn.how2j.mytomcat.watchar;

import cn.how2j.mytomcat.catalina.Context;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class ContextFileChangeWatcher {
    //监听器

    private WatchMonitor monitor; //真正起作用的监听
    private boolean stop = false; //判断是否处理过了

    public ContextFileChangeWatcher(Context context){
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher(){
            private void dealWith(WatchEvent<?> event){
                synchronized(ContextFileChangeWatcher.class){
                    //获取当前文件或目录的名称
                    String fileName = event.context().toString();

                    //判断是否重载过了
                    if(stop)
                        return;
                    //判断文件的格式
                    if(fileName.endsWith(".jar") || fileName.endsWith(".xml") || fileName.endsWith(".class")){
                        stop = true;

                        LogFactory.get().info("检测到文件已经发生了变化{}", fileName);
                        //进行重载
                        context.reload();
                    }

                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }
        });
    }

    //启动监听
    public void start(){
        monitor.start();
    }

    //停止监听
    public void stop(){
        monitor.close();
    }
}
