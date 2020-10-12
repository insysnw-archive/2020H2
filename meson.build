project('lab1-chat', 'cpp',
  version : run_command('git', 'describe', '--abbrev=0', '--tags').stdout().strip(),
  default_options : ['warning_level=3',
                     'cpp_std=c++17',
                     'build.cpp_std=c++17'])

group = 'ktlo'
maintainer = 'ktlo <ktlo@handtruth.com>'

modules = [
  'ekutils',
  'paket-cpp'
]

######################################

module_deps = []

foreach module : modules
  module_deps += dependency(module, fallback : [module, 'dep'])
endforeach

module_deps += dependency('threads')

cppres_proj = subproject('cpp-res', default_options : ['native_tool=true'])
cppres_exe = cppres_proj.get_variable('exe')

subdir('res')
subdir('src')
subdir('test')

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
