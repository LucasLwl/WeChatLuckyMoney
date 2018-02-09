package com.phone.konka.wechatluckymoney.Service;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 廖伟龙 on 2018-1-25.
 */

public class LuckyMoneyAccessibilityService extends AccessibilityService {

    private boolean haveNotify = false;

    private boolean haveLuckyMoney = false;

    private boolean isInLaunchUI = false;

    private boolean isScreenLocked = false;

    private KeyguardManager mKeyguardManager;

    private PowerManager.WakeLock mWakeLock;

    private KeyguardManager.KeyguardLock mKeyguardLock;

    private AccessibilityNodeInfo mCacheNode = null;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        mKeyguardLock = mKeyguardManager.newKeyguardLock("luckyMoney");
    }

    @Override
    public void onInterrupt() {
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        int type = event.getEventType();
        String className = event.getClassName().toString();
        switch (type) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                if (isLuckyMoneyNotification(event)) {
                    if (isScreenLocked())
                        releaseLock();
                    handleNotification(event);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (className.equals("com.tencent.mm.ui.LauncherUI"))
                    isInLaunchUI = true;
                else {
                    isInLaunchUI = false;
                    if (haveLuckyMoney) {
                        if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                            openLuckyMoney();
                        } else if (!className.contains("com.tencent.mm.ui.base")) {
                            close();
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (isInLaunchUI && !haveLuckyMoney) {
                    handleLuckyMoney();
                }
                break;
        }
    }


    /**
     * 状态栏信息是否为红包信息
     *
     * @param event
     * @return
     */
    private boolean isLuckyMoneyNotification(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (texts != null && !texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                if (content.contains("微信红包"))
                    return true;
            }
        }
        return false;
    }

    /**
     * 处理微信红包Notification
     *
     * @param event 状态栏事件
     */

    private void handleNotification(AccessibilityEvent event) {
        if (event.getParcelableData() != null &&
                event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
                haveNotify = true;
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 检查屏幕是否锁屏
     *
     * @return 锁屏 true
     */
    private boolean isScreenLocked() {
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }


    /**
     * 亮屏并且解锁
     * 无解锁密码时才有效
     */
    private void releaseLock() {
        isScreenLocked = true;
        mWakeLock.acquire();
        mKeyguardLock.disableKeyguard();
    }


    /**
     * 点击聊天界面中的微信红包
     */
    private void handleLuckyMoney() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        AccessibilityNodeInfo node = recycleNode(rootNode);
        if (node != null) {
            if (node.isClickable()) {
                haveLuckyMoney = true;
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                node.recycle();
                return;
            }
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    haveLuckyMoney = true;
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    parent.recycle();
                    break;
                }
                parent = parent.getParent();
            }
        }
    }


    /**
     * 查找聊天界面中的领取红包View
     *
     * @param rootNode 根节点
     * @return 红包节点
     */
    private AccessibilityNodeInfo recycleNode(AccessibilityNodeInfo rootNode) {

        List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText("领取红包");
        if (list != null && !list.isEmpty())
            return list.get(0);


//        领取自己发出去的红包
        list = rootNode.findAccessibilityNodeInfosByText("查看红包");
        if (list != null && !list.isEmpty()) {
            int count = list.size();
            AccessibilityNodeInfo node;
            for (int i = 0; i < count; i++) {
                node = list.get(i);
                if (!node.equals(mCacheNode)) {
                    mCacheNode = node;
                    return node;
                }
            }
        } else {
            mCacheNode = null;
        }
        return null;
    }


    /**
     * 点击“开”，进行抢红包
     */
    private void openLuckyMoney() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        int count = rootNode.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            AccessibilityNodeInfo node = rootNode.getChild(i);
            if (node.getClassName().equals("android.widget.Button")) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    node.recycle();
                }
            }
        }
    }


    /**
     * 关闭界面
     */
    private void close() {
        haveLuckyMoney = false;
        performGlobalAction(GLOBAL_ACTION_BACK);
        if (haveNotify) {
            haveNotify = false;
            new MyTimer().schedule();
        }
    }


    private class MyTimer extends Timer {
        private TimerTask task;

        public void schedule() {
            task = new TimerTask() {
                @Override
                public void run() {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    if (isScreenLocked) {
                        isScreenLocked = false;
                        mKeyguardLock.reenableKeyguard();
                        mWakeLock.release();
                    }
                }
            };
            schedule(task, 250);
        }
    }
}
