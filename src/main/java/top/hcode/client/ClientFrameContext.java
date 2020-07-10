package top.hcode.client;


/**
 * @Author: Himit_ZH
 * @Date: 2020/7/10 00:13
 * @Description:
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