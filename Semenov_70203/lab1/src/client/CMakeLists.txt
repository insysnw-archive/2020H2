cmake_minimum_required(VERSION 3.5)

find_package(Qt5 COMPONENTS Widgets Network Core REQUIRED)

include_directories(${CMAKE_CURRENT_BINARY_DIR})

qt_wrap_ui(client_ui
    ${PROJECT_SOURCE_DIR}/ui/client.ui
    ${PROJECT_SOURCE_DIR}/ui/message_widget.ui
    ${PROJECT_SOURCE_DIR}/ui/name_dialog.ui)

qt_wrap_cpp(client_moc
    ${PROJECT_SOURCE_DIR}/include/client/client.h
    ${PROJECT_SOURCE_DIR}/include/client/event_listener_adapter.h
    ${PROJECT_SOURCE_DIR}/include/client/message_widget.h
    ${PROJECT_SOURCE_DIR}/include/client/name_dialog.h)

add_executable(client ${client_moc} ${client_ui}
    ${PROJECT_SOURCE_DIR}/src/client/client.cpp
    ${PROJECT_SOURCE_DIR}/src/client/client_main.cpp
    ${PROJECT_SOURCE_DIR}/src/client/event_listener_adapter.cpp
    ${PROJECT_SOURCE_DIR}/src/client/message_widget.cpp
    ${PROJECT_SOURCE_DIR}/src/client/name_dialog.cpp)

target_link_libraries(client PRIVATE
    chatlibs Qt5::Widgets Qt5::Network Qt5::Core)
