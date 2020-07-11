package top.hcode.client;

import io.netty.util.internal.StringUtil;
import top.hcode.commons.MsgModel;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/10 00:15
 * @Description: Swing窗口父类，管理管理端和用户端的参数和操作方法
 */
public abstract class ClientFrame {

    protected JFrame frame;
    //头部参数
    protected JTextField host_textfield;
    protected JTextField port_textfield;
    protected JTextField name_textfield;
    protected JButton head_connect;
    protected JButton head_exit;
    //底部参数
    protected JTextField text_field;
    protected JTextField sysText_field; //管理员特有
    protected JButton foot_send;
    protected JButton foot_sysClear;
    protected JButton foot_userClear;
    protected JButton foot_sysSend;   //管理员特有

    //右边参数
    protected JLabel users_label;
    protected JButton kick_button;  //管理员特有
    protected JButton privateChat_button;
    protected JButton shield_button;
    protected JButton unshield_button;
    protected volatile JList<String> userlist;
    protected volatile DefaultListModel<String> users_model;
    protected volatile ConcurrentMap<String, String> users_map;

    //左边参数
    protected JScrollPane sysTextScrollPane;
    protected JTextPane sysMsgArea;
    protected JScrollBar sysVertical;


    //中间参数
    protected JScrollPane userTextScrollPane;
    protected JTextPane userMsgArea;
    protected JScrollBar userVertical;

    //当前用户的id
    protected volatile String userId;

    //当前用户列表昵称
    protected volatile String userName;


    // 记录可能重复的昵称 进行计数
    protected ConcurrentMap<String, Integer> repeatNameCount;

    protected ChatRoomClient chatRoomClient; //当前连接的客户端

    //私聊窗口Map
    protected ConcurrentMap<String, ClientFrame.privateChatFrame> privateChatFrameMap;

    //抽象类 其子类具体实现窗口初始化
    ClientFrame clientFrame;

    //时间格式化工具类
    private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static Date parse(String dateStr) throws ParseException {
        return df.get().parse(dateStr);
    }

    public static String format(Date date) {
        return df.get().format(date);
    }



    public ClientFrame(ChatRoomClient chatRoomClient) {
        this.chatRoomClient = chatRoomClient;
    }


    // 抽象方法 实现界面初始化 等待子类重写
    public abstract void frameInit();



    /**
     * @MethodName ipCheck
     * @Params * @param null
     * @Description 验证ip格式是否正确
     * @Return
     * @Since 2020/6/8
     */

    public static boolean ipCheckHost(String text) {
        if (text != null && !text.isEmpty()) {
            // 定义正则表达式
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            // 判断ip地址是否与正则表达式匹配
            if (text.matches(regex)) {
                // 返回判断信息
                return true;
            } else {
                // 返回判断信息
                return false;
            }
        }
        return false;
    }

    /**
     * @MethodName ipCheckPort
     * @Params * @param null
     * @Description 验证端口格式是否准确
     * @Return
     * @Since 2020/6/8
     */
    public static boolean ipCheckPort(String text) {
        return text.matches("([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-5]{2}[0-3][0-5])");
    }

