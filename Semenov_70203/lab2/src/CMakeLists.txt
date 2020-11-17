cmake_minimum_required(VERSION 3.0)

add_executable(dhcp_server
    ${PROJECT_SOURCE_DIR}/src/dhcp_server.cpp
    ${PROJECT_SOURCE_DIR}/src/dhcp_package.cpp
    ${PROJECT_SOURCE_DIR}/src/dhcp_server_main.cpp)

target_include_directories(dhcp_server PUBLIC
    ${PROJECT_SOURCE_DIR}/include)

target_link_libraries(dhcp_server PUBLIC chatlibs)
