require 'socket'
include Socket::Constants

class String
  def numeric?
    !Float(self).nil?
  rescue StandardError
    false
  end
end

def send(socket, msg)
  socket.send(msg + "~@~", 0)
end

begin
  print "Hello, client is starting\n"

  if ARGV.length < 3
    warn "No server`s ip, port number and nik in args\n"
    exit 1
  end

  if (ARGV[0] =~ /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/).nil?
    warn "Server`s ip is not correct\n"
    exit 1
  end

  unless ARGV[1].numeric?
    warn "Port is not number\n"
    exit 1
  end

  @work = true
  nik = ARGV[2]

  @socket = Socket.new(AF_INET, SOCK_STREAM, 0)
  sockaddr = Socket.pack_sockaddr_in(ARGV[1].to_i, ARGV[0])
  @socket.connect(sockaddr)
  output = Thread.new(nik) {
    while @work do
      text = STDIN.gets.chomp
      send @socket, "[#{Time.now}] <#{_1}> #{text}"
    end
  }
  input = Thread.new {
    while @work do
      text = ''
      text << @socket.recvfrom(256)[0].chomp until text.end_with? "~@~"
      print ">" + text[0..-4] + "\n"
      exit if text == "Server is out. Please reconnect~@~"
    end
  }

  def shutdown(input, output)
    @work = false
    input.exit
    output.exit
    @socket.close
    exit 0
  end

  Signal.trap('INT') do
    shutdown(input, output)
  end

  input.join
  shutdown input, output
end