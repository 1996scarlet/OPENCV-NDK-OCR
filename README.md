# OPENCV-NDK-OCR
* Android ocr with OpenCV and tess-two
* This project is a dead-simple which described how to build an easy offline OCR system.

## Total Process Diagram
This diagram is a dead-simple which described how to build an easy motion identification system.
![Diagram](imgs/diagram.png)

## Effect Comparison
* In first step, we use morphological edge detection function to drop text rects in source image.
* And then, we use tess-two to identify Chinese characters and print them.
<img src="images/RECT_RESULT.jpg" width="50%" height="50%">
<img src="images/OCR_RESULT.png" width="50%" height="50%">

## How to use tess-two in Android
Add `implementation 'com.rmtheis:tess-two:9.0.0` in your app module gradle, and use tess-two like this.

      val tessBaseAPI = TessBaseAPI()
      tessBaseAPI.init("/storage/emulated/0/", "chi_sim")
      tessBaseAPI.setImage(src)
      println(tessBaseAPI.utF8Text)
      tessBaseAPI.end()

## How to use OpenCV(Native & JAVA) in Android with Android Studio
Follow [this link](http://dkhoa.me/post/opencv_android_studio_ndk/) to build your app with OpenCV(NDK & Java).

## Use Native Function

## Use Java Function

## Crop Mat with RotatedRect

## Cautions
> 0.   Please give this app bluetooth permission
> 1.   You may need to pair Lpms-B before open the app

## Response Data Type
* In Android source code, we use Kotlin `MutableList<LpmsData>` to save temp result.
* When the HTTP request is going to launch, we use `GSON.toJson()` function to convert temp result to formatted string.
* If you don't know how to parse JSON or unfamiliar with it, please check [JSON.org](http://www.json.org/).

## Project Dependences
The project dependences are as follows.
* [OKHTTP3](https://github.com/square/okhttp) to replace Android default HTTP client.
* [RxJava2](https://github.com/ReactiveX/RxJava) to make HTTP request and response async.
* [Retrofit2](https://github.com/square/retrofit) to build local HTTP function interface.
* [GraphView](https://github.com/jjoe64/GraphView) to show real-time data in chart.

## Usage
> 0.   Android Studio 3.2 or latest
> 1.   Kotlin 1.2.51 or latest
> 2.   Android SDK 28 or latest
> 3.   You Android phone kernal version required 21 or above
> 4.   Android Jetpack 1.0.0-beta3 or latest
