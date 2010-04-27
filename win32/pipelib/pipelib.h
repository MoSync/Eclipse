/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/

// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the PIPELIB_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// PIPELIB_API functions as being imported from a DLL, whereas this DLL sees symbols
// defined with this macro as being exported.
#ifdef WIN32
#define PIPELIB_EXPORT __declspec(dllexport)
#define PIPELIB_IMPORT __declspec(dllimport)
#else
#define PIPELIB_EXPORT
#define PIPELIB_IMPORT
#endif

#ifdef PIPELIB_EXPORTS
#define PIPELIB_API PIPELIB_EXPORT
#else
#define PIPELIB_API PIPELIB_IMPORT
#endif

extern "C" PIPELIB_API int pipe_create(int* fds);

extern "C" PIPELIB_API int pipe_read(int fd, void* dstbuf, int maxchars);

extern "C" PIPELIB_API int pipe_write(int fd, void* srcbuf, int length);

extern "C" PIPELIB_API int pipe_dup(int fd);

extern "C" PIPELIB_API int pipe_dup2(int fd1, int fd2);

extern "C" PIPELIB_API int pipe_close(int fd);

extern "C" PIPELIB_API int proc_spawn(char* cmd, char* args, char* dir);

extern "C" PIPELIB_API int proc_wait_for(int handle);

extern "C" PIPELIB_API int proc_kill(int pid, int exit_code);
