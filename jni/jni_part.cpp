#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <vector>
#include <math.h>
#include <android/log.h>

#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <opencv2/calib3d/calib3d.hpp>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT jintArray JNICALL Java_org_opencv_samples_tutorial2_Tutorial2Activity_MatchFeatures(JNIEnv* env, jobject obj, jlong addrGray, jlong addrRgba, jlong addrObject);

JNIEXPORT jintArray JNICALL Java_org_opencv_samples_tutorial2_Tutorial2Activity_MatchFeatures(JNIEnv* env, jobject obj , jlong addrGray, jlong addrRgba, jlong addrObject)
{
	char stringtolog[100];
	const char* TAG="JNI";

	int detected=0;

	//load frame data
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;

    //load image data
    Mat& mObject = *(Mat*)addrObject;
    sprintf(stringtolog,"image load size (%d,%d)\n", mObject.rows,mObject.cols);
           __android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);


    //keypoints
    vector<KeyPoint> keypoints_scene;
    vector<KeyPoint> keypoints_object;

    //FastFeatureDetector detector(50);
    //detector.detect(mGr, v);

    //ORB detection
    OrbFeatureDetector detector(200,1.2,8,31,0,2,0,31);
    //SiftFeatureDetector detector(100,3,0.04,10,1.6);
    detector.detect(mGr, keypoints_scene);
    detector.detect(mObject, keypoints_object);
    sprintf(stringtolog,"SIFT Feature-- Keypoints %d in object, %d in scene\n", keypoints_object.size(),keypoints_scene.size());
       __android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log

    //extraction
    //SiftDescriptorExtractor extractor;
    OrbDescriptorExtractor extractor;
    Mat descriptor_scene;
    Mat descriptor_object;
    extractor.compute(mGr,keypoints_scene,descriptor_scene);
    extractor.compute(mObject,keypoints_scene,descriptor_object);

    //draw keypoints
    bool todrawkeypoints=false;
    if (todrawkeypoints) {
    	for( unsigned int i = 0; i < keypoints_scene.size(); i++ )
    	    {
    	        const KeyPoint& kp = keypoints_scene[i];
    	        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    	    }
    }

    //match
    std::vector<DMatch> matches;

	// L2 distance based matching. Brute Force Matching
	cv::BFMatcher matcher( NORM_HAMMING,false);

	//cv::FlannBasedMatcher matcher;

    // display of corresponding points
    matcher.match( descriptor_object, descriptor_scene, matches );

    //quick calculation of max and min distance
    double max_dist=-1;
    double min_dist=10000;
    double sum_dist=0;
    double sumsqr_dist=0;
    int dist_count[100]={0};
    //vector<double> dist_vector;

    //quick sort and process distances
    for (int i=0;i<descriptor_object.rows;i++)
    {
    	const DMatch& dmatch=matches[i];
		double dist = dmatch.distance;
		min_dist=(min_dist<dist)?min_dist:dist;
		max_dist=(max_dist>dist)?max_dist:dist;
		dist_count[int(dist/10)]=dist_count[int(dist/10)]+1;
		sum_dist+=dist;
		sumsqr_dist+=(110-dist)*(110-dist);
	//	dist_vector.push_back(dist);
    }

    //log
        sprintf(stringtolog,"SIFT Feature-- Max dist : %.2f \n", max_dist );
    	__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log
    	sprintf(stringtolog,"SIFT Feature-- Min dist : %.2f \n", min_dist );
    	__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log
    	sprintf(stringtolog,"SIFT Feature-- Mean dist : %.2f \n",  sum_dist/(descriptor_object.rows+0.0f));
		__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log
		sprintf(stringtolog,"SIFT Feature-- RMS dist : %.2f \n",  sqrt((sumsqr_dist+0.0f)/(descriptor_object.rows+0.0f)));
		__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log




