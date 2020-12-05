find_package(Threads)

if(PRINT_ERRORS)
    add_definitions(-DCHAT_LOG_ERROR=1)
endif()

if(PRINT_INFO)
    add_definitions(-DCHAT_LOG_INFO=1)
endif()

add_library(
    chatlibs STATIC
    ${PROJECT_SOURCE_DIR}/src/incoming_events_listener.cpp
    ${PROJECT_SOURCE_DIR}/src/io_operations.cpp
    ${PROJECT_SOURCE_DIR}/src/looped_thread.cpp
    ${PROJECT_SOURCE_DIR}/src/manual_control.cpp
    ${PROJECT_SOURCE_DIR}/src/message.cpp
    ${PROJECT_SOURCE_DIR}/src/utils.cpp
    ${PROJECT_SOURCE_DIR}/src/worker.cpp
    ${PROJECT_SOURCE_DIR}/src/workers_pool.cpp)

target_link_libraries(chatlibs PUBLIC ${CMAKE_THREAD_LIBS_INIT})

target_include_directories(chatlibs PUBLIC ${PROJECT_SOURCE_DIR}/include)

add_executable(server
    ${PROJECT_SOURCE_DIR}/src/connection_listener.cpp
    ${PROJECT_SOURCE_DIR}/src/server.cpp
    ${PROJECT_SOURCE_DIR}/src/server_main.cpp)

target_link_libraries(server PRIVATE chatlibs)
