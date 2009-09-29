// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the PIPELIB_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// PIPELIB_API functions as being imported from a DLL, whereas this DLL sees symbols
// defined with this macro as being exported.
#ifdef PIPELIB_EXPORTS
#define PIPELIB_API __declspec(dllexport)
#else
#define PIPELIB_API __declspec(dllimport)
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