//threshold based on empirical
    double num_std=3.0f;
    double dist=70.0f/num_std;
    min_dist=(min_dist<dist)?min_dist:dist;


    //draw matches
    std::vector< DMatch > good_matches;
    bool todrawmatch=true;
	for (int i=0; i<descriptor_object.rows;i++) {

			const DMatch& dmatch=matches[i];
			const KeyPoint& kp = keypoints_scene[dmatch.trainIdx];
			double dist = dmatch.distance;
			if (dist<min_dist*num_std) {
				if (todrawmatch)
					circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(0,255,0,255));
				good_matches.push_back(dmatch);
			}
	}

	sprintf(stringtolog,"SIFT Feature-- Good Matches : %d \n",  good_matches.size());
	__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log



	//compute position
	std::vector<cv::Point2f> points_obj;
	std::vector<cv::Point2f> points_scene;

	for (int i=0; i<good_matches.size();i++)
	{
		//Get the keypoints from the good matches
		const DMatch& dmatch=good_matches[i];
		const KeyPoint& oKeyPoint=keypoints_object[dmatch.queryIdx];
		points_obj.push_back(oKeyPoint.pt);
		const KeyPoint& sKeyPoint=keypoints_scene[dmatch.trainIdx];
		points_scene.push_back(sKeyPoint.pt);
	}

	//clear 1	//clear memory
	int n_goodmatches=good_matches.size();
	if (n_goodmatches<=4)
		return NULL;
	while (good_matches.begin()!=good_matches.end())
		good_matches.pop_back();
	good_matches.clear();
	while (matches.begin()!=matches.end())
		matches.pop_back();
	matches.clear();


	//if enough points do homography
	Mat myHomo = findHomography(points_obj,points_scene,CV_RANSAC);

	//check error
	perspectiveTransform( points_obj, points_scene, myHomo);
	float HomoError=0.0f;
	for (int i=0; i<n_goodmatches;i++)
	{
		const Point2f p1=points_obj[i];
		const Point2f p2=points_scene[i];
		float dis=(cv::norm(p1-p2));
		HomoError+=dis*dis;
	}
	sprintf(stringtolog,"Homography Mean Error %.2f \n",  HomoError/(n_goodmatches+0.0f));
		__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log


	//compute position
	std::vector<cv::Point2f> obj_corners(5);
	std::vector<Point2f> scene_corners(5);
	obj_corners[0] = cvPoint(0,0);
	obj_corners[1] = cvPoint( mObject.cols, 0 );
	obj_corners[2] = cvPoint( mObject.cols, mObject.rows );
	obj_corners[3] = cvPoint( 0, mObject.rows );
	obj_corners[4] = cvPoint( mObject.cols/2, mObject.rows*4/11);
	perspectiveTransform( obj_corners, scene_corners, myHomo);
	Point2f kp=scene_corners[4];

    //draw position
	if (HomoError<20000) {
		//if error is small
			circle(mRgb, Point2f(kp.x, kp.y), 50, Scalar(0,0,255,255),10);
	}
	circle(mRgb, Point2f(kp.x, kp.y), 50, Scalar(0,0,255,255),10);


	//output
	jintArray graphic;
	jint size = 5;
	graphic = (env)->NewIntArray(size);
	if(graphic == NULL) {
		sprintf(stringtolog,"JNI array not created");
		__android_log_write(ANDROID_LOG_INFO, TAG, stringtolog);// JNI log
		return NULL;
	}
	jint fill[size];
	fill[0]=(jint)1;//image id
	fill[1]=(jint) kp.x;//x position
	fill[2]=(jint) kp.y;//y position
	fill[3]=(jint) HomoError/(n_goodmatches+0.0f); //fitting error
	fill[4]=(jint) n_goodmatches; // matched feature
	env->SetIntArrayRegion(graphic,0,size,fill);

	//clear memory
		while (obj_corners.begin()!=obj_corners.end())
			obj_corners.pop_back();
		obj_corners.clear();
		while ( scene_corners.begin()!= scene_corners.end())
			 scene_corners.pop_back();
		scene_corners.clear();
		while (points_obj.begin()!= points_obj.end())
				points_obj.pop_back();
		points_obj.clear();
		while (points_scene.begin()!= points_scene.end())
					points_scene.pop_back();
		points_scene.clear();

	//return
		return graphic;



}
}
