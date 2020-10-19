project('net-lab2', 'cpp',
  version : run_command('git', 'describe', '--abbrev=0', '--tags').stdout().strip(),
  default_options : ['warning_level=3',
                     'cpp_std=c++17'])

group = 'ktlo.net.lab2'
maintainer = 'ktlo <ktlo@handtruth.com>'

modules = [
  'ekutils'
]

######################################

add_global_arguments('-Wno-gnu-anonymous-struct', language : 'cpp')
add_global_arguments('-Wno-nested-anon-types', language : 'cpp')

module_deps = []

foreach module : modules
  module_deps += dependency(module, fallback : [module, 'dep'])
endforeach

cmake = import('cmake')
yamlcpp_proj = cmake.subproject('yaml-cpp', cmake_options : [
  '-DYAML_CPP_BUILD_TESTS=OFF'
])
ipv6_parse_proj = cmake.subproject('ipv6-parse')
module_deps += yamlcpp_proj.dependency('yaml-cpp')
module_deps += ipv6_parse_proj.dependency('ipv6-parse')

subdir('src')
subdir('test')

sources = dns_sources
test_files = dns_test_files

cppcheck = custom_target(meson.project_name() + '_cppcheck_internal',
  output : meson.project_name() + '_cppcheck.log',
  input : sources + test_files,
  command : [
    'cppcheck',
    '--enable=all',
    '-I', meson.current_source_dir() / 'include',
    '-I', meson.current_source_dir() / 'src',
    '@INPUT@',
#   '--project=compile_commands.json',
    '--output-file=@OUTPUT@'
])

run_target('cppcheck', command : ['cat', cppcheck.full_path()], depends : cppcheck)
