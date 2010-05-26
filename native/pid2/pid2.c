#ifndef WIN32
#include <unistd.h>
#else
#include <process.h>
#endif


/**
 * Returns the pid of the proccess
 *
 * @return Proccess pid as int
 */
int getpid2 ( void )
{
#ifndef WIN32
	return (int)getpid( );
#else
	return _getpid( );
#endif
}

