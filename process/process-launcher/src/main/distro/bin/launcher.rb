#!/usr/bin/env ruby
#
# Copyright 2010 Proofpoint, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if RUBY_VERSION < "1.9" || RUBY_ENGINE != "ruby" then
  puts "MRI Ruby 1.9+ is required. Current version is #{RUBY_VERSION} [#{RUBY_PLATFORM}]"
  exit 99
end

=begin

Options
* start (run as daemon)
* stop (stop gracefully)
* restart (restart gracefully)
* kill (hard stop)
* status (check status of daemon)

Custom commands (for dev & debugging convenience)
* run (run in foreground)

Expects config under "etc":
  jvm.config
  config.properties

--config to override config file
--jvm-config to override jvm config file

Logs to var/log/launcher.log when run as daemon
Logs to console when run in foreground, unless log file provided

Libs must be installed under "lib"

Requires java & ruby to be in PATH

=end

require 'fileutils'
require 'optparse'
require 'pathname'
require 'pp'

# loads lines and strips comments
def load_lines(file)
  File.open(file, 'r') do |f|
    f.readlines.
            map { |line| line.strip }.
            select { |line| line !~ /^(\s)*#/ }
  end
end

def load_properties(file)
  entries = load_lines(file).map do |line|
    k, v = line.split('=', 2).map(&:strip)
  end
  Hash[entries]
end

def strip(string)
  space = /(\s+)/.match(string)[1]
  string.gsub(/^#{space}/, '')
end

class Pid
  def initialize(path, options = {})
    raise "Nil path provided" if path.nil?
    @options = options
    @path = path
  end

  def save(pid)
    Pathname.new(@path).parent.mkpath
    File.open(@path, "w") { |f| f.puts(pid) }
  end

  def clear()
    File.delete(@path) if File.exists?(@path)
  end

  def alive?
    pid = get
    begin
      !pid.nil? && Process.kill(0, pid) == 1
    rescue Errno::ESRCH
      puts "Process #{pid} not running" if @options[:verbose]
      false
    rescue Errno::EPERM
      puts "Process #{pid} not visible" if @options[:verbose]
      false
    end
  end

  def get
    begin
      File.open(@path) { |f| f.read.to_i }
    rescue Errno::ENOENT
      puts "Can't find pid file #{@path}" if @options[:verbose]
    end
  end
end

class CommandError < RuntimeError
  attr_reader :code
  attr_reader :message
  def initialize(code, message)
    @code = code
    @message = message
  end
end

def escape(string)
  string = string.gsub("'", %q('\\\''))
  "'#{string}'"
end

def merge_node_properties(options)
  properties = {}
  properties = load_properties(options[:node_properties_path]) if File.exists?(options[:node_properties_path])

  options[:system_properties] = properties.merge(options[:system_properties])
  options[:data_dir] = properties['node.data-dir'] unless properties['node.data-dir'].nil?

  options
end

def build_cmd_line(options)
  install_path = Pathname.new(__FILE__).parent.parent.expand_path

  log_option = if options[:daemon]
    "'-Dlog.output-file=#{options[:log_path]}'"
  else
    ""
  end

  log_levels_option = if File.exists?(options[:log_levels_path])
    "'-Dlog.levels-file=#{options[:log_levels_path]}'"
  else
    "" # ignore if levels file does not exist. TODO: should only ignore if using default & complain if user-provided file does not exist or has issues
  end

  config_path = options[:config_path]
  raise CommandError.new(:config_missing, "Config file is missing: #{config_path}") unless File.exists?(config_path)

  jvm_config_path = options[:jvm_config_path]
  raise CommandError.new(:config_missing, "JVM config file is missing: #{jvm_config_path}") unless File.exists?(jvm_config_path)

  jvm_properties = load_lines(jvm_config_path).join(' ')

  jar_path = File.join(install_path, 'lib', 'main.jar')

  system_properties = options[:system_properties].
                         map { |k, v| "-D#{k}=#{v}" }.
                         map { |v| escape(v) }.
                         join(' ')

  # TODO: fix lack of escape handling by building an array
  command =<<-CMD
    java #{jvm_properties} #{system_properties} '-Dconfig=#{config_path}' #{log_option} #{log_levels_option} -jar '#{jar_path}'
  CMD

  puts command if options[:verbose]

  command
end

def run(options)
  exec(build_cmd_line(options),
      :chdir=>options[:data_dir]
  )
end

def start(options)
  pid_file = Pid.new(options[:pid_file])
  if pid_file.alive?
    return :success, "Already running as #{pid_file.get}"
  end

  options[:daemon] = true
  command = build_cmd_line(options)

  puts command if options[:verbose]
  pid = spawn("exec #{command}",
    :chdir => options[:data_dir],
    :out => "/dev/null",
    :err => "/dev/null"
  )
  Process.detach(pid)

  pid_file.save(pid)

  return :success, "Started as #{pid}"
end


def stop(options)
  pid_file = Pid.new(options[:pid_file])

  if !pid_file.alive?
    pid_file.clear
    return :success, "Stopped #{pid_file.get}"
  end

  pid = pid_file.get
  Process.kill(Signal.list["TERM"], pid)

  while pid_file.alive? do
    sleep 0.1
  end

  pid_file.clear

  return :success, "Stopped #{pid}"
end

def restart(options)
  code, message = stop(options)
  if code != :success then
    return code, message
  else
    start(options)
  end
end

def kill(options)
  pid_file = Pid.new(options[:pid_file])

  if !pid_file.alive?
    pid_file.clear
    return :success, "foo"
  end

  pid = pid_file.get

  Process.kill(Signal.list["KILL"], pid)

  while pid_file.alive? do
    sleep 0.1
  end

  pid_file.clear

  return :success, "Killed #{pid}"
end

def status(options)
  pid_file = Pid.new(options[:pid_file])

  if pid_file.get.nil?
    return :not_running, "Not running"
  elsif pid_file.alive?
    return :running, "Running as #{pid_file.get}"
  else
    # todo this is wrong. how do you get path from the pid_file
    return :not_running_with_pid_file, "Program is dead and pid file #{pid_file.get} exists"
  end
end

commands = [:run, :start, :stop, :restart, :kill, :status]
install_path = Pathname.new(__FILE__).parent.parent.expand_path

legacy_log_properties_file = File.join(install_path, 'etc', 'log.config')
log_properties_file = File.join(install_path, 'etc', 'log.properties')

if (!File.readable?(log_properties_file) && File.readable?(legacy_log_properties_file))
  log_properties_file = legacy_log_properties_file
  warn "Did not find a log.properties, but found a log.config instead.  log.config is deprecated, please use log.properties."
end

# initialize defaults
options = {
        :node_properties_path => File.join(install_path, 'etc', 'node.properties'),
        :jvm_config_path => File.join(install_path, 'etc', 'jvm.config'),
        :config_path => File.join(install_path, 'etc', 'config.properties'),
        :data_dir => install_path,
        :log_levels_path => log_properties_file,
        :install_path => install_path,
        :system_properties => {},
        }

option_parser = OptionParser.new(:unknown_options_action => :collect) do |opts|
  banner = <<-BANNER
    Usage: #{File.basename($0)} [options] <command>

    Commands:
      #{commands.join("\n  ")}

    Options:
  BANNER
  opts.banner = strip(banner)

  opts.on("-v", "--verbose", "Run verbosely") do |v|
    options[:verbose] = true
  end

  opts.on("--node-config FILE", "Defaults to INSTALL_PATH/etc/node.properties") do |v|
    options[:node_properties_path] = Pathname.new(v).expand_path
  end

  opts.on("--jvm-config FILE", "Defaults to INSTALL_PATH/etc/jvm.config") do |v|
    options[:jvm_config_path] = Pathname.new(v).expand_path
  end

  opts.on("--config FILE", "Defaults to INSTALL_PATH/etc/config.properties") do |v|
    options[:config_path] = Pathname.new(v).expand_path
  end

  opts.on("--data DIR", "Defaults to INSTALL_PATH") do |v|
    options[:data_dir] = Pathname.new(v).expand_path
  end

  opts.on("--pid-file FILE", "Defaults to DATA_DIR/var/run/launcher.pid") do |v|
    options[:pid_file] = Pathname.new(v).expand_path
  end

  opts.on("--log-file FILE", "Defaults to DATA_DIR/var/log/launcher.log (daemon only)") do |v|
    options[:log_path] = Pathname.new(v).expand_path
  end

  opts.on("--log-levels-file FILE", "Defaults to INSTALL_PATH/etc/log.config") do |v|
    options[:log_levels_path] = Pathname.new(v).expand_path
  end

  opts.on("-D<name>=<value>", "Sets a Java System property") do |v|
    if v.start_with?("config=") then
      raise("Config can not be passed in a -D argument.  Use --config instead")
    end
    property_key, property_value = v.split('=', 2).map(&:strip)
    options[:system_properties][property_key] = property_value
  end

  opts.on('-h', '--help', 'Display this screen') do
    puts opts
    exit 2
  end
end

option_parser.parse!(ARGV)

options = merge_node_properties(options)

if options[:log_path].nil? then
  options[:log_path] =  File.join(options[:data_dir], 'var', 'log', 'launcher.log')
end

if options[:pid_file].nil? then
  options[:pid_file] =  File.join(options[:data_dir], 'var', 'run', 'launcher.pid')
end

puts options.map { |k, v| "#{k}=#{v}"}.join("\n") if options[:verbose]

# symlink etc directory into data directory
# this is needed to support programs that reference etc/xyz from within their config files (e.g., log.levels-file=etc/log.properties)
if install_path != options[:data_dir]
  File.delete(File.join(options[:data_dir], 'etc')) rescue nil
  File.symlink(File.join(install_path, 'etc'), File.join(options[:data_dir], 'etc'))
end

status_codes = {
        :success => 0,
        :running => 0,
        :not_running_with_pid_file => 1,
        :not_running => 3
}


error_codes = {
        :generic_error => 1,
        :invalid_args => 2,
        :unsupported => 3,
        :config_missing => 6
}

if ARGV.length != 1
  puts option_parser
  puts
  puts "Expected a single command, got '#{ARGV.join(' ')}'"
  exit error_codes[:invalid_args]
end

command = ARGV[0].to_sym

unless commands.include?(command)
  puts option_parser
  puts
  puts "Unsupported command: #{command}"
  exit error_codes[:unsupported]
end

begin
  code, message = send(command, options)
  puts message unless message.nil?
  exit status_codes[code]
rescue CommandError => e
  puts e.message
  puts e.code if options[:verbose]
  exit error_codes[e.code]
end
