package org.opencv.samples.tutorial2;

import java.io.File;
import java.util.HashMap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Tutorial2Activity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "OCVSample::Activity";

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_GRAY     = 1;
    private static final int       VIEW_MODE_CANNY    = 2;
    private static final int       VIEW_MODE_FEATURES = 5;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;
    
  //to recongnize
    private Mat					   mObject;
    private Bitmap				   bmpObject;
    
    /**HashMap that holds added images IDs and its titles in the matching pool */
    private HashMap<Integer, String> imageTitles=new HashMap<Integer,String>();

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("mixed_sample");

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial2Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @SuppressLint("NewApi")
	public ImageButton createImageButton(RelativeLayout layout,int x, int y)
    {
    	//create imaeg button
    	ImageButton myButton = new ImageButton(this);
    	myButton.setBackgroundResource(R.drawable.btn_item_mdpi);
    	myButton.getBackground().setAlpha(100);
    	//set size
    	myButton.setMaxWidth(100);
    	myButton.setMinimumWidth(50);
        myButton.setMaxHeight(100);
        myButton.setMinimumHeight(50);
        
        //set focus
        myButton.setFocusable(true);
        myButton.setFocusableInTouchMode(true);
                
        //set position
        layout.measure(0, 0);        
        int lw=layout.getMeasuredWidth();
        int lh=layout.getMeasuredHeight();
        myButton.measure(0, 0);
        int w=myButton.getMeasuredWidth();
        int h=myButton.getMeasuredHeight();
        /*
        myButton.setLeft(Math.max(1,x-w/2));        
        myButton.setTop(Math.max(1,y-h/2));
        myButton.setScaleType(ScaleType.CENTER_INSIDE);

        myButton.setPadding(myButton.getPaddingLeft()+x,myButton.getPaddingTop()+y,
        		myButton.getPaddingRight()-x,myButton.getPaddingBottom()-y);
        */
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT); 
        params.leftMargin = x-w/2; //Your X coordinate
        params.topMargin = y-h/2; //Your Y coordinate
        myButton.setLayoutParams(params);
        
        //btnLp.setMargins(x-w/2, y-h/2, lw-x-w/2, lh-y-h/2);
        Log.i(TAG,"create button at "+x+' '+y+' '+w+' '+h+' '+lw+' '+lh);
        
    	
    	return myButton;
    }
    
    private int addImageFromResources(int imageResourceId, String title) {
		/**Add images from local resources to image matching pool
     * Adding image returns image id assigned to the image in the matching pool.
     * We will save it to know which image was matched
     * 
     * (The best practice is to make image adding in another thread to avoid the system to get stuck)
     * 
     * IMORTANT !!! Decoding large images to Bitmap may result in insufficient memory allocations and application crashing, 
     * therefore large images must be reduced/scaled.
     */        
    	int imagePool_Id=-1;
        try
        {
        	Bitmap bmp = BitmapFactory.decodeResource(getResources(), imageResourceId);    	
        	//image prop
        	bmpObject=bmp;
        	File root = Environment.getExternalStorageDirectory();
            bmpObject =  BitmapFactory.decodeFile(root+"/pg46.jpg");
        	int height=bmpObject.getHeight();
            int width=bmpObject.getWidth();
            Log.i(TAG, "loaded image Size "+height +" , "+ width);
            //record
        	imagePool_Id=imageResourceId;
        	imageTitles.put(imagePool_Id,title);
        	//to mat
        	Mat mObjectRgb = new Mat(height,width, CvType.CV_8UC4);
            Utils.bitmapToMat(bmpObject,mObjectRgb);
            //downsample
            int height_ds=400;
            int width_ds=300;            
            Mat mObjectGray = new Mat(height,width, CvType.CV_8UC1);
            Imgproc.cvtColor(mObjectRgb, mObjectGray, Imgproc.COLOR_RGB2GRAY, 1);
            mObject = new Mat(height_ds,width_ds, CvType.CV_8UC1);
            Imgproc.resize(mObjectGray, mObject, mObject.size(), 0, 0, Imgproc.INTER_LINEAR);
            //log
            Log.i(TAG, "sampled image Size "+mObject.rows() +" , "+mObject.cols());
        	Log.i(TAG,"image added to the pool with id: " + imagePool_Id);
        	//recycle
        	bmp.recycle();
        }
        catch (Exception e)
        {
        	Log.i(TAG,"load image err" + e );
        }
        return imagePool_Id;
    }

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.tutorial2_surface_view);
        
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.relativeLayout);
        
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial2_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
      
       //set layout
        setLayout(layout);
          
    }
    
    private void setLayout(RelativeLayout layout) {
    	 //set layout    	
                
        //top bar
        View v = findViewById(R.id.imageView1);//找到你要设透明背景的layout 的id 
        v.getBackground().setAlpha(100);//0~255透明度值 
        v = findViewById(R.id.imageButton1);
        v.getBackground().setAlpha(100);
        
        //buttons        
       
        //left-top corner menu button
        ImageButton TagButton = createImageButton(layout,500,300);
        layout.addView(TagButton);
        TagButton.setOnFocusChangeListener(new OnFocusChangeListener()  
        {  
          public void onFocusChange(View arg0, boolean isFocused)  
          {  
            // TODO Auto-generated method stub  
              
            /*若ImageButton状态为onFocus改变ImageButton的图片 
             * 并改变textView的文字*/
        	ImageButton myImageButton = (ImageButton) arg0;
            if (isFocused==true)  
            {
            	myImageButton.setBackgroundResource(R.drawable.btn_item_down);           
            }  
            /*若ImageButton状态为offFocus改变ImageButton的图片 
             *并改变textView的文字*/  
            else   
            {  
            	myImageButton.setBackgroundResource(R.drawable.btn_item_mdpi);                
            }  
          }  
        });
        
        TagButton.setOnTouchListener(new OnTouchListener(){      
        	  
            @Override     
       
            public boolean onTouch(View v, MotionEvent event) {      
       
                   if(event.getAction() == MotionEvent.ACTION_DOWN){
                	   v.setBackgroundResource(R.drawable.btn_item_down);
                    }else if(event.getAction() == MotionEvent.ACTION_UP){
                    	v.setBackgroundResource(R.drawable.btn_item_mdpi);
                    }             
                   return false;
            }
        });  
        
        TagButton.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	//ImageButton myImageButton = (ImageButton) v;	        	
	        	//myImageButton.setBackgroundResource(R.drawable.btn_item_down);
	            Toast.makeText(Tutorial2Activity.this, "tagged", Toast.LENGTH_SHORT).show();
	            goPurchase();
	            //myImageButton.setBackgroundResource(R.drawable.btn_item_mdpi);  
	        }
	    });

        
        
        
        
        //customized testing button
        Button buyButton = new Button(this);
        buyButton.setText("testing");
        buyButton.setWidth(100);
        buyButton.setHeight(100);
        buyButton.setPadding(buyButton.getPaddingLeft()+200, buyButton.getPaddingTop(),
        		buyButton.getPaddingRight()-200, buyButton.getPaddingBottom());
        buyButton.setFocusable(true);
        buyButton.setFocusableInTouchMode(true);
    	
    }
    
    private void goPurchase() {
        Intent intent = new Intent(Tutorial2Activity.this,PurchaseActivity.class);
        Tutorial2Activity.this.startActivity(intent);        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
      //load iamges
        addImageFromResources(R.drawable.pg46, "pg46");
        
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
        case VIEW_MODE_GRAY:
            // input frame has gray scale format
            Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case VIEW_MODE_RGBA:
            // input frame has RBGA format
            mRgba = inputFrame.rgba();
            break;
        case VIEW_MODE_CANNY:
            // input frame has gray scale format
            mRgba = inputFrame.rgba();
            Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case VIEW_MODE_FEATURES:
            // input frame has RGBA format
            mRgba = inputFrame.rgba();
            mGray = inputFrame.gray();
            //FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
            int[] pos_detection;
            pos_detection=MatchFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), mObject.getNativeObjAddr());
            if ((pos_detection!=null) && (pos_detection.length>1))
            	Log.i(TAG,"Detected Item #"+pos_detection[0]+"@("+pos_detection[1]+","+pos_detection[2]+") with Error("+pos_detection[3]+"x"+pos_detection[4]+")");
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.relativeLayout);
            break;
        }

        return mRgba;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        }

        return true;
    }

    //public native void FindFeatures(long matAddrGr, long matAddrRgba);
    //public native void MatchFeatures(long matAddrGr, long matAddrRgba, long matAddrObject);
    public native int[] MatchFeatures(long matAddrGr, long matAddrRgba, long matAddrObject);

}
