#include <ma.h>

int MAMain() {
	maWriteLog("BEFORE\0", 7);
	maWriteLog("It still works!\0", 16); //BREAKPOINT
	maWriteLog("AFTER\0", 6);
	return 0;
}
