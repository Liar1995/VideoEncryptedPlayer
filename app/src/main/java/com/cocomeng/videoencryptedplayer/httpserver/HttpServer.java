package com.cocomeng.videoencryptedplayer.httpserver;

/**
 * Created by Sunmeng on 12/30/2016.
 * E-Mail:Sunmeng1995@outlook.com
 * Description: 模拟服务端
 */

public class HttpServer {

    private final static String TAG = "HttpServer";
    private final static int SERVER_PORT = 0;
    private static HttpServer mHttpServer;
    private int mPort;
    private HttpServiceThread mServiceThread;
    private ThreadGroup mThreadGroup;
    public static HttpServer getInstance() {
        if (null == mHttpServer) {
            mHttpServer = new HttpServer();
        }
        return mHttpServer;
    }

    private HttpServer() {
        mThreadGroup = new ThreadGroup(HttpServer.class.getName());
    }

    public boolean start(IHttpStream stream, int port) {
        if (0 == port)
            port = SERVER_PORT;
        mPort = port;
        try {
            if (null != mServiceThread) {
                if (mServiceThread.isBound()) {
                    mPort = mServiceThread.getPort();
                    mServiceThread.setStream(stream);
                    return true;
                }
                mServiceThread.close();
            }
            mServiceThread = new HttpServiceThread(stream, mThreadGroup, port);
            mPort = mServiceThread.getPort();
            mServiceThread.start();
            return true;
        } catch (Exception e) {
            mServiceThread = null;
            e.printStackTrace();
        }
        return false;
    }

    public void stop() {
        if (null != mServiceThread) {
            mServiceThread.close();
            mServiceThread = null;
        }
    }


    public String getHttpAddr() {
        return "http://127.0.0.1:" + mPort;
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
    }
}
