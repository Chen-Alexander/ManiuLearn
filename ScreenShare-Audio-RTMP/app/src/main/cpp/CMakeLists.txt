cmake_minimum_required(VERSION 3.18.1)
project("rtmpmaniu")

file(GLOB david *.cpp)
add_subdirectory(librtmp)

add_library(
        rtmpmaniu
        SHARED
        ${david}
)
#系统目录  系统环境变量     log-lib 变量
find_library(
        log-lib
        log)
target_link_libraries(
        rtmpmaniu
        ${log-lib}
        rtmp111)