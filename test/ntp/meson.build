ntp_test_names = [
  'md5test'
]

ntp_test_files = []

foreach test_name : ntp_test_names
  ntp_test_files += files(test_name + '.cpp')
  test_exe = executable(
    test_name + '.test', ntp_test_files[-1],
    link_with : ntp_lib,
    include_directories : [ntp_src, common_headers, test_headers],
    dependencies : module_deps
  )
  test('ntp-' + test_name, test_exe, suite : 'regular')
endforeach
