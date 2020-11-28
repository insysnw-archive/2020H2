cmake_minimum_required(VERSION 3.0)

find_package(Threads REQUIRED)

add_executable(
  dhcp_server
  ${PROJECT_SOURCE_DIR}/src/client.cpp
  ${PROJECT_SOURCE_DIR}/src/common.cpp
  ${PROJECT_SOURCE_DIR}/src/config.cpp
  ${PROJECT_SOURCE_DIR}/src/dhcp_packet.cpp
  ${PROJECT_SOURCE_DIR}/src/dhcp_server.cpp
  ${PROJECT_SOURCE_DIR}/src/dhcp_server_main.cpp
  ${PROJECT_SOURCE_DIR}/src/ip_allocator.cpp
  ${PROJECT_SOURCE_DIR}/src/ip_type.cpp
  ${PROJECT_SOURCE_DIR}/src/lease.cpp
  ${PROJECT_SOURCE_DIR}/src/net_int.cpp
  ${PROJECT_SOURCE_DIR}/src/range.cpp
  ${PROJECT_SOURCE_DIR}/src/timer.cpp)

target_include_directories(dhcp_server PUBLIC ${PROJECT_SOURCE_DIR}/include)
target_link_libraries(dhcp_server PUBLIC rt ${CMAKE_THREAD_LIBS_INIT})
