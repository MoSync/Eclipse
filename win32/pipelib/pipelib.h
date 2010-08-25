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
#define PIPELIB_EXPORT __attribute__((visibility("default")))
#define PIPELIB_IMPORT __attribute__((visibility("default")))
#endif

#ifdef PIPELIB_EXPORTS
#define PIPELIB_API PIPELIB_EXPORT
#else
#define PIPELIB_API PIPELIB_IMPORT
#endif

/**
 * Creates a pipe.
 *
 * @param fds An array that will contain the read and write file descriptors
 *            of the pipe upon return. It must be able to contain two ints,
 *            the first element will be the read descriptor, and the second
 *            the write descriptor.
 * @return 0 on success, otherwise a negative number is returned.
 */
extern "C" PIPELIB_API int pipe_create(int* fds);

/**
 * Reads from a pipe with the given file descriptor.
 *
 * @param fd The file descriptor that specifies the read end of the pipe.
 * @param dstbuf The buffer to read to.
 * @param maxchars The number of bytes to read into the buffer.
 * @return the number of bytes that was read, or -1 on error.
 */
extern "C" PIPELIB_API int pipe_read(int fd, void* dstbuf, int maxchars);

/**
 * Writes to a pipe with the given file descriptor.
 *
 * @param fd The file descriptor that specifies the write end of the pipe.
 * @param srcbuf The buffer to write to.
 * @param maxchars The number of bytes in the buffer to write.
 * @return the number of bytes that was written, or -1 on error.
 */
extern "C" PIPELIB_API int pipe_write(int fd, void* srcbuf, int length);

/**
 * Duplicates the given file descriptor.
 *
 * @param fd The file descriptor to duplicate.
 * @return The id of the file descriptor copy, or -1 on error.
 */
extern "C" PIPELIB_API int pipe_dup(int fd);

/**
 * Duplicates the given file descriptor such that the new file descriptor
 * gets a given id.
 *
 * @param fd1 The file descriptor to duplicate.
 * @param fd2 The id of the new file descriptor.
 * @return The id of the file descriptor copy, or -1 on error.
 */
extern "C" PIPELIB_API int pipe_dup2(int fd1, int fd2);

/**
 * Closes the given file descriptor.
 *
 * @param fd File descriptor to close.
 * @return 0 on success, -1 on error.
 */
extern "C" PIPELIB_API int pipe_close(int fd);

/**
 * Spawns a new process.
 *
 * Note: On *nix systems a new session is created such that the spawned
 *       process is session and process group leader. This is will allow
 *       us to send a signal to the whole process tree.
 *
 * @param cmd The path to the binary that will be executed.
 * @param args The path to the binary followed by the arguments to it.
 * @param dir Changes the current working directory to this before 
 *            spawning the process.
 * @return The pid of the spawned process, or negative on error.
 *
 */
extern "C" PIPELIB_API int proc_spawn(char* cmd, char* args, char* dir);

/**
 * Waits for the given process to exit.
 *
 * @param pid Process id of the process to wait for.
 * @return 0 if the process was waited for and terminated, -1 otherwise.
 */
extern "C" PIPELIB_API int proc_wait_for(int pid);

/**
 * Kills the given process.
 *
 * Note: On *nix all processes in the process group of the given process
 *       get the kill signal.
 *
 * @param pid
 * @param exit_code
 * @return 0 on success, or negative on error.
 */
extern "C" PIPELIB_API int proc_kill(int pid, int exit_code);
