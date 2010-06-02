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
#include "btDialog.h"

#ifdef WIN32
#include <string.h>

#include <windows.h>
#include <Bthsdpdef.h>
#include <BluetoothAPIs.h>

BTDIALOG_API int btDialog(BT_DEVICE* device) {
	int i;
	BLUETOOTH_SELECT_DEVICE_PARAMS bsdp;
	memset(&bsdp, 0, sizeof(bsdp));
	bsdp.dwSize = sizeof(bsdp);
	//bsdp.pszInfo = L"MoBuild-specific information";
	bsdp.fShowRemembered = TRUE;
	bsdp.fShowUnknown = TRUE;
	bsdp.fShowAuthenticated = TRUE;
	bsdp.cNumDevices = 1;
	if(!BluetoothSelectDevices(&bsdp)) {
		int error = GetLastError();
		BluetoothSelectDevicesFree(&bsdp);
		if(error == ERROR_CANCELLED || error == 0 ||
			error == ERROR_INVALID_WINDOW_HANDLE)	//bug in Windows, should be CANCELLED.
		{
			return BTD_CANCEL;
		}
		//printf("BluetoothSelectDevices error %i\n", error);
		return BTD_ERROR;
	}

	//parse device data, save it.
	if(bsdp.cNumDevices != 1) {
		//printf("%i devices selected!\n", bsdp.cNumDevices);
		BluetoothSelectDevicesFree(&bsdp);
		return BTD_ERROR;
	}

	//memcpy(a.a, bsdp.pDevices->Address.rgBytes, BTADDR_LEN);
	//they write the address in reverse.
	for(i=0; i<6; i++) {
		device->address[i] = bsdp.pDevices->Address.rgBytes[6 - i - 1];
	}

#if 0
	String name;
	WCHAR* wname = bsdp.pDevices->szName;
	name.resize(wcslen(bsdp.pDevices->szName));
	for(int i=0; i<name.size(); i++) {
		name[i] = (char)wname[i];
	}
#endif
	wcscpy(device->name, bsdp.pDevices->szName);

	BluetoothSelectDevicesFree(&bsdp);

	return BTD_OK;
}
#else
BTDIALOG_API int btDialog(BT_DEVICE* device)
{
	return BTD_ERROR;
}

#endif
