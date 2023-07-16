# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.

project("opusaudio")

#CMAKE_SOURCE_DIR系统变量
message(${CMAKE_SOURCE_DIR})
message(${ANDROID_ABI})
# rtmp
include_directories(${CMAKE_SOURCE_DIR}/librtmp)
add_subdirectory(librtmp)
# opus
include_directories(${CMAKE_SOURCE_DIR}/opus)
add_subdirectory(opus)
# fdk_aac
include_directories(${CMAKE_SOURCE_DIR}/faac-1_29_9_2)
add_subdirectory(faac-1_29_9_2)
# webrtc_ns
include_directories(${CMAKE_SOURCE_DIR}/webrtc_ns)
add_subdirectory(webrtc_ns)
# ffmpeg
include_directories(${CMAKE_SOURCE_DIR}/ffmpeg/include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")
message("11111${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

file(GLOB cpp_source  *.cpp)
add_library( # Sets the name of the library.
        opusaudio

        # Sets the library as a shared library.
        SHARED

        ${cpp_source})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        opusaudio
        rtmp
        opus
        fdk-aac-lib
#        fdk-aad-lib
        legacy_ns-lib

        avcodec
#        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        android)