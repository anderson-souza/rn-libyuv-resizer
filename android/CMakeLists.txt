project(libyuvresizer)
cmake_minimum_required(VERSION 3.9.0)

set(PACKAGE_NAME "libyuvresizer")
set(CMAKE_CXX_STANDARD 17)

find_library(LOG_LIB log)

# libyuv
add_subdirectory(../libyuv libyuv)

add_library(
    ${PACKAGE_NAME}
    SHARED
    src/main/cpp/LibyuvResizerModule.cpp
)

target_include_directories(
    ${PACKAGE_NAME} PRIVATE
    src/main/cpp
    ../libyuv/include
)

target_link_libraries(
    ${PACKAGE_NAME}
    ${LOG_LIB}
    android
    jnigraphics
    yuv
)
