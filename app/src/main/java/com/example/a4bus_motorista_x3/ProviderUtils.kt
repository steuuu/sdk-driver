package com.example.a4bus_motorista_x3

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import java.lang.IllegalStateException
import java.util.Objects

object ProviderUtils {
    private const val TAG = "ProviderUtils"
    private const val PROVIDER_ID_KEY = ".provider_id"
    private const val PROVIDER_URL_KEY = ".provider_url"

    fun getProviderId(context: Context): String {
        val metadata = getAppMetadata(context)
        check(metadata.containsKey(PROVIDER_ID_KEY)) {
            "Os metadados do aplicativo no manifesto não contêm o ID do provedor."
        }
        return metadata.getString(PROVIDER_ID_KEY) !!
    }

    fun getProviderBaseUrl(context: Context): String {
        val metadata = getAppMetadata(context)
        check(metadata.containsKey(PROVIDER_URL_KEY)) {
            "Os metadados do aplicativo no manifesto não contêm o URL base do provedor."
        }
        return metadata.getString(PROVIDER_URL_KEY) !!
    }

    private fun getAppMetadata(context: Context): Bundle {
        val packageName = context.packageName
        val applicationInfo: ApplicationInfo
        return try {
            applicationInfo =
                context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            applicationInfo.metaData
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Não é possível encontrar os metadados do manifesto no pacote : $packageName")
            throw IllegalStateException(e)
        }
    }
}
