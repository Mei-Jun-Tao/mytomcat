package cn.how2j.mytomcat.ThreadUtil;

import cn.hutool.db.sql.StatementWrapper;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    //线程池类

    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20, 100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public static void execute(Runnable r){
        threadPool.execute(r);
    }
}
