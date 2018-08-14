cmake_minimum_required(VERSION 3.4.1)

include_directories(/home/humanmotion/Downloads/opencv-3.4.1-android-sdk/OpenCV-android-sdk/sdk/native/jni/include)
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION /home/humanmotion/Downloads/opencv-3.4.1-android-sdk/OpenCV-android-sdk/sdk/native/libs/${ANDROID_ABI}/libopencv_java3.so)

add_library( native-lib
             SHARED
             src/main/cpp/native-lib.cpp )

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec  PROPERTIES IMPORTED_LOCATION
../../../../libs/${ANDROID_ABI}/libavcodec.so)

add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter  PROPERTIES IMPORTED_LOCATION
../../../../libs/${ANDROID_ABI}/libavfilter.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(avformat  PROPERTIES IMPORTED_LOCATION
../../../../libs/${ANDROID_ABI}/libavformat.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil  PROPERTIES IMPORTED_LOCATION
../../../../libs/${ANDROID_ABI}/libavutil.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(swresample  PROPERTIES IMPORTED_LOCATION
../../../../libs/${ANDROID_ABI}/libswresample.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(swscale  PROPERTIES IMPORTED_LOCATION
../../../../libs/${ANDROID_ABI}/libswscale.so)

find_library( log-lib
              log )

include_directories( libs/include )

target_link_libraries( native-lib

                       avcodec
                       avfilter
                       avformat
                       avutil
                       swresample
                       swscale

                       lib_opencv
                       jnigraphics
                       ${log-lib} )