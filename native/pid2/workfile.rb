#!/usr/bin/ruby

require File.expand_path("#{ENV['MOSYNC_TRUNK']}/rules/dll.rb")
work = DllWork.new
work.instance_eval do 
	@SOURCES = ['.']
	@EXTRA_CPPFLAGS = ""
	@NAME = "pid2"
end

work.invoke
