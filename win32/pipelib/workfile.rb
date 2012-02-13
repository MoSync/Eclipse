#!/usr/bin/ruby

# This build file is NOT part of the full build, but need to
# invoked manually. Please modify the rules/native_link.rb file
# and make sure the @EXTRA_LINKFLAGS for Mac is set to -m64
# instead of -m32. This modification should be local and then
# reverted before checkin.

require File.expand_path("#{ENV['MOSYNC_TRUNK']}/rules/dll.rb")
work = DllWork.new
work.instance_eval do 
	@SOURCES = ['.']
	@EXTRA_CPPFLAGS = " -arch x86_64 -DPIPELIB_EXPORTS"
	@NAME = "pipelib"
end

work.invoke
