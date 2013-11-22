package org.opencv.samples.tutorial2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * 
 * @{#} SplashActivity.java Create on 2013-5-2 ����9:10:01    
 *    
 * class desc:   ��������
 *
 * <p>Copyright: Copyright(c) 2013 </p> 
 * @Version 1.0
 * @Author <a href="mailto:gaolei_xj@163.com">Leo</a>   
 *  
 *
 */
public class SplashActivity extends Activity {

    //�ӳ�3�� 
    private static final long SPLASH_DELAY_MILLIS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        // ʹ��Handler��postDelayed������3���ִ����ת��MainActivity 
        new Handler().postDelayed(new Runnable() {
            public void run() {
                goHome();
            }
        }, SPLASH_DELAY_MILLIS);
    }

    private void goHome() {
        Intent intent = new Intent(SplashActivity.this, Tutorial2Activity.class);
        SplashActivity.this.startActivity(intent);
        SplashActivity.this.finish();
    }
}