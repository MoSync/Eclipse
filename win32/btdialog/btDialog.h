#ifndef BTDIALOG_H
#define BTDIALOG_H

enum BtdResult {
	BTD_ERROR,
	BTD_OK,
	BTD_CANCEL
};

typedef unsigned char byte;
typedef unsigned short wchar;

typedef struct BT_DEVICE {
	byte address[6];
	wchar name[248];
} BT_DEVICE;

//returns one of BtdResult.
//fills device if BTD_OK is returned.
int btDialog(BT_DEVICE* device);

#endif	//BTDIALOG_H
