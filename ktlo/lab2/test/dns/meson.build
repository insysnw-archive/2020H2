dns_test_names = [
  'namez',
  'packet',
  'db',
  'base64'
]

dns_test_files = []

foreach test_name : dns_test_names
  dns_test_files += files(test_name + '.cpp')
  test_exe = executable(
    test_name + '.test', dns_test_files[-1],
    link_with : dns_lib,
    include_directories : [dns_src, common_headers, test_headers],
    dependencies : module_deps
  )
  test('dns-' + test_name, test_exe, suite : 'regular')
endforeach
