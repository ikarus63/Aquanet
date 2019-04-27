package cz.pallasoftware.shout;

import android.content.Context;
import android.net.ConnectivityManager;

import java.net.URL;
import java.net.URLConnection;


public class DetectConnection {
    public static boolean checkInternetConnection(Context context) {

        ConnectivityManager con_manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        checkConnection();

        return (con_manager.getActiveNetworkInfo() != null
                && con_manager.getActiveNetworkInfo().isAvailable()
                && con_manager.getActiveNetworkInfo().isConnected());
    }

    public static void checkConnection() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL myUrl = new URL("https://www.shout.cz/mobile.aspx");
                    URLConnection connection = myUrl.openConnection();
                    connection.setConnectTimeout(4000);
                    connection.connect();
                    //vše je ok
                } catch (Exception e) {
                    //není spojení se serverem!
                    cancelWebView();
                }
            }
        }).start();
    }

    public static void cancelWebView() {
        WebViewActivity.cancelWebView();
    }
}
