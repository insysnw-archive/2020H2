dns_sources = files([
  'arguments.cpp',
  'namez.cpp',
  'record.cpp',
  'records/A.cpp',
  'records/NS.cpp',
  'records/CNAME.cpp',
  'records/SOA.cpp',
  'records/PTR.cpp',
  'records/MX.cpp',
  'records/TXT.cpp',
  'records/AAAA.cpp',
  'records/SRV.cpp',
  'records/OPT.cpp',
  'dns_enum.cpp',
  'packet.cpp',
  'dnscodec.cpp',
  'database.cpp',
  'zones.cpp',
  'question.cpp',
  'answer.cpp',
  'base64.cpp',
  'client.cpp'
])

dns_sources += common_sources

dns_src = include_directories('.')

dns_lib = static_library(meson.project_name(), dns_sources, dependencies: module_deps, include_directories: common_headers)

dns_server = executable(
  meson.project_name() + '-dns-server',
  'server.cpp',
  link_with : dns_lib,
  include_directories: common_headers,
  install : true,
  dependencies : module_deps
)
