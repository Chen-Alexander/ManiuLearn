cmake_minimum_required(VERSION 3.4.1)

file(GLOB SRC_FILES
        "libfaac/*.c"
        )

add_library(fdk-aac-lib SHARED ${SRC_FILES})
include_directories(./include)
add_definitions(
        -DPACKAGE_VERSION="1.29.9.2"
)

find_library(log-lib log)

target_link_libraries(fdk-aac-lib ${log-lib})