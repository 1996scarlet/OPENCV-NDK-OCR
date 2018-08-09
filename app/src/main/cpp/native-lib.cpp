#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>

#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

using namespace cv;
using namespace std;

extern "C" JNIEXPORT jstring

JNICALL
Java_ac_ict_humanmotion_ocr_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void
JNICALL
Java_ac_ict_humanmotion_ocr_MainActivity_selfEdge(
        JNIEnv *env,
        jobject obj,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
              info.format == ANDROID_BITMAP_FORMAT_RGB_565);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat temp;

    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888)
        temp = Mat(info.height, info.width, CV_8UC4, pixels);
    else temp = Mat(info.height, info.width, CV_8UC2, pixels);

    Mat gray;
    cvtColor(temp, gray, COLOR_RGBA2GRAY);

    //1.Sobel算子，x方向求梯度
    Mat sobel;
    Sobel(gray, sobel, CV_8U, 1, 0, 3);

    //2.二值化
    Mat binary;
    threshold(sobel, binary, 0, 255, THRESH_OTSU + THRESH_BINARY);

    //3.膨胀和腐蚀操作核设定
    Mat element1 = getStructuringElement(MORPH_RECT, Size(30, 9));
    //控制高度设置可以控制上下行的膨胀程度，例如3比4的区分能力更强,但也会造成漏检
    Mat element2 = getStructuringElement(MORPH_RECT, Size(24, 3));

    //4.膨胀一次，让轮廓突出
    Mat dilate1;
    dilate(binary, dilate1, element2);

    dilate(dilate1, dilate1, element2);
    dilate(dilate1, dilate1, element2);

    //5.腐蚀一次，去掉细节，表格线等。这里去掉的是竖直的线
    Mat erode1;
    erode(dilate1, erode1, element1);

    //6.再次膨胀，让轮廓明显一些
    Mat dilate2;
    dilate(erode1, dilate2, element2);

    vector<RotatedRect> rects;

    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;

    findContours(dilate2, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE, Point(0, 0));

    for (int i = 0; i < contours.size(); i++) {
        //计算当前轮廓的面积
        double area = contourArea(contours[i]);

        //面积小于1000的全部筛选掉
        if (area < 1000)
            continue;

        //轮廓近似，作用较小，approxPolyDP函数有待研究
        double epsilon = 0.001 * arcLength(contours[i], true);
        Mat approx;
        approxPolyDP(contours[i], approx, epsilon, true);

        //找到最小矩形，该矩形可能有方向
        RotatedRect rect = minAreaRect(contours[i]);

        //计算高和宽
        int m_width = rect.boundingRect().width;
        int m_height = rect.boundingRect().height;

        //筛选那些太细的矩形，留下扁的
        if (m_height > m_width * 1.2)
            continue;

        //符合条件的rect添加到rects集合中
        rects.push_back(rect);

    }

    for (int i = 0; i < rects.size(); i++) {
        Point2f P[4];
        rects[i].points(P);
        for (int j = 0; j <= 3; j++) {
            line(temp, P[j], P[(j + 1) % 4], Scalar(0, 255, 0), 2);
        }
    }

//    cvtColor(dilate1, temp, COLOR_GRAY2RGBA);
    AndroidBitmap_unlockPixels(env, bitmap);
}


extern "C" JNIEXPORT void
JNICALL
Java_ac_ict_humanmotion_ocr_MainActivity_selfBinary(
        JNIEnv *env,
        jobject obj,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
              info.format == ANDROID_BITMAP_FORMAT_RGB_565);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat temp;

    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888)
        temp = Mat(info.height, info.width, CV_8UC4, pixels);
    else temp = Mat(info.height, info.width, CV_8UC2, pixels);

    Mat gray;
    cvtColor(temp, gray, COLOR_RGBA2GRAY);

    Mat sobel;
    Sobel(gray, sobel, CV_8U, 1, 0, 3);

    Mat binary;
    threshold(sobel, binary, 0, 255, THRESH_OTSU + THRESH_BINARY);

    cvtColor(binary, temp, COLOR_GRAY2RGBA);
    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT void
JNICALL
Java_ac_ict_humanmotion_ocr_MainActivity_selfDilate(
        JNIEnv *env,
        jobject obj,
        jobject bitmap,
        jint p1,
        jint p2) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat temp(info.height, info.width, CV_8UC4, pixels);

    Mat gray;
    cvtColor(temp, gray, COLOR_RGBA2GRAY);

    Mat m_dilate;
    dilate(gray, m_dilate, getStructuringElement(MORPH_RECT, Size(p1, p2)));

    cvtColor(m_dilate, temp, COLOR_GRAY2RGBA);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT void
JNICALL
Java_ac_ict_humanmotion_ocr_MainActivity_selfErode(
        JNIEnv *env,
        jobject obj,
        jobject bitmap,
        jint p1,
        jint p2) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat temp(info.height, info.width, CV_8UC4, pixels);

    Mat gray;
    cvtColor(temp, gray, COLOR_RGBA2GRAY);

    Mat m_erode;
    erode(gray, m_erode, getStructuringElement(MORPH_RECT, Size(p1, p2)));

    cvtColor(m_erode, temp, COLOR_GRAY2RGBA);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT void
JNICALL
Java_ac_ict_humanmotion_ocr_MainActivity_selfRect(
        JNIEnv *env,
        jobject obj,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat temp(info.height, info.width, CV_8UC4, pixels);

    Mat gray;
    cvtColor(temp, gray, COLOR_RGBA2GRAY);

    vector<RotatedRect> rects;

    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;

    findContours(gray, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE, Point(0, 0));

    for (int i = 0; i < contours.size(); i++) {

        double area = contourArea(contours[i]);

        if (area < 1000)
            continue;

        double epsilon = 0.001 * arcLength(contours[i], true);
        Mat approx;
        approxPolyDP(contours[i], approx, epsilon, true);

        RotatedRect rect = minAreaRect(contours[i]);

        int m_width = rect.boundingRect().width;
        int m_height = rect.boundingRect().height;

        if (m_height > m_width * 1.2)
            continue;

        rects.push_back(rect);

    }

    for (int i = 0; i < rects.size(); i++) {
        Point2f P[4];

        RotatedRect rotated = rects[i];

        rotated.points(P);

        for (int j = 0; j <= 3; j++) {
            line(temp, P[j], P[(j + 1) % 4], Scalar(0, 255, 0), 2);
        }

//        Rect tto = rotated.boundingRect();
//        Mat cto = temp(tto);

        vector<int> compression_params;
        compression_params.push_back(CV_IMWRITE_JPEG_QUALITY);
        compression_params.push_back(100);

        if (i == 4){
            // imwrite("/storage/emulated/0/tessdata/05.jpg", temp(rotated.boundingRect()), compression_params);
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}