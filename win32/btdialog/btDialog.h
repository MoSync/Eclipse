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

#ifndef BTDIALOG_H
#define BTDIALOG_H

#ifdef WIN32
#define BTDIALOG_EXPORT __declspec(dllexport)
#define BTDIALOG_IMPORT __declspec(dllimport)
#else
#define BTDIALOG_EXPORT __attribute__((visibility("default")))
#define BTDIALOG_IMPORT __attribute__((visibility("default")))
#endif

#ifdef BTDIALOG_EXPORTS
#define BTDIALOG_API BTDIALOG_EXPORT
#else
#define BTDIALOG_API BTDIALOG_IMPORT
#endif

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
BTDIALOG_API int btDialog(BT_DEVICE* device);

#endif	//BTDIALOG_H
