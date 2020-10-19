ntp_sources = files([
  'packet.cpp',
  'md5.cpp'
])

ntp_sources += common_sources

ntp_src = include_directories('.')

ntp_lib = static_library(meson.project_name(), ntp_sources, dependencies: module_deps, include_directories: common_headers)

ntp_client = executable(
  meson.project_name() + '-ntp-client',
  'client.cpp',
  link_with : ntp_lib,
  include_directories: common_headers,
  install : true,
  dependencies : module_deps
)
