package cn.how2j.mytomcat.catalina;

import cn.how2j.mytomcat.ThreadUtil.ThreadPoolUtil;
import cn.how2j.mytomcat.http.Request;
import cn.how2j.mytomcat.http.Response;
import cn.hutool.log.LogFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable {
    //实现任务接口,把处理请求交给了HttpProcessor类处理

    private int port; //端口号
    private Service service;

    private String compression; //表示是否启动，当等于 "on" 的时候，表示启动
    private int compressionMinSize; //表示最小进行压缩的字节数，太小就没有必要压缩了，一般是 1024. 但是这里为了看到效果，故意设置成20，否则就看不到现象了
    private String noCompressionUserAgents; //这表示不进行压缩的浏览器
    private String compressableMimeType; //这表示哪些 mimeType 才需要进行压缩

    public Connector(Service service, int port){
        this.service = service;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(port);

            //用于连续接收请求
            while(true){
                Socket s = ss.accept();

                //创建一个任务
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //利用Request对象来接受浏览器发出的请求信息
                            Request request = new Request(s, Connector.this);
                            Response response = new Response();
                            HttpProcessor.execute(s, request, response);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                };

                //交给线程池处理
                ThreadPoolUtil.execute(r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //打印日志
    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }
    //启动线程，并把当前任务添加进去
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }

    public Service getService() {
        return service;
    }
}
