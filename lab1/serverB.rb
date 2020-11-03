require 'socket'
require 'fcntl'

@clients = []

class String
  def numeric?
    !Float(self).nil?
  rescue StandardError
    false
  end
end

if ARGV.empty? or !ARGV[0].numeric?
  warn "Invalid port number"
  exit 1
end

serv = TCPServer.new(ARGV[0].to_i)
flags = serv.fcntl(Fcntl::F_GETFL, 0)
serv.fcntl(Fcntl::F_SETFL, Fcntl::O_NONBLOCK | flags)

Signal.trap('INT') do
  @clients.each { |c|
    c.puts "Server is out. Please reconnect~@~"
    c.close
  }
  serv.close
  exit
end

loop do
  sock, addr = serv.accept_nonblock(exception: false)
  unless sock == :wait_readable
    @clients << sock
    print "New client on server\n"
  end
  if @clients.length > 0
    streams = IO.select(@clients, nil, nil, 1)
    unless streams == nil
      streams[0].each { |c|
        text = ''
        text << c.recvfrom(256)[0].chomp until text.end_with? "~@~"
        if text.index('[') != 0
          puts stderr 'Incorrect format of message'
        else
          dt = Time.at(text[1..text.index(']')].to_i)
          text = "[#{dt.strftime('%I:%M%p')}] #{text[text.index(']') + 2..-1]}"
          print text[0..-4] + "\n"
          @clients.each do |sok|
            sok.puts(text) if sok != c
          end
        end
      }
      streams[2].each { |c| @clients.delete_at(@clients.index(c)) }
    end
  end
end