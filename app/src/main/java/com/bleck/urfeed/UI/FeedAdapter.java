package com.bleck.urfeed.UI;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.bleck.urfeed.Modelo.ScriptDatabase;
import com.bleck.urfeed.Web.VolleySingleton;
import com.bleck.urfeed.R;

/**
 * Creado por Hermosa Programación
 *
 * Adaptador para inflar la lista de entradas
 */
public class FeedAdapter extends CursorAdapter {

    /*
    Etiqueta de Depuración
     */
    private static final String TAG = FeedAdapter.class.getSimpleName();
    private String feedName;
    private int iconImage;

    /**
     * View holder para evitar multiples llamadas de findViewById()
     */
    static class ViewHolder {
        TextView publisher;
        TextView titulo;
        TextView descripcion;
        NetworkImageView imagen;
        ImageView icon;

        int tituloI;
        int descripcionI;
        int imagenI;
    }

    public FeedAdapter(Context context, Cursor c,String feedName ,int iconImage ,int flags) {
        super(context, c, flags);

        this.feedName = feedName;
        this.iconImage = iconImage;
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View view = inflater.inflate(R.layout.item_layout, null, false);

        ViewHolder vh = new ViewHolder();

        // Almacenar referencias
        vh.titulo = (TextView) view.findViewById(R.id.titulo);
        vh.descripcion = (TextView) view.findViewById(R.id.descripcion);
        vh.imagen = (NetworkImageView) view.findViewById(R.id.imagen);

        vh.publisher = (TextView) view.findViewById(R.id.publisher);
        vh.icon = (ImageView) view.findViewById(R.id.icon);

        // Setear indices
        vh.tituloI = cursor.getColumnIndex(ScriptDatabase.ColumnEntradas.TITULO);
        vh.descripcionI = cursor.getColumnIndex(ScriptDatabase.ColumnEntradas.DESCRIPCION);
        vh.imagenI = cursor.getColumnIndex(ScriptDatabase.ColumnEntradas.URL_MINIATURA);

        view.setTag(vh);

        return view;
    }

    public void bindView(View view, Context context, Cursor cursor) {

        final ViewHolder vh = (ViewHolder) view.getTag();

        // Setear el texto al titulo
        vh.titulo.setText(cursor.getString(vh.tituloI));

        // Obtener acceso a la descripción y su longitud
        int ln = cursor.getString(vh.descripcionI).length();
        String descripcion = cursor.getString(vh.descripcionI);

        // Acortar descripción a 77 caracteres
        if (ln >= 150)
            vh.descripcion.setText(descripcion.substring(0, 150)+"...");
        else vh.descripcion.setText(descripcion);

        // Obtener URL de la imagen
        String thumbnailUrl = cursor.getString(vh.imagenI);

        // Obtener instancia del ImageLoader
        ImageLoader imageLoader = VolleySingleton.getInstance(context).getImageLoader();

        // Volcar datos en el image view
        vh.imagen.setImageUrl(thumbnailUrl, imageLoader);

        // Escribir el publisher
        vh.publisher.setText(feedName);

        // Mostrar imagen
        vh.icon.setImageResource(iconImage);

    }
}