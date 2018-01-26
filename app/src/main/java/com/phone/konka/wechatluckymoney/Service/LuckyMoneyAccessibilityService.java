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

    private boolean isScreenLocked = false;

    private PowerManager mPowerManager;

    private KeyguardManager mKeyguardManager;

    private PowerManager.WakeLock mWakeLock;

    private KeyguardManager.KeyguardLock mkeyguardLock;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        mkeyguardLock = mKeyguardManager.newKeyguardLock("1");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        int type = event.getEventType();
        switch (type) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                if (isScreenLocked())
                    releaseLock(event);
                else {
                    handleNotification(event);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if (haveNotify) {
                    String className = event.getClassName().toString();
                    if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                        handleLuckyMoney();
                    } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                        openLuckyMoney();
                    } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                        close();
                    }
                }
                break;
        }
    }


    /**
     * 处理微信红包Notification
     *
     * @param event
     */
    private void handleNotification(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (texts != null && !texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                if (content.contains("微信红包")) {
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
            }
        }
    }


    /**
     * 检查屏幕是否锁屏
     *
     * @return
     */
    private boolean isScreenLocked() {
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }


    /**
     * 亮屏并且解锁
     * 无解锁密码时才有效
     */
    private void releaseLock(AccessibilityEvent event) {
        isScreenLocked = true;
        mWakeLock.acquire();
        mkeyguardLock.disableKeyguard();

        handleNotification(event);
    }


    /**
     * 点击聊天界面中的微信红包
     */
    private void handleLuckyMoney() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        AccessibilityNodeInfo node = recycleNode(rootNode);

        if (node != null) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return;
            }
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
                parent = parent.getParent();
            }
        }
    }


    /**
     * 查找聊天界面中的领取红包View
     *
     * @param node
     * @return
     */
    private AccessibilityNodeInfo recycleNode(AccessibilityNodeInfo node) {

        List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByText("领取红包");
        if (list != null && !list.isEmpty())
            return list.get(0);
        return null;
//        AccessibilityNodeInfo result = null;
//        if (node.getChildCount() == 0) {
//            if (node.getText() != null && node.getText().equals("获取红包")) {
//                return node;
//            } else
//                return null;
//        }
//        for (int i = 0; i < node.getChildCount(); i++) {
//            if (node.getChild(i) != null) {
//                result = recycleNode(node.getChild(i));
//                if (result != null)
//                    return result;
//            }
//        }
//        return result;
    }


    /**
     * 点击“开”，进行抢红包
     */
    private void openLuckyMoney() {

//        if (rootNode != null) {
//            List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByViewId("@id/c2i");
//            rootNode.recycle();
//            for (AccessibilityNodeInfo node : list) {
//                if (node.isClickable()) {
//                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    break;
//                }
//            }
//        }
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        int count = rootNode.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            AccessibilityNodeInfo node = rootNode.getChild(i);
            if (node.getClassName().equals("android.widget.Button")) {
                if (node.isClickable())
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
    }


    /**
     * 关闭界面
     */
    private void close() {
        haveNotify = false;
        performGlobalAction(GLOBAL_ACTION_BACK);
        new MyTimer().schedule();
    }

    @Override
    public void onInterrupt() {
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
                        mkeyguardLock.reenableKeyguard();
                        mWakeLock.release();
                    }
                }
            };
            schedule(task, 250);
        }
    }
}
