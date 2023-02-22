package com.example.a4bus_motorista_x3

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionsToRequest = PermissionUtils.getPermissionsToRequest(this)
        if (permissionsToRequest.isEmpty()) {
            loadMainActivity()
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest,
                APP_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != APP_PERMISSION_REQUEST_CODE) {
            return
        }
        val permissionsToRequest = PermissionUtils.getPermissionsToRequest(this)
        if (permissionsToRequest.isEmpty()) {
            loadMainActivity()
        } else {
            val missingPermissionsText = permissionsToRequest.joinToString(separator = ", ")
            AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(
                    resources.getString(
                        R.string.msg_require_permissions,
                        missingPermissionsText
                    )
                )
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    ActivityCompat.requestPermissions(
                        this,
                        permissionsToRequest,
                        APP_PERMISSION_REQUEST_CODE
                    )
                }
                .create()
                .show()
        }
    }

    private fun loadMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val APP_PERMISSION_REQUEST_CODE = 99
    }
}
