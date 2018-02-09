package com.phone.konka.wechatluckymoney.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.phone.konka.wechatluckymoney.R;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openAccessibility();
    }


    /**
     * 跳转到系统辅助功能设置
     */
    private void openAccessibility() {
        if (!isAccessibilitySettingOn(this, getPackageName() + ".Service.LuckyMoneyAccessibilityService")) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if (intent.resolveActivity(getPackageManager()) != null)
                startActivity(intent);
        }
    }

    /**
     * 检测是否已打开Accessibility服务
     *
     * @param context
     * @param accessName
     * @return
     */
    private boolean isAccessibilitySettingOn(Context context, String accessName) {

        int accessibilityEnable = 0;
        String serviceName = getPackageName() + "/" + accessName;
        try {
            accessibilityEnable = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (accessibilityEnable == 1) {
            TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(serviceName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
