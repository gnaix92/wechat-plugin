package com.gnaix.wechatplugin;

import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

public class PluginService extends AccessibilityService {

    static final String TAG = "PluginService";

    /** 微信的包名 */
    static final String WECHAT_PACKAGENAME      = "com.tencent.mm";
    /** 拆红包类*/
    static final String WECHAT_RECEIVER_CALSS   = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    /** 红包详情类 */
    static final String WECHAT_DETAIL_CALSS     = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    /** 微信主界面或者是聊天界面 */
    static final String WECHAT_LAUNCHER         = "com.tencent.mm.ui.LauncherUI";


    /** QQ 红包界面*/
    static final String QQ_LAUNCHER             = "com.tencent.mobileqq.activity.ChatActivity";

    /** 红包消息的关键 */
    static final String WECHAT_NOTIFI_TEXT      = "[微信红包]";
    static final String WECHAT_KEY_TEXT         = "领取红包";
    static final String QQ_NOTIFI_TEXT          = "[QQ红包]";
    static final String QQ_KEY_TEXT             = "QQ红包";


    static boolean isNotifi                     = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        CharSequence className = event.getClassName();
        Log.d(TAG, "onAccessibilityEvent    eventType:" + eventType + ", className:" + className);

        // 通知栏事件
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> texts = event.getText();
            if (!texts.isEmpty()) {
                for (CharSequence t : texts) {
                    String text = String.valueOf(t);
                    if (text.contains(WECHAT_NOTIFI_TEXT) || text.contains(QQ_NOTIFI_TEXT)) {
                        openNotify(event);
                        break;
                    }
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            //第一次进入聊天页面
            /**
              微信主界面以及聊天界面应该采用的FragmentActivity+Fragment这样导致
              如果用户进入到微信主界面则会调用AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED，
              导致再次进入微信聊天界面不会再调用AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED，
              而会调用AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED，
              而AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED只要内容改变后都会调用，
             */
            intoHongBao(event);
        } else  {
            intoHongBao(event);
        }
    }

    /**
     * 打开通知栏消息
     */
    private void openNotify(AccessibilityEvent event) {
        Log.d(TAG, "openNotify");

        if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }
        // 将微信的通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
            isNotifi = true;
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理红包消息
     * @param event
     */
    private void intoHongBao(AccessibilityEvent event) {
        CharSequence className = event.getClassName();
        Log.d(TAG, "intoHongBao    className:" + className);

        checkScreen(getApplicationContext());
        if (WECHAT_RECEIVER_CALSS.equals(className)) {
            // 点中了红包，下一步就是去拆红包
            getHongBao(event);
        } else if (WECHAT_LAUNCHER.equals(className) || QQ_LAUNCHER.equals(className)) {
            // 在聊天界面,去点中红包
            searchHongBao(event);
        } else if(WECHAT_DETAIL_CALSS.equals(className)){
            //红包详情直接退出
            //clickChild(getRootInActiveWindow());
            //performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        } else{
            searchHongBao(event);
        }
    }

    /**
     * 处理红包
     */
    private void getHongBao(AccessibilityEvent event) {
        Log.d(TAG, "getHongBao     ");
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null)
            return;
        clickChild(nodeInfo, event);
    }

    /**
     * 遍历子View
     * @param nodeInfo
     */
    private void clickChild(AccessibilityNodeInfo nodeInfo, AccessibilityEvent event){
        if (nodeInfo.getChildCount() > 0) {
            int childCount = nodeInfo.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
                clickChild(childNodeInfo, event);
            }
        }
        //红包详情直接退出
        if(WECHAT_DETAIL_CALSS.equals(event.getClassName())){
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            Log.i(TAG, "detail return");
            return;
        }
        if(nodeInfo.getClassName().toString().contains("Button")){
            //抢完返回 nitification 返回主页面
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            Log.i(TAG,"get return");
            //if(isNotifi){
            //    performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            //}else {
            //    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            //}
            return;
        }


    }

    /**
     * 查找红包
     * @param event
     */
    private void searchHongBao(AccessibilityEvent event) {

        CharSequence className = event.getClassName();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        Log.d(TAG, "searchHongBao     className:" + className);
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        if(QQ_LAUNCHER.equals(className)){
            //QQ红包
            List<AccessibilityNodeInfo> qqList = nodeInfo.findAccessibilityNodeInfosByText(QQ_KEY_TEXT);
            if (!qqList.isEmpty()) {
                // 界面上的红包总个数
                int totalCount = qqList.size();
                // 领取最近发的红包
                for (int i = totalCount - 1; i >= 0; i--) {
                    // 如果为领取过该红包，则执行点击、
                    AccessibilityNodeInfo parent = qqList.get(i).getParent();
                    Log.d(TAG,"QQ  openHongBao ");
                    if (parent != null) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                }

            }
        }else {
            //获取好友昵称
            CharSequence name = null;
            try {
                if (nodeInfo.getChildCount() >= 1 && nodeInfo.getChild(0).getChildCount() >= 1 && nodeInfo.getChild(0).getChild(0).getChildCount() >= 2) {
                    name = nodeInfo.getChild(0).getChild(0).getChild(1).getText();
                    Log.d(TAG, "name:  " + name);
                }else{
                    return;
                }

                //微信红包
                List<AccessibilityNodeInfo> wxList = nodeInfo.findAccessibilityNodeInfosByText(WECHAT_KEY_TEXT);
                if(name != null) {
                    List<AccessibilityNodeInfo> getList = nodeInfo.findAccessibilityNodeInfosByText("你领取了" + name.toString() + "的红包");
                    Log.i(TAG, "H:" + wxList.size() + ", Y:" + getList.size());
                    //已经全领取不进入红包详情(防止 读取到上一个已领取信息)
                    if (wxList.size() < getList.size()) return;
                }
                if (!wxList.isEmpty()) {
                    // 界面上的红包总个数
                    int totalCount = wxList.size();
                    // 领取最近发的红包
                    for (int i = totalCount - 1; i >= 0; i--) {
                        // 如果未领取过该红包，则执行点击、
                        AccessibilityNodeInfo parent = wxList.get(i).getParent();
                        Log.d(TAG, "WeChat  openHongBao " + i);
                        if (parent != null) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @description: 检查屏幕是否亮着并且唤醒屏幕
     * @date: 2016-1-29 下午2:08:25
     * @author: yems
     */
    private void checkScreen(Context context) {
        // TODO Auto-generated method stub
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (!pm.isScreenOn()) {
            wakeUpAndUnlock(context);
        }

    }

    private void wakeUpAndUnlock(Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
        // 解锁
        kl.disableKeyguard();
        // 获取电源管理器对象
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
        // 点亮屏幕
        wl.acquire();
        // 释放
        wl.release();
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "连接抢红包服务", Toast.LENGTH_SHORT).show();
    }

}
