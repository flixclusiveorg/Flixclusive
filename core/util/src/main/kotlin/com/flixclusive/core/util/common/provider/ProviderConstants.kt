package com.flixclusive.core.util.common.provider

import android.os.Environment.getExternalStorageDirectory

val PLUGINS_PATH = getExternalStorageDirectory().absolutePath + "/Flixclusive/repositories/"