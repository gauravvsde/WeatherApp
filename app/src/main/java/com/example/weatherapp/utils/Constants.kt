package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


object Constants {
    // TODO (Add a function to check the network connection is available or not.)
    /**
     * This function is used check the weather the device is connected to the Internet or not.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // In the latest version of android studio so we have to
        // return depending on the version. Here M is version(here = 23)
        // This if block for new versions of android.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Create a Network. If we can't get the access then return false;
            val network= connectivityManager.activeNetwork ?: return false
            // first we get the active network then we check network capabilites
            // if they also are empty then return false.
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                // Return true when active network capabilities like- wifi,
                // ethernet, cellular data.
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // This deprecated approach for old android studio versions.
            // Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}