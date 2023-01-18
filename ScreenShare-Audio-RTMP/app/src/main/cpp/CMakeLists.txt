cmake_minimum_required(VERSION 3.18.1)
project("rtmp-sender")

file(GLOB native_source *.cpp)
add_subdirectory(librtmp)

add_library(
        rtmp-sender
        SHARED
        ${native_source}
)
#系统目录  系统环境变量     log-lib 变量
find_library(
        log-lib
        log)
target_link_libraries(
        rtmp-sender
        ${log-lib}
        rtmp111)