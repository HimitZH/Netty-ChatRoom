package top.hcode.client;

import io.netty.util.internal.StringUtil;
import top.hcode.commons.MsgModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/10 14:57
 * @Description: Admin 管理员的初始化聊天室窗口，继承总窗口类，实现初始化方法
 */
public class Admin extends ClientFrame {

    public Admin(ChatRoomClient chatRoomClient) {
        super(chatRoomClient);
    }

    @Override
    public void frameInit() {
        repeatNameCount = new ConcurrentHashMap<>();
        users_map = new ConcurrentHashMap<>();
        privateChatFrameMap = new ConcurrentHashMap<>();
        frame = new JFrame("HCODE聊天室管理端");
        JPanel panel = new JPanel();        /*主要的panel，上层放置连接区，下层放置消息区，
                                                  中间是消息面板，左边是room列表，右边是当前room的用户列表*/
        JPanel headpanel = new JPanel();    /*上层panel，用于放置连接区域相关的组件*/
        JPanel footpanel = new JPanel();    /*下层panel，用于放置发送信息区域的组件*/
        JPanel centerpanel = new JPanel();    /*中间panel，用于放置聊天信息*/
        JPanel leftpanel = new JPanel();    /*左边panel，用于放置房间列表和加入按钮*/
        JPanel rightpanel = new JPanel();   /*右边panel，用于放置房间内人的列表*/

        /*最上层的布局，分中间，东南西北五个部分*/
        BorderLayout layout = new BorderLayout();
        /*格子布局，主要用来设置西、东、南三个部分的布局*/
        GridBagLayout gridBagLayout = new GridBagLayout();
        /*主要设置北部的布局*/
        FlowLayout flowLayout = new FlowLayout();
        /*设置初始窗口的一些性质*/
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setLayout(layout);
        /*设置各个部分的panel的布局和大小*/
        headpanel.setLayout(flowLayout);
        footpanel.setLayout(gridBagLayout);
        leftpanel.setLayout(gridBagLayout);
        centerpanel.setLayout(gridBagLayout);
        rightpanel.setLayout(gridBagLayout);
        //设置面板大小
        leftpanel.setPreferredSize(new Dimension(350, 0));
        rightpanel.setPreferredSize(new Dimension(200, 0));
        footpanel.setPreferredSize(new Dimension(0, 40));

        //头部布局
        //头部布局
        host_textfield = new JTextField("127.0.0.1");
        port_textfield = new JTextField("8888");
        name_textfield = new JTextField("匿名");
        host_textfield.setPreferredSize(new Dimension(100, 25));
        port_textfield.setPreferredSize(new Dimension(70, 25));
        name_textfield.setPreferredSize(new Dimension(150, 25));

        JLabel host_label = new JLabel("服务器IP:");
        JLabel port_label = new JLabel("端口:");
        JLabel name_label = new JLabel("管理员:");

        head_connect = new JButton("连接");
        head_exit = new JButton("退出");

        headpanel.add(host_label);
        headpanel.add(host_textfield);
        headpanel.add(port_label);
        headpanel.add(port_textfield);
        headpanel.add(name_label);
        headpanel.add(name_textfield);
        headpanel.add(head_connect);
        headpanel.add(head_exit);

        //底部布局
        foot_send = new JButton("发送");
        foot_sysSend = new JButton("发送系统消息");
        foot_sysSend.setPreferredSize(new Dimension(110, 0));
        foot_userClear = new JButton("清空聊天消息");
        foot_userClear.setPreferredSize(new Dimension(195, 0));

        sysText_field = new JTextField();
        sysText_field.setPreferredSize(new Dimension(230, 0));
        text_field = new JTextField();
        footpanel.add(sysText_field, new GridBagConstraints(0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
        footpanel.add(foot_sysSend, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 3), 0, 0));
        footpanel.add(text_field, new GridBagConstraints(2, 0, 1, 1, 100, 100,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
        footpanel.add(foot_send, new GridBagConstraints(3, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
        footpanel.add(foot_userClear, new GridBagConstraints(4, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));


        //左边布局
        JLabel sysMsg_label = new JLabel("系统日志：");
        sysMsgArea = new JTextPane();
        sysMsgArea.setEditable(false);
        sysTextScrollPane = new JScrollPane();
        sysTextScrollPane.setViewportView(sysMsgArea);
        sysVertical = new JScrollBar(JScrollBar.VERTICAL);
        sysVertical.setAutoscrolls(true);
        sysTextScrollPane.setVerticalScrollBar(sysVertical);
        leftpanel.add(sysMsg_label, new GridBagConstraints(0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        leftpanel.add(sysTextScrollPane, new GridBagConstraints(0, 1, 1, 1, 100, 100,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        //右边布局

        users_label = new JLabel("当前连接用户：0");
        privateChat_button = new JButton("私聊");
        kick_button = new JButton("踢出");
        users_model = new DefaultListModel<>();
        userlist = new JList<String>(users_model);
        JScrollPane userListPane = new JScrollPane(userlist);

        rightpanel.add(users_label, new GridBagConstraints(0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        rightpanel.add(privateChat_button, new GridBagConstraints(0, 1, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        rightpanel.add(kick_button, new GridBagConstraints(0, 2, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        rightpanel.add(userListPane, new GridBagConstraints(0, 3, 1, 1, 100, 100,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        //中间布局
        JLabel userMsg_label = new JLabel("世界聊天：");
        userMsgArea = new JTextPane();
        userMsgArea.setEditable(false);
        userTextScrollPane = new JScrollPane();
        userTextScrollPane.setViewportView(userMsgArea);
        userVertical = new JScrollBar(JScrollBar.VERTICAL);
        userVertical.setAutoscrolls(true);
        userTextScrollPane.setVerticalScrollBar(userVertical);

        centerpanel.add(userMsg_label, new GridBagConstraints(0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        centerpanel.add(userTextScrollPane, new GridBagConstraints(0, 1, 1, 1, 100, 100,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        //设置顶层布局
        panel.add(headpanel, "North");
        panel.add(footpanel, "South");
        panel.add(leftpanel, "West");
        panel.add(rightpanel, "East");
        panel.add(centerpanel, "Center");

        //将按钮事件全部注册到监听器

        //连接聊天室
        head_connect.addActionListener(chatRoomClient);
        //管理员发布消息
        foot_send.addActionListener(chatRoomClient);
        //退出聊天室
        head_exit.addActionListener(chatRoomClient);
        //清空消息日志
        foot_sysSend.addActionListener(chatRoomClient);
        //清空世界聊天消息
        foot_userClear.addActionListener(chatRoomClient);
        //私聊
        privateChat_button.addActionListener(chatRoomClient);
        //踢人
        kick_button.addActionListener(chatRoomClient);
        //窗口关闭事件
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int option = JOptionPane.showConfirmDialog(frame, "确定关闭聊天室界面?", "提示",
                        JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    if (e.getWindow() == frame) {
                        frame.dispose();
                        if (ChatRoomClient.socketChannel != null && !ChatRoomClient.socketChannel.isActive()) {
                            chatRoomClient.disconnect(); //优雅的关闭~
                        }
                        System.exit(0);
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        });

        //聊天信息输入框的监听回车按钮事件
        text_field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {

                if (e.getKeyChar() == KeyEvent.VK_ENTER) { //监听回车键
                    String text = text_field.getText(); // 获取群聊消息输入框的内容
                    if (!StringUtil.isNullOrEmpty(text)) { // 输入框内应该不为空
                        //先判断是否进入了聊天室。没进入则报错提醒！
                        if (ChatRoomClient.socketChannel != null && !ChatRoomClient.socketChannel.isActive()) {
                            JOptionPane.showMessageDialog(frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        String formatMsg = "<id>" + userId + "</id>" + "<msg>" + text + "</msg>";
                        MsgModel msgModel = new MsgModel((byte) 2, (byte) 1, formatMsg);
                        chatRoomClient.getSocketChannel().writeAndFlush(msgModel); //写入通道
                        text_field.setText("");
                    }
                }
            }
        });

        //发送系统消息的输入框的监听回车按钮事件
        sysText_field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) { //如果按下的是回车
                    String text = sysText_field.getText(); // 获取系统消息输入框的内容
                    if (!StringUtil.isNullOrEmpty(text)) { // 输入框内应该不为空
                        //先判断是否进入了聊天室。没进入则报错提醒！
                        if (ChatRoomClient.socketChannel != null && !ChatRoomClient.socketChannel.isActive()) {
                            JOptionPane.showMessageDialog(frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        MsgModel adminSysModel = new MsgModel((byte) 1, (byte) 7, text);
                        chatRoomClient.getSocketChannel().writeAndFlush(adminSysModel); //写入通道
                        sysText_field.setText("");
                    }
                }
            }
        });

        //窗口显示
        frame.setVisible(true);

        String name = JOptionPane.showInputDialog("请输入聊天所用昵称：");
        if (!StringUtil.isNullOrEmpty(name)) { // 如果弹窗的昵称不为空，则设置昵称
            name_textfield.setText(name);
        }

    }
}