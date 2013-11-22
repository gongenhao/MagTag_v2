package org.opencv.samples.tutorial2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.Activity;

public class PurchaseActivity extends Activity{

	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.purchase);
	        ImageButton ib = (ImageButton) findViewById(R.id.imageButtonReturn);
		    ib.setOnClickListener(new View.OnClickListener() {
		        @Override
		        public void onClick(View v) {
		            Toast.makeText(PurchaseActivity.this, "purchased", Toast.LENGTH_SHORT).show();
		            goHome();
		        }
		    });

   	    }

	    private void goHome() {
	    	PurchaseActivity.this.setResult(1);
	    	//其中1为标志位,	    	前一个activity中的onActivityForResult中可进行设置
	    	PurchaseActivity.this.finish();	        
	    }
	    
	   
}
