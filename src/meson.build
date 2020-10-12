sources = files([
  'client.cpp',
  'client_shared.cpp',
  'client_sync.cpp',
  'connection.cpp',
  'connection_sync.cpp',
  'gate.cpp',
  'protocol.cpp',
  'server.cpp',
  'server_sync.cpp',
  'settings.cpp',
  'tunnel.cpp'
])

src = include_directories('.')

sources += configure_file(output : 'config.cpp', input : 'config.cpp.in',
  configuration : {
  'group' : group,
  'project' : meson.project_name(),
  'version' : meson.project_version(),
  'build' : run_command('git', 'describe').stdout().strip(),
  'maintainer' : maintainer,
})

lib = library(meson.project_name(), [sources, res_header, res_source], dependencies: module_deps)
exe_server = executable(meson.project_name() + '-server', ['server_main.cpp', res_header], link_with : lib, install : true, dependencies : module_deps)
exe_client = executable(meson.project_name() + '-client', ['client_main.cpp', res_header], link_with : lib, install : true, dependencies : module_deps)
run_target('run', command : [exe_server] + get_option('run_args'), depends : exe_server)
