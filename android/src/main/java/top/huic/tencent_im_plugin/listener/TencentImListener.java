package top.huic.tencent_im_plugin.listener;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.tencent.imsdk.TIMConnListener;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMGroupEventListener;
import com.tencent.imsdk.TIMGroupTipsElem;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMMessageListener;
import com.tencent.imsdk.TIMRefreshListener;
import com.tencent.imsdk.TIMUserStatusListener;
import com.tencent.imsdk.ext.message.TIMMessageLocator;
import com.tencent.imsdk.ext.message.TIMMessageReceipt;
import com.tencent.imsdk.ext.message.TIMMessageReceiptListener;
import com.tencent.imsdk.ext.message.TIMMessageRevokedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import top.huic.tencent_im_plugin.TencentImPlugin;
import top.huic.tencent_im_plugin.ValueCallBack;
import top.huic.tencent_im_plugin.entity.MessageEntity;
import top.huic.tencent_im_plugin.entity.SessionEntity;
import top.huic.tencent_im_plugin.enums.ListenerTypeEnum;
import top.huic.tencent_im_plugin.util.JsonUtil;
import top.huic.tencent_im_plugin.util.TencentImUtils;

/**
 * 腾讯云IM监听器
 * @author 蒋具宏
 */
public class TencentImListener implements TIMUserStatusListener,
        TIMConnListener, TIMGroupEventListener,
        TIMRefreshListener, TIMMessageRevokedListener,
        TIMMessageListener, TIMMessageReceiptListener {

    /**
     * 日志签名
     */
    public static String TAG = TencentImListener.class.getName();

    /**
     * 监听器回调的方法名
     */
    private final static String LISTENER_FUNC_NAME = "onListener";

    /**
     * 与Flutter的通信管道
     */
    private static MethodChannel channel;

    public TencentImListener(MethodChannel channel) {
        TencentImListener.channel = channel;
    }

    /**
     * 调用监听器
     *
     * @param type   类型
     * @param params 参数
     */
    public static void invokeListener(ListenerTypeEnum type, Object params) {
        Map<String, Object> resultParams = new HashMap<>(2, 1);
        resultParams.put("type", type);
        resultParams.put("params", params == null ? null : JsonUtil.toJSONString(params));
        channel.invokeMethod(LISTENER_FUNC_NAME, JsonUtil.toJSONString(resultParams));
    }

    /**
     * 踢下线通知
     */
    @Override
    public void onForceOffline() {
        Log.d(TAG, "onForceOffline: ");
        invokeListener(ListenerTypeEnum.ForceOffline, null);
    }

    /**
     * 用户登录的 userSig 过期（用户需要重新获取 userSig 后登录）
     */
    @Override
    public void onUserSigExpired() {
        Log.d(TAG, "onUserSigExpired: ");
        invokeListener(ListenerTypeEnum.UserSigExpired, null);
    }

    /**
     * 网络连接成功
     */
    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected: ");
        invokeListener(ListenerTypeEnum.Connected, null);
    }

    /**
     * 网络连接断开（断线只是通知用户，不需要重新登录，重连以后会自动上线）
     */
    @Override
    public void onDisconnected(int i, String s) {
        Log.d(TAG, "onDisconnected: ");
        Map<String, Object> params = new HashMap<>(2, 1);
        params.put("code", i);
        params.put("msg", s);
        invokeListener(ListenerTypeEnum.Disconnected, params);
    }

    /**
     * wifi需要身份认证
     */
    @Override
    public void onWifiNeedAuth(String s) {
        Log.d(TAG, "onWifiNeedAuth: ");
        invokeListener(ListenerTypeEnum.WifiNeedAuth, s);
    }

    /**
     * 群消息事件
     */
    @Override
    public void onGroupTipsEvent(TIMGroupTipsElem timGroupTipsElem) {
        Log.d(TAG, "onGroupTipsEvent: ");
        invokeListener(ListenerTypeEnum.GroupTips, timGroupTipsElem);
    }

    /**
     * 会话刷新
     */
    @Override
    public void onRefresh() {
        Log.d(TAG, "onRefresh: ");
        invokeListener(ListenerTypeEnum.Refresh, null);
    }

    /**
     * 刷新部分会话
     */
    @Override
    public void onRefreshConversation(List<TIMConversation> list) {
        Log.d(TAG, "onRefreshConversation: ");
        // 获取资料后调用回调
        TencentImUtils.getConversationInfo(new ValueCallBack<List<SessionEntity>>(null) {
            @Override
            public void onSuccess(List<SessionEntity> data) {
                invokeListener(ListenerTypeEnum.RefreshConversation, data);
            }

            @Override
            public void onError(int code, String desc) {
                Log.d(TencentImPlugin.TAG, "getUsersProfile failed, code: " + code + "|descr: " + desc);
            }
        }, list);
    }

    /**
     * 消息撤回
     */
    @Override
    public void onMessageRevoked(TIMMessageLocator timMessageLocator) {
        Log.d(TAG, "onMessageRevoked: ");
        invokeListener(ListenerTypeEnum.MessageRevoked, timMessageLocator);
    }

    /**
     * 已读消息通知
     *
     * @param list 消息列表
     */
    @Override
    public void onRecvReceipt(List<TIMMessageReceipt> list) {
        Log.d(TAG, "onRecvReceipt: ");
        List<String> rs = new ArrayList<>(list.size());
        for (TIMMessageReceipt timMessageReceipt : list) {
            rs.add(timMessageReceipt.getConversation().getPeer());
        }
        invokeListener(ListenerTypeEnum.RecvReceipt, rs);
    }

    /**
     * 新消息通知
     *
     * @param list 消息列表
     * @return 默认情况下所有消息监听器都将按添加顺序被回调一次，除非用户在 onNewMessages 回调中返回 true，此时将不再继续回调下一个消息监听器
     */
    @Override
    public boolean onNewMessages(List<TIMMessage> list) {
        Log.d(TAG, "onNewMessages: " + list.toString());
        TencentImUtils.getMessageInfo(list, new ValueCallBack<List<MessageEntity>>(null) {
            @Override
            public void onSuccess(List<MessageEntity> messageEntities) {
                invokeListener(ListenerTypeEnum.NewMessages, messageEntities);
            }

            @Override
            public void onError(int code, String desc) {
                Log.d(TencentImPlugin.TAG, "getUsersProfile failed, code: " + code + "|descr: " + desc);
            }
        });
        return false;
    }
}