cmake_minimum_required(VERSION 3.16)
project(NetLab2)

set(CMAKE_CXX_STANDARD 17)

set(DNS_SERVER_SRC src/dns/main.cpp)

set(DHCP_CLIENT_SRC
        src/dhcp/main.cpp
        src/dhcp/protocol/Dhcp.h src/dhcp/protocol/Dhcp.cpp
        src/dhcp/util.cpp src/dhcp/util.h
        )

set(COMMON_SRC src/common/UdpSocket.cpp src/common/UdpSocket.h)

add_executable(lab2_dns_server ${DNS_SERVER_SRC} ${COMMON_SRC})
if (WIN32)
    target_link_libraries(lab2_dns_server wsock32 ws2_32)
else ()
    target_link_libraries(lab2_dns_server pthread)
endif ()

add_executable(lab2_dhcp_client ${DHCP_CLIENT_SRC} ${COMMON_SRC})
if (WIN32)
    target_link_libraries(lab2_dhcp_client wsock32 ws2_32)
else ()
    target_link_libraries(lab2_dhcp_client pthread)
endif ()