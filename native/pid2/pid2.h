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

#ifdef WIN32
#define PID2_EXPORT __declspec(dllexport)
#define PID2_IMPORT __declspec(dllimport)
#else
#define PID2_EXPORT __attribute__((visibility("default")))
#define PID2_IMPORT __attribute__((visibility("default")))
#endif

#ifdef PID2_EXPORTS
#define PID2_API PID2_EXPORT
#else
#define PID2_API PID2_IMPORT
#endif

PID2_API int pid(void);
