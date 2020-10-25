cmake_minimum_required(VERSION 3.16)
project(NetLab1)

set(CMAKE_CXX_STANDARD 17)

set(SERVER_SRC src/server/Server.h src/server/Server.cpp src/server/main.cpp)

set(CLIENT_SRC src/client/Client.h src/client/Client.cpp src/client/main.cpp)

set(COMMON_SRC
        src/common/PacketHeaders.h
        src/common/ConnectionRequest.h src/common/ConnectionRequest.cpp
        src/common/ConnectionResponse.h src/common/ConnectionResponse.cpp
        src/common/ChatMessage.h src/common/ChatMessage.cpp
        src/common/DisconnectionRequest.cpp src/common/DisconnectionRequest.h)

add_executable(lab1_server ${SERVER_SRC} ${COMMON_SRC})
if(WIN32)
    target_link_libraries(lab1_server wsock32 ws2_32)
else()
    target_link_libraries(lab1_server pthread)
endif()

add_executable(lab1_client ${CLIENT_SRC} ${COMMON_SRC})
if(WIN32)
    target_link_libraries(lab1_client wsock32 ws2_32)
else()
    target_link_libraries(lab1_client pthread)
endif()