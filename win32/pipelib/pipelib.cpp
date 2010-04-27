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

// pipelib.cpp : Defines the exported functions for the DLL application.
//

#ifdef WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <io.h>
#include <process.h>
#include <direct.h>
#define pipe _pipe
#else	// GNU libc
#include <unistd.h>
#include <spawn.h>
#include <sys/wait.h>
#endif

#include <stdio.h>
#include <fcntl.h>

#include "pipelib.h"

PIPELIB_API int pipe_create(int* fds)
{
#ifdef WIN32
	return _pipe(fds, 512, O_BINARY/* | O_NOINHERIT*/);
#else
	return pipe(fds);
#endif
}

PIPELIB_API int pipe_read(int fd, void* dstbuf, int maxchars) {
	return read(fd, dstbuf, maxchars);
}

PIPELIB_API int pipe_write(int fd, void* srcbuf, int length) {
	return write(fd, srcbuf, length);
}

PIPELIB_API int pipe_dup(int fd) {
	return dup(fd);
}

PIPELIB_API int pipe_dup2(int fd1, int fd2) {
	return dup2(fd1, fd2);
}

PIPELIB_API int pipe_close(int fd) {
	return close(fd);
}

PIPELIB_API int proc_spawn(char* cmd, char* args, char* dir) {
	if(chdir(dir) != 0)
		return -1;
#ifdef WIN32
	return (int)spawnl(P_NOWAIT, cmd, args, NULL);
#else
	pid_t pid;
	char* argv[2] = {args, NULL};
	int res = posix_spawn(&pid, cmd, NULL, NULL, argv, NULL);
	if(res < 0)
		return res;
	return pid;
#endif
}

PIPELIB_API int proc_wait_for(int handle) {
	int err_code;
#ifdef WIN32
	_cwait(&err_code, handle, NULL);
#else
	int res = waitpid(handle, &err_code, 0);
	if(res < 0)
		return res;
#endif
	return err_code;
}

PIPELIB_API int proc_kill(int pid, int exit_code) {
#ifdef WIN32
	HANDLE handle = OpenProcess(PROCESS_QUERY_INFORMATION |
           PROCESS_VM_READ | PROCESS_TERMINATE, FALSE, pid);
	
	int gotHandle = 0;

	if (handle != NULL) {
		gotHandle = 0xf0000;
		int result = TerminateProcess(handle, exit_code);
		CloseHandle(handle);
		if (result) {
			return 0;
		}
	}

	int lastError = GetLastError();
	if (lastError = ERROR_NOACCESS) {
		lastError = -1;
	}

	return lastError | gotHandle;
#else
	// exit_code is ignored.
	int res = kill(pid, SIGTERM);
	if(res < 0)
		return res;
	return 0xf0000;	// magic number?
#endif
}