    /**
     * @MethodName updateTextArea
     * @Params * @param null
     * @Description 更新系统文本域或聊天事件文本域
     * @Return
     * @Since 2020/6/6
     */
    public void updateTextArea(String content, String where) {
        if (content.length() > 0) {
            Matcher matcher = null;
            if (where.equals("user")) { //更新群聊消息到世界聊天窗口
                Pattern pattern = Pattern.compile("<id>(.*)</id><name>(.*)</name><msg>(.*)</msg><time>(.*)</time>");
                matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String currentId = matcher.group(1);
                    String userName = matcher.group(2);
                    String userMsg = matcher.group(3);
                    String time = matcher.group(4);
                    if (!currentId.equals(userId)) {
                        userName = getUserName(currentId);
                    } else {
                        //如果是自己说的话
                        userName = "你";
                    }
                    insertMessage(userTextScrollPane, userMsgArea, null, userName + " " + time, " " + userMsg, userVertical, false);
                }
            } else if (where.equals("sys")) { //更新系统消息
                Pattern sysPattern = Pattern.compile("<msg>(.*)</msg><time>(.*)</time>");
                matcher = sysPattern.matcher(content);
                if (matcher.find()) { //处理系统消息
                    String sysMsg = matcher.group(1);
                    String sysTime = matcher.group(2);
                    insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + sysTime, sysMsg, sysVertical, true);
                }
            } else if (where.equals("admin")) {  //更新管理员发布的系统消息
                Pattern adminPattern = Pattern.compile("<admin>(.*)</admin><msg>(.*)</msg><time>(.*)</time>");
                matcher = adminPattern.matcher(content);
                if (matcher.find()) {
                    String adminId = matcher.group(1);
                    String adminMsg = matcher.group(2);
                    String adminTime = matcher.group(3);
                    String adminName = getUserName(adminId);
                    insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统通知] " + adminTime, adminName + " 通知：" + adminMsg, sysVertical, true);
                }
            }
        }
    }

    /**
     * @MethodName insertMessage
     * @Params * @param null
     * @Description 更新文本域信息格式化工具
     * @Return
     * @Since 2020/6/6
     */
    protected void insertMessage(JScrollPane scrollPane, JTextPane textPane, String icon_code,
                                 String title, String content, JScrollBar vertical, boolean isSys) {
        StyledDocument document = textPane.getStyledDocument();     /*获取textpane中的文本*/
        /*设置标题的属性*/
        Color content_color = null;
        if (isSys) {
            content_color = Color.RED;
        } else {
            content_color = Color.GRAY;
        }
        SimpleAttributeSet title_attr = new SimpleAttributeSet();
        StyleConstants.setBold(title_attr, true);
        StyleConstants.setForeground(title_attr, Color.BLUE);
        /*设置正文的属性*/
        SimpleAttributeSet content_attr = new SimpleAttributeSet();
        StyleConstants.setBold(content_attr, false);
        StyleConstants.setForeground(content_attr, content_color);
        Style style = null;
        if (icon_code != null) {
            Icon icon = new ImageIcon("icon/" + icon_code + ".png");
            style = document.addStyle("icon", null);
            StyleConstants.setIcon(style, icon);
        }

        try {
            document.insertString(document.getLength(), title + "\n", title_attr);
            if (style != null)
                document.insertString(document.getLength(), "\n", style);
            else
                document.insertString(document.getLength(), content + "\n", content_attr);

        } catch (BadLocationException ex) {
            System.out.println("Bad location exception");
        }
        /*设置滑动条到最后*/
        textPane.setCaretPosition(textPane.getDocument().getLength());
//        vertical.setValue(vertical.getMaximum());
    }

    /**
     * @MethodName getUserName
     * @Params * @param null
     * @Description 在users_map中根据value值用户ID获取key值的用户名字
     * @Return
     * @Since 2020/6/6
     */
    protected String getUserName(String strId) {
        Set<String> set = users_map.keySet();
        Iterator<String> iterator = set.iterator();
        String cur = null;
        while (iterator.hasNext()) {
            cur = iterator.next();
            if (users_map.get(cur).equals(strId)) {
                return cur;
            }
        }
        return "未知";
    }

    /**
     * @MethodName beKick
     * @Params * @param null
     * @Description 处理某人被踢的相关操作方法，可能是自己，也可能是别人
     * @Return
     * @Since 2020/7/11
     */

    protected void beKick(String content) {
        if (content.length() > 0) {
            Pattern pattern = Pattern.compile("<admin>(.*)</admin><kick>(.*)</kick><time>(.*)</time>");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String adminId = matcher.group(1);
                String adminName = getUserName(adminId);
                String beKickId = matcher.group(2);
                String time = matcher.group(3);
                if (beKickId.equals(userId)) { //如果被踢的是自己
                    ChatRoomClient.isReConnect = false;
                    showEscDialog("对不起，您被" + adminName + " 踢出了聊天室！", time);
                } else { //被踢的是别人
                    String beKickName = getUserName(beKickId);
                    String sysMsg = "通知：" + beKickName + " 被 " + getUserName(adminId) + " 踢出了聊天室！";
                    insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + time, sysMsg, sysVertical, true);
                    users_map.remove(beKickId);
                    users_model.removeElement(beKickName);
                    repeatNameCount.clear();
                }
            }
        }
    }

    /**
     * @MethodName disConnect
     * @Params * @param null
     * @Description 手动断开与服务器的操作方法
     * @Return
     * @Since 2020/7/11
     */

    protected void disConnect() {
        userMsgArea.setText("");
        users_map.clear();
        repeatNameCount.clear();
        users_model.removeAllElements();
        users_label.setText("聊天室内人数：0");
        //清除所有私聊
        if (privateChatFrameMap.size() != 0) {
            Set<Map.Entry<String, ClientFrame.privateChatFrame>> entrySet = privateChatFrameMap.entrySet();
            for (Map.Entry<String, ClientFrame.privateChatFrame> entry : entrySet) {
                entry.getValue().dispose(); //关闭对应窗口
                String text = "<id>" + chatRoomClient.getSocketChannel().id().asLongText() + "</id><order>2</order><msg>exit</msg>";
                MsgModel msgModel = new MsgModel((byte) 2, (byte) 2, text);
                chatRoomClient.getSocketChannel().writeAndFlush(msgModel);
            }
        }
    }


    /**
     * @MethodName showEscDialog
     * @Params * @param null
     * @Description 处理当前客户端用户断开与服务器连接的一切事务
     * @Return
     * @Since 2020/6/6
     */
    public void showEscDialog(String content, String time) {
        /*清除消息区内容，清除用户数据模型内容和用户map内容，更新房间内人数*/

        chatRoomClient.disconnect();//关闭通道

        userMsgArea.setText("");
        users_map.clear();
        users_model.removeAllElements();
        repeatNameCount.clear();
        users_label.setText("聊天室内人数：0");
        //清除所有私聊
        if (privateChatFrameMap.size() != 0) {
            Set<Map.Entry<String, ClientFrame.privateChatFrame>> entrySet = privateChatFrameMap.entrySet();
            for (Map.Entry<String, ClientFrame.privateChatFrame> entry : entrySet) {
                entry.getValue().dispose(); //关闭对应窗口
                String text = "<id>" + chatRoomClient.getSocketChannel().id().asLongText() + "</id><order>2</order><msg>exit</msg>";
                MsgModel msgModel = new MsgModel((byte) 2, (byte) 2, text);
                chatRoomClient.getSocketChannel().writeAndFlush(msgModel);
            }
        }
        //输入框可编辑
        port_textfield.setEditable(true);
        name_textfield.setEditable(true);
        host_textfield.setEditable(true);
        head_connect.setText("连接");
        head_exit.setText("已退出");
        if (StringUtil.isNullOrEmpty(time)) {
            time = format(new Date());
        }
        insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + time, content, sysVertical, true);
        JOptionPane.showMessageDialog(frame, content, "提示", JOptionPane.WARNING_MESSAGE);

    }


    // 处理重名问题，如果有多个昵称相同在名字后面添加(num)
    public synchronized String addAutoId(String name) {

        int num = repeatNameCount.get(name) + 1;
        for (int i = 0; i < num; i++) {
            if (i==0 && !users_map.containsKey(name)){
                return null;
            }
            else if(!users_map.containsKey(name + "(" + i + ")")&&i!=0){
                return String.valueOf(i);
            }
        }
        repeatNameCount.put(name, num);
        return String.valueOf(num);
    }

    

    /**
     * @MethodName addUser
     * @Params * @param null
     * @Description 当有新的用户加入聊天室，系统文本域的更新和用户列表的更新
     * @Return
     * @Since 2020/6/6
     */
    public void addUser(String content) {
        if (content.length() > 0) {
            Pattern pattern = Pattern.compile("<id>(.*)</id><name>(.*)</name><authority>(.*)</authority><time>(.*)</time>");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String id = matcher.group(1);
                String name = matcher.group(2);
                String authority = matcher.group(3);
                String time = matcher.group(4);
                if (!users_map.containsValue(id)) {
                    if (!id.equals(userId)) {
                        if (authority.equals("admin")) {
                            String temp = "[管理员] " + name;
                            if (repeatNameCount.containsKey(temp)) {
                                String num = addAutoId(temp);
                                if (num!=null){
                                    temp = temp + "(" + num + ")";
                                }
                            } else {
                                repeatNameCount.put(temp, 0);
                            }
                            users_map.put(temp, id);
                            users_model.insertElementAt(temp, 1);

                        } else {
                            String temp = "[用户] " + name;
                            if (repeatNameCount.containsKey(temp)) {
                                String num = addAutoId(temp);
                                if (num!=null){
                                    temp = temp + "(" + num + ")";
                                }
                            } else {
                                repeatNameCount.put(temp, 0);
                            }
                            users_map.put(temp, id);
                            users_model.addElement(temp);
                        }
                    } else {
                        name = userName;
                    }
                }
                insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + time, name + " 加入了聊天室", sysVertical, true);
            }
        }
        users_label.setText("聊天室内人数：" + users_map.size()); //更新房间内的人数
    }


    /**
     * @MethodName delUser
     * @Params content(为退出用户的ID)
     * @Description 当有用户退出时，系统文本域的通知和用户列表的更新
     * @Return
     * @Since 2020/6/6
     */
    public void delUser(String content) {
        if (content.length() > 0) {
            Pattern pattern = Pattern.compile("<id>(.*)</id><name>(.*)</name><authority>(.*)</authority><time>(.*)</time>");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String id = matcher.group(1);
                String name = matcher.group(2);
                String authority = matcher.group(3);
                String time = matcher.group(4);
                String delName = getUserName(id);
                insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + time, delName + " 退出了聊天室", sysVertical, true);
                users_map.remove(delName);
                users_model.removeElement(delName);
            }
        }
        users_label.setText("聊天室内人数：" + users_map.size());//更新房间内的人数
    }


    /**
     * @MethodName getUserList
     * @Params * @param null
     * @Description 从服务器获取全部用户信息的列表，解析信息格式，列出所有用户
     * @Return
     * @Since 2020/6/6
     */
    public void getUserList(String content) {
        String name = null;
        String id = null;
        String auth = null;
        Pattern numPattern = null;
        Matcher numMatcher = null;
        Pattern userListPattern = null;
        if (content.length() > 0) {
            numPattern = Pattern.compile("<user>(.*?)</user>");
            numMatcher = numPattern.matcher(content);
            //遍历字符串，进行正则匹配，获取所有用户信息
            boolean me = true;
            int num = 0;
            while (numMatcher.find()) {
                String detail = numMatcher.group(1);
                userListPattern = Pattern.compile("<id>(.*)</id><name>(.*)</name><authority>(.*)</authority>");
                Matcher userListMatcher = userListPattern.matcher(detail);
                if (userListMatcher.find()) {
                    id = userListMatcher.group(1);
                    name = userListMatcher.group(2);
                    auth = userListMatcher.group(3);
                    if (auth.equals("admin")) {
                        String temp = "[管理员] " + name;
                        if (repeatNameCount.containsKey(temp)) {
                            String nameNum = addAutoId(temp);
                            if (nameNum!=null){
                                temp = temp + "(" + num + ")";
                            }
                        } else {
                            repeatNameCount.put(temp, 0);
                        }
                        users_map.put(temp, id);
                        if (me){ //如果是自己管理员
                            users_model.insertElementAt(temp, 0);
                        }else { //如果是别人管理员
                            users_model.insertElementAt(temp, 1);
                        }
                    } else {
                        String temp = "[用户] " + name;
                        if (repeatNameCount.containsKey(temp)) {
                            String nameNum = addAutoId(temp);
                            if (nameNum!=null){
                                temp = temp + "(" + num + ")";
                            }
                        } else {
                            repeatNameCount.put(temp, 0);
                        }
                        users_map.put(temp, id);
                        users_model.addElement(temp);
                    }
                    userlist.ensureIndexIsVisible(num++);
                    if (me) {
                        userId = id;
                        userName = name;
                        me = false;
                    }
                }
            }

        }

        users_label.setText("聊天室内人数：" + users_map.size());
    }


    /**
     * @MethodName AllPrivateChatActions
     * @Params * @param null
     * @Description 处理私聊相应指令
     * @Return
     * @Since 2020/7/10
     */

    protected void AllPrivateChatActions(String content) {
        if (content.length() > 0) {
            Pattern pattern = Pattern.compile("<id>(.*)</id><order>(.*)</order><msg>(.*)</msg><time>(.*)</time>");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String id = matcher.group(1);
                String order = matcher.group(2);
                String msg = matcher.group(3);
                String time = matcher.group(4);
                switch (order) {
                    case "1": //建立私聊窗口
                        if (!privateChatFrameMap.containsKey(id)) {
                            ClientFrame.privateChatFrame chatFrame = new ClientFrame.privateChatFrame("与" + getUserName(id) + "的私聊窗口", getUserName(id), id);
                            privateChatFrameMap.put(id, chatFrame);
                        }
                        break;
                    case "2": //关闭私聊窗口
                        ClientFrame.privateChatFrame endChatFrame = privateChatFrameMap.get(id);
                        JOptionPane.showMessageDialog(frame, "由于对方结束了私聊，该私聊窗口即将关闭！", "提示", JOptionPane.WARNING_MESSAGE);
                        endChatFrame.dispose();
                        insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + time, "由于 " + endChatFrame.otherName + " 关闭了私聊窗口，私聊结束！", sysVertical, true);
                        privateChatFrameMap.remove(id);
                        break;
                    case "3": //私聊消息
                        ClientFrame.privateChatFrame chatFrame = privateChatFrameMap.get(id);
                        insertMessage(chatFrame.textScrollPane, chatFrame.msgArea, null, time + " 对方说：", " " + msg, chatFrame.vertical, false);
                        break;
                    case "4": //对方离线，私聊窗口关闭
                        ClientFrame.privateChatFrame endChat = privateChatFrameMap.get(id);
                        JOptionPane.showMessageDialog(endChat, msg, "提示", JOptionPane.WARNING_MESSAGE);
                        endChat.dispose();
                        insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + time, "由于 " + endChat.otherName + " 离开了聊天室，私聊结束！", sysVertical, true);
                        privateChatFrameMap.remove(id);
                    default:
                        System.out.println("私聊指令有误！");
                }
            }
        }
    }


    /**
     * @ClassName privateChatFrame
     * @Params * @param null
     * @Description 私聊窗口GUI的内部类
     * @Return
     * @Since 2020/6/6
     */
    protected class privateChatFrame extends JFrame {
        private String otherName;
        private String otherId;
        private JButton sendButton;
        private JTextField msgTestField;
        private JTextPane msgArea;
        private JScrollPane textScrollPane;
        private JScrollBar vertical;

        public privateChatFrame(String title, String otherName, String otherId) throws HeadlessException {
            super(title);
            this.otherName = otherName;
            this.otherId = otherId;
            //全局面板容器
            JPanel panel = new JPanel();
            //全局布局
            BorderLayout layout = new BorderLayout();

            JPanel headpanel = new JPanel();    //上层panel，
            JPanel footpanel = new JPanel();    //下层panel
            JPanel centerpanel = new JPanel(); //中间panel

            //头部布局
            FlowLayout flowLayout = new FlowLayout();
            //底部布局
            GridBagLayout gridBagLayout = new GridBagLayout();

            setSize(600, 500);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setContentPane(panel);
            setLayout(layout);

            headpanel.setLayout(flowLayout);
            footpanel.setLayout(gridBagLayout);
            footpanel.setPreferredSize(new Dimension(0, 40));
            centerpanel.setLayout(gridBagLayout);

            //添加头部部件
            JLabel Name = new JLabel(otherName);
            headpanel.add(Name);

            //设置底部布局
            sendButton = new JButton("发送");
            sendButton.setPreferredSize(new Dimension(40, 0));
            msgTestField = new JTextField();
            footpanel.add(msgTestField, new GridBagConstraints(0, 0, 1, 1, 100, 100,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
            footpanel.add(sendButton, new GridBagConstraints(1, 0, 1, 1, 10, 10,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));

            //中间布局
            msgArea = new JTextPane();
            msgArea.setEditable(false);
            textScrollPane = new JScrollPane();
            textScrollPane.setViewportView(msgArea);
            vertical = new JScrollBar(JScrollBar.VERTICAL);
            vertical.setAutoscrolls(true);
            textScrollPane.setVerticalScrollBar(vertical);
            centerpanel.add(textScrollPane, new GridBagConstraints(0, 0, 1, 1, 100, 100,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

            //设置顶层布局
            panel.add(headpanel, "North");
            panel.add(footpanel, "South");
            panel.add(centerpanel, "Center");

            //窗口关闭事件
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int option = JOptionPane.showConfirmDialog(e.getOppositeWindow(), "确定结束私聊？", "提示",
                            JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        String msg = "<id>" + otherId + "</id><order>2</order><msg>exit</msg>";
                        MsgModel msgModel = new MsgModel((byte) 2, (byte) 2, msg);
                        chatRoomClient.getSocketChannel().writeAndFlush(msgModel); // 写进通道
                        insertMessage(sysTextScrollPane, sysMsgArea, null, "[系统消息] " + format(new Date()), "您与 " + otherName + " 的私聊已结束", sysVertical, true);
                        dispose();
                        privateChatFrameMap.remove(otherId);
                    } else {
                        return;
                    }
                }
            });

            //聊天信息输入框的监听回车按钮事件
            msgTestField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                        if (!chatRoomClient.getSocketChannel().isActive() || chatRoomClient == null) {
                            JOptionPane.showMessageDialog(frame, "请先连接聊天室的服务器！", "提示", JOptionPane.WARNING_MESSAGE);
                            dispose();
                            return;
                        }
                        String text = msgTestField.getText();
                        if (!StringUtil.isNullOrEmpty(text)) {
                            String msg = "<id>" + otherId + "</id><order>3</order><msg>" + text + "</msg>";
                            MsgModel msgModel = new MsgModel((byte) 2, (byte) 2, msg);
                            chatRoomClient.getSocketChannel().writeAndFlush(msgModel); // 写进通道
                            msgTestField.setText("");
                            insertMessage(textScrollPane, msgArea, null, format(new Date()) + " 你说：", " " + text, vertical, false);
                        }
                    }
                }
            });
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String cmd = e.getActionCommand();
                    if (cmd.equals("发送")) {
                        if (!chatRoomClient.getSocketChannel().isActive() || chatRoomClient == null) {
                            JOptionPane.showMessageDialog(frame, "请先连接聊天室的服务器！", "提示", JOptionPane.WARNING_MESSAGE);
                            dispose();
                            return;
                        }
                        String text = msgTestField.getText();
                        if (!StringUtil.isNullOrEmpty(text)) {
                            String msg = "<id>" + otherId + "</id><order>3</order><msg>" + text + "</msg>";
                            MsgModel msgModel = new MsgModel((byte) 2, (byte) 2, msg);
                            chatRoomClient.getSocketChannel().writeAndFlush(msgModel); // 写进通道
                            msgTestField.setText("");
                            insertMessage(textScrollPane, msgArea, null, format(new Date()) + " 你说：", " " + text, vertical, false);
                        }
                    }
                }
            });
            //窗口显示
            setVisible(true);
        }
    }


    /**
     * @MethodName setUIStyle
     * @Params * @param null
     * @Description 根据操作系统自动变化GUI界面风格
     * @Return
     * @Since 2020/6/6
     */
    public static void setUIStyle() {
//       String lookAndFeel = UIManager.getSystemLookAndFeelClassName(); //设置当前系统风格
        String lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName(); //可跨系统
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}