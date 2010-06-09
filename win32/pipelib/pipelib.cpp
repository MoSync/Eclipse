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
#define _WIN32_WINNT 0x0501
#include <windows.h>
#include <io.h>
#include <process.h>
#include <direct.h>

#define pipe _pipe
#else	// GNU libc
#include <stdlib.h>
#include <signal.h>
#include <unistd.h>
#include <spawn.h>
#include <sys/wait.h>
#include <string.h>
#endif

#include <stdio.h>
#include <fcntl.h>

#include "pipelib.h"

/* In OS X and Linux we need to explicitly pass the environment
 * to the spawned process. Usually it is linked in with ld, but
 * not in the case of shared libraries, and in OS X you have to use 
 * the trick below to find the address of the environment array. On
 * Linux it can however be accessed through unistd.h.
 */
#ifdef DARWIN
#include <crt_externs.h>
#define environ (*_NSGetEnviron())
#endif

#ifdef WIN32
/**
 * Function used to destroy all windows of the specified process.
 *
 * @param hwnd handle to top-level window.
 * @param pid Pid of process to stop.
 */
static BOOL CALLBACK
terminateWindows(HWND hwnd, LPARAM pid);

static BOOL CALLBACK
terminateWindows(HWND hwnd, LPARAM pid)
{
	DWORD id;

	GetWindowThreadProcessId(hwnd, &id);

	if(id == (DWORD) pid)
	{
		PostMessage(hwnd, WM_CLOSE, 0, 0);
	}

	return TRUE;
}
#endif

#ifndef WIN32

#define BUFFER_SIZE 4096
#define MAX_ARGV_SIZE 4096

/**
 * Frees the allocated argument strings, should
 * be called after a call to parseCmdLine to 
 * release the memory.
 *
 * @param a An array of pointers previously allocated
 *          parseCmdLine.
 */
static void 
parseFreeMem ( char * a[] )
{
	while ( *a != NULL ) 
	{
		free( *a++ );
	}
}

/**
 * Copies the given string to a string that is allocted by
 * this function and the returned.
 * 
 * Note: The user is reponsible for freeing the memory
 *       allocated by this function.
 *
 * @param s The string that should be copied.
 * @return Returns a copy of the given string, or NULL
 *         if no memory was availible.
 */
static char *
parseStrCopyN ( char *s )
{
	size_t len = strlen( s );
	char * res = (char *) malloc( len+1 );
	
	if (res == NULL)
	{
		return NULL;
	}
	
	strcpy( res, s );
	return res;
}

/**
 * Parses a command string and converts it to an argv-style
 * array. Memory for the argv-array is allocated within this
 * function and should be relased by a call to parseFreeMem.
 *
 * @param cmd Command string.
 * @return The command string converted to an argv array.
 */
char **
parseCmdLineN ( const char *cmd )
{
	int i = 0;
	char *tok;
	static char tmpStr[BUFFER_SIZE];
	static char tmpCmd[BUFFER_SIZE];
	static char *tmpArr[MAX_ARGV_SIZE];
	
	strcpy( tmpCmd, cmd );
	tok = strtok( tmpCmd, " \t\f\n" );
	while ( tok != NULL && i < MAX_ARGV_SIZE )
	{
		/* Copy first token */
		strcpy( tmpStr, tok );
		
		/* Is there a quotation mark? */
		if ( tok[0] == '\"' )
		{
			tok = strtok( NULL, "\"" );
			strcat( tmpStr, " " );
			if ( tok != NULL )
			{
				strcat( tmpStr, tok );
			}
			strcat( tmpStr, "\"" );
		}
		
		/* Copy argument */
		char *copiedString = parseStrCopyN( tmpStr );
		if(copiedString == NULL)
		{
			return NULL;
		}
		
		tmpArr[i++] = copiedString;
		tok = strtok( NULL, " \t\f\n" );
	}
	
	tmpArr[i] = NULL;
	return tmpArr;
}

#endif

PIPELIB_API int pipe_create(int* fds)
{
#ifdef WIN32
	return _pipe(fds, 512, O_BINARY | O_NOINHERIT);
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

PIPELIB_API int proc_spawn(char* cmd, char* args, char* dir) 
{
	if(chdir(dir) != 0)
	{
		return -1;
	}
#ifdef WIN32
	char *argv[2] = {args, NULL};
	HANDLE handle = (HANDLE) _spawnvp(_P_NOWAIT, cmd, argv);
	return GetProcessId(handle);
#else
	pid_t pid = 0;
	char **argv = parseCmdLineN( args );
	int res = posix_spawnp(&pid, cmd, NULL, NULL, argv, environ);
	parseFreeMem( argv );
	
	if(res < 0) {
		return res;
	}
	return pid;
#endif
}

PIPELIB_API int proc_wait_for(int handle) {
#ifdef WIN32
	HANDLE procHandle = OpenProcess(SYNCHRONIZE, FALSE, handle);
	if(procHandle == NULL) {
		return -1;
	}
	WaitForSingleObject(procHandle, INFINITE);
	CloseHandle(procHandle);
#else
	int status;
	int res = waitpid(handle, &status, 0);
	if(res < 0 || (!WIFEXITED(status) && !WIFSIGNALED(status))) {
		return -1;
	}
#endif
	return 0;
}

PIPELIB_API int proc_kill(int pid, int exit_code) {
#ifdef WIN32
	int killStatus = 0;
	HANDLE procHandle = OpenProcess(PROCESS_TERMINATE | SYNCHRONIZE, FALSE, pid);
	if(procHandle == NULL)
	{
		return -1;
	}

	/* Post WM_CLOSE to all windows owned by the process */
	EnumWindows((WNDENUMPROC) terminateWindows, (LPARAM) pid);

	/* Wait for process to respond and kill it */
	if(WaitForSingleObject(procHandle, 500) != WAIT_OBJECT_0)
	{
		int res = TerminateProcess(procHandle, exit_code);
		if(res == 0)
		{
			killStatus = -2;
		}
	}

	/* Close handle */
	CloseHandle(procHandle);

	return killStatus;
#else
	// exit_code is ignored.
	return kill(pid, SIGTERM);
#endif
}
