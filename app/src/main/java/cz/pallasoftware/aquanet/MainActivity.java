package cz.pallasoftware.aquanet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

public class MainActivity extends Activity {

    String URL_ACTION;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            if (extras.containsKey("notificationUrl")) {
                String url = getIntent().getExtras().getString("notificationUrl");
                URL_ACTION = url;

                Log.v("echolofhdfjdsjkf", "MA: " + url);
            } else {
                String url = getIntent().getExtras().getString("url");
                URL_ACTION = url;

                Log.v("echolofhdfjdsjkf", "MA: " + url);
            }
        }else{
            Log.v("echolofhdfjdsjkf", "MA: EXTRAS NULL");
            URL_ACTION = "empty";
        }

        if(URL_ACTION == null || URL_ACTION.isEmpty() || URL_ACTION.equals("")){
            URL_ACTION = "empty";
        }

        Intent i = new Intent(this, WebViewActivity.class);
        i.putExtra("url", URL_ACTION);
        startActivity(i);
        this.finish();
    }
}
