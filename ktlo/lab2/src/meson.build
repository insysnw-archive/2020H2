common_headers = include_directories('.')

common_sources = files([
  'codec.cpp'
])

common_sources += configure_file(output : 'config.cpp', input : 'config.cpp.in',
  configuration : {
  'group' : group,
  'project' : meson.project_name(),
  'version' : meson.project_version(),
  'build' : run_command('git', 'describe').stdout().strip(),
  'maintainer' : maintainer,
})

subdir('dns')
subdir('ntp')
