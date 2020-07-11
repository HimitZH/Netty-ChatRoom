package top.hcode.client;


/**
 * @Author: Himit_ZH
 * @Date: 2020/7/10 00:13
 * @Description: 客户端上下文管理器，采用策略模式。
 */
public class ClientFrameContext {

    private ClientFrame clientFrame;

    public ClientFrameContext(ClientFrame clientFrame) {
        this.clientFrame = clientFrame;
    }

    //上下文接口
    public void init() {
        clientFrame.frameInit();
    }

}