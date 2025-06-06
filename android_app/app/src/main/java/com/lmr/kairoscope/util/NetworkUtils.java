package com.lmr.kairoscope.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utilidad para verificar el estado de conectividad de red.
 */
public class NetworkUtils {

    /**
     * Verifica si hay conexión a internet disponible.
     * @param context Contexto de la aplicación para acceder al servicio de conectividad
     * @return true si hay conexión activa, false en caso contrario
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}