package com.example.a4bus_motorista_x3

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.ArrayList

object PermissionUtils {
    private val REQUIRED_PERMISSIONS =
        arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.WRITE_EXTERNAL_STORAGE,
            permission.READ_PHONE_STATE
        )

    @RequiresApi(Build.VERSION_CODES.S)
    private val REQUIRED_PERMISSIONS_OS_12PLUS =
        arrayOf(
            permission.ACCESS_FINE_LOCATION,
            permission.WRITE_EXTERNAL_STORAGE,
            permission.READ_PHONE_STATE,
            permission.BLUETOOTH_CONNECT
        )

    fun getPermissionsToRequest(context: Context?): Array<String> {
        val requiredPermissions =
            if (Build.VERSION.SDK_INT >= 33) REQUIRED_PERMISSIONS_OS_12PLUS else REQUIRED_PERMISSIONS
        val permissionsToRequest: MutableList<String> = ArrayList()
        for (permissionToCheck in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(context !!, permissionToCheck) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permissionToCheck)

                if (permissionToCheck == permission.ACCESS_FINE_LOCATION) {
                    permissionsToRequest.add(permission.ACCESS_COARSE_LOCATION)
                }
            }
        }
        return permissionsToRequest.toTypedArray()
    }
}
