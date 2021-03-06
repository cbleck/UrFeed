package com.it.dfeed.UI;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.it.dfeed.Modelo.FeedDatabase;
import com.it.dfeed.Modelo.ScriptDatabase;
import com.it.dfeed.RssParse.Rss;
import com.it.dfeed.Web.VolleySingleton;
import com.it.dfeed.Web.XmlRequest;
import com.it.dfeed.R;

/**
 * Creado por IT
 *
 * Actividad principal que representa el Home de la aplicaci�n
 */

public class MainActivity extends AppCompatActivity {

    /*
    Etiqueta de depuraci�n
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    /*
    Lista
     */
    private ListView listView;
    private String URL_FEED;
    private String feedName;
    private int iconImage;

    /*
    Adaptador
     */
    private com.it.dfeed.UI.FeedAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getIntent() != null) {
            URL_FEED = getIntent().getStringExtra("com.bleck.feedurl");
            iconImage = getIntent().getIntExtra("com.bleck.feedimg",0);
            feedName = getIntent().getStringExtra("com.bleck.feedname");
        }

        setTitle(URL_FEED);

        // Obtener la lista
        listView = (ListView)findViewById(R.id.lista);

        // Regisgrar escucha de la lista
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) adapter.getItem(position);

                // Obtene url de la entrada seleccionada
                String url = c.getString(c.getColumnIndex(ScriptDatabase.ColumnEntradas.URL));

                // Nuevo intent explícito
                Intent i = new Intent(MainActivity.this, DetailActivity.class);

                // Setear url
                i.putExtra("url-extra", url);

                // Iniciar actividad
                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart(){

        super.onStart();

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            VolleySingleton.getInstance(this).addToRequestQueue(
                    new XmlRequest<>(
                            URL_FEED,
                            Rss.class,
                            null,
                            new Response.Listener<Rss>() {
                                @Override
                                public void onResponse(Rss response) {

                                    Log.d(TAG, "Hola");
                                    // Caching
                                    FeedDatabase.getInstance(MainActivity.this).
                                            sincronizarEntradas(response.getChannel().getItems());
                                    // Carga inicial de datos...
                                    new LoadData().execute();
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d(TAG, "Error Volley: " + error.getMessage());
                                }
                            }
                    )
            );
        } else {
            Log.i(TAG, "La conexión a internet no está disponible");
            adapter= new FeedAdapter(
                    this,
                    FeedDatabase.getInstance(this).obtenerEntradas(),
                    feedName,
                    iconImage,
                    SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            listView.setAdapter(adapter);
        }
    }

    public class LoadData extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... params) {
            // Carga inicial de registros
            return FeedDatabase.getInstance(MainActivity.this).obtenerEntradas();

        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            super.onPostExecute(cursor);

            // Crear el adaptador
            adapter = new FeedAdapter(
                    MainActivity.this,
                    cursor,
                    feedName,
                    iconImage,
                    SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            // Relacionar la lista con el adaptador
            listView.setAdapter(adapter);
        }
    }
}
