#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

extern "C" {

JNIEXPORT jintArray JNICALL Java_com_example_administrator_myapplication_ArActivity_Test
        (JNIEnv *env,
         jobject,
         jlong matAddrInput,
         jlong matAddrResult
        ) {

    Mat &img_input = *(Mat *) matAddrInput;
    Mat &img_output = *(Mat *) matAddrResult;

    cvtColor(img_input, img_output, CV_RGB2YCrCb);

    blur(img_output, img_output, Size(33, 33));

    inRange(img_output, Scalar(0,133,77), Scalar(255,173,127), img_output);

    erode(img_output, img_output, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));
    erode(img_output, img_output, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));
    //dilate(img_output, img_output, getstructuringelement(morph_ellipse, size(5, 5)));
    //dilate(img_output, img_output, getstructuringelement(morph_ellipse, size(5, 5)));

    threshold(img_output,img_output,127, 255, THRESH_BINARY);


    Mat stats,centroids,img_labels;
    int numOfLables = connectedComponentsWithStats(img_output,img_labels,stats, centroids, 8, CV_32S);

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;

    findContours(img_output, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE,
                 Point(0, 0));

    int largest_area = 0;
    int largest_contour_index = 0;

    int compareX=0,compareY=0;
    int index=0;

    for (int i = 0; i < contours.size(); i++) {
        double a = contourArea(contours[i], false);
        if (a > largest_area) {
            largest_area = a;
            largest_contour_index = i;
        }
    }


    vector<Point> hull(contours[largest_contour_index].size());
    vector<int> hulls(contours[largest_contour_index].size());
    vector<Vec4i> defects( contours[largest_contour_index].size() );

    convexHull(contours[largest_contour_index], hull,  false);
    convexHull(contours[largest_contour_index], hulls,  false);
    convexityDefects(contours[largest_contour_index],hulls,defects);

    vector <Point> defectsPointer;


    int tryx=0;
    int tryy=0;
    int devideNum =0;

    for(int i=0; i<defects.size(); i++){

        if((double)(defects[i][3]/256) > 80){
            //170
            Point defectsPoint (contours[largest_contour_index][defects[i][2]]);
            //circle(img_input, defectsPoint, 5, Scalar(255, 0,0 ), 5);

            tryx += contours[largest_contour_index][defects[i][2]].x;
            tryy += contours[largest_contour_index][defects[i][2]].y;
            devideNum ++;
            defectsPointer.push_back(contours[largest_contour_index][defects[i][2]]);
        }
    }

    double Mindistance = 30;

    if (defectsPointer.size() == 4) {

        Mindistance = sqrt(((defectsPointer[1].x - defectsPointer[2].x) * (defectsPointer[1].x - defectsPointer[2].x))
                           + ((defectsPointer[1].y - defectsPointer[2].y) * (defectsPointer[1].y - defectsPointer[2].y)));

    }

    if (devideNum == 0) {

        devideNum = 1;

    }


    Point center = Point((int)(tryx/devideNum),(int)(tryy/devideNum));
    // circle(img_input, center, 5, Scalar(255, 255, 0), 5);

    vector <int> startPoint;
    vector <int> endPoint;
    int rightest_defects = 0;
    int rightest_defectsindex = 0;

    for (int i = 0; i < hull.size(); i++) {

        if (rightest_defects < hull[i].x) {

            rightest_defects = hull[i].x;
            rightest_defectsindex = i;

        }

    }

    for (int j = 0; j < hull.size() - 1; j++ ) {
        if (startPoint.size() < 6) {

            double distance = sqrt(((hull[j].x - hull[j + 1].x) * (hull[j].x - hull[j + 1].x))
                                   + ((hull[j].y - hull[j + 1].y) * (hull[j].y - hull[j + 1].y)));

            if (distance > (Mindistance * 1.2) && hull[rightest_defectsindex].y > hull[j].y && defectsPointer.size() > 3) {
                //&& TestPoint.size() < 10


                // Point temp = Point(hull[j].x, hull[j].y);
                startPoint.push_back(hull[j].x);
                startPoint.push_back(hull[j].y);
                // circle(img_input, temp, 5, Scalar(255, 0, 0), 5);
            }

        }
        else break;
    }

    //if(defectsPointer.size() >= 3) {
    //    int finger_distance = (int) sqrt(((defectsPointer[1].x - defectsPointer[2].x)
    //                                      * (defectsPointer[1].x - defectsPointer[2].x))
    //                                     + ((defectsPointer[1].y - defectsPointer[2].y)
    //                                        * (defectsPointer[1].y - defectsPointer[2].y)));

    //    TestPoint.push_back(finger_distance);
    //}

    for (int j = hull.size() - 1 ; j > 0; j-- ) {
        if (endPoint.size() < 6) {

            double distance = sqrt(((hull[j].x - hull[j - 1].x) * (hull[j].x - hull[j -  1].x))
                                   + ((hull[j].y - hull[j - 1].y) * (hull[j].y - hull[j  - 1].y)));

            if (distance > (Mindistance * 1.2) && hull[rightest_defectsindex].y > hull[j].y && defectsPointer.size() > 3) {
                //&& TestPoint.size() < 10

                //    Point temp = Point(hull[j].x, hull[j].y);
                endPoint.push_back(hull[j].x);
                endPoint.push_back(hull[j].y);
                // circle(img_input, temp, 5, Scalar(0, 255, 0), 5);
            }

        }
        else break;
    }




    vector <int> TestPoint;
    if(startPoint.size() == 6 && endPoint.size() == 6) {
        int midfingerX = (startPoint[4] + endPoint[4]) / 2;
        int midfingerY = (startPoint[5] + endPoint[5]) / 2;

        TestPoint.push_back(startPoint[0]);
        TestPoint.push_back(startPoint[1]);
        TestPoint.push_back(startPoint[2]);
        TestPoint.push_back(startPoint[3]);

        TestPoint.push_back(midfingerX);
        TestPoint.push_back(midfingerY);

        TestPoint.push_back(endPoint[2]);
        TestPoint.push_back(endPoint[3]);
        TestPoint.push_back(endPoint[0]);
        TestPoint.push_back(endPoint[1]);


        jintArray ji_array = NULL;
        ji_array = env->NewIntArray(TestPoint.size());
        jint *int_buf = (jint *) malloc(sizeof(jint) * TestPoint.size());

        for (int i = 0; i < TestPoint.size(); i++) {
            int_buf[i] = TestPoint[i];
        }

        env->SetIntArrayRegion(ji_array, 0, TestPoint.size(), (const jint *) int_buf);


        free(int_buf);

        img_input.copyTo(img_output);

        return ji_array;
    }

    else{

        jintArray ji_array = NULL;
        ji_array = env->NewIntArray(1);
        jint *int_buf = (jint *) malloc(sizeof(jint) * 1);
        int_buf[0] = 44;
        return  ji_array;

    }
}
}