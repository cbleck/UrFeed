package com.bleck.urfeed.Web;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Creado por Hermosa Programación.
 *
 * Petición personalizada para el trato de flujos XML
 */

public class XmlRequest<T> extends Request<T> {

    private static final String TAG = XmlRequest.class.getSimpleName();

    // Atributos
    private final Class<T> clazz;
    private final Map<String, String> headers;
    private final Response.Listener<T> listener;
    private final Serializer serializer = new Persister();

    /**
     * Se predefine para el uso de peticiones GET
     */
    public XmlRequest(String url, Class<T> clazz, Map<String, String> headers,
                      Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.clazz = clazz;
        this.headers = headers;
        this.listener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers != null ? headers : super.getHeaders();
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {

            // Convirtiendo el flujo en cadena con formato UTF-8
            String xml = new String(response.data, "UTF-8");

            // Depurando...
            Log.d(TAG, xml);

            // Enviando la respuesta parseada
            return Response.success(
                   serializer.read(clazz, xml),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }
    }
}
