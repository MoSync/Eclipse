#include <ma.h>

int MAMain() {
	maWriteLog("Before breakpoint", 17);
	maWriteLog("It still works!", 15); //BREAKPOINT
	maWriteLog("After breakpoint", 16);
	return 0;
}
