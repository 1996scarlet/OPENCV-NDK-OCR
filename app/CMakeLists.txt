cmake_minimum_required(VERSION 3.4.1)

include_directories(/home/humanmotion/Downloads/opencv-3.4.1-android-sdk/OpenCV-android-sdk/sdk/native/jni/include)
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION /home/humanmotion/Downloads/opencv-3.4.1-android-sdk/OpenCV-android-sdk/sdk/native/libs/${ANDROID_ABI}/libopencv_java3.so)

add_library( native-lib
             SHARED
             src/main/cpp/native-lib.cpp )

find_library( log-lib
              log )

target_link_libraries( native-lib
                       lib_opencv
                       jnigraphics
                       ${log-lib} )