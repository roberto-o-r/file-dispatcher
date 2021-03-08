package com.isscroberto.filedispatcher;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.voghdev.pdfviewpager.library.PDFViewPager;
import es.voghdev.pdfviewpager.library.RemotePDFViewPager;
import es.voghdev.pdfviewpager.library.adapter.PDFPagerAdapter;
import es.voghdev.pdfviewpager.library.remote.DownloadFile;

public class MainActivity extends AppCompatActivity implements DownloadFile.Listener {

    @BindView(R.id.layout_configuracion)
    LinearLayout layoutConfiguracion;
    @BindView(R.id.text_url)
    EditText textUrl;
    @BindView(R.id.view_pdf)
    PDFViewPager viewPdf;
    @BindView(R.id.image_main)
    ImageView imageMain;
    @BindView(R.id.video_main)
    VideoView videoMain;

    PDFPagerAdapter adapter;
    RemotePDFViewPager viewPager;
    String url = "";
    ArrayList<Archivo> archivos = new ArrayList<Archivo>();
    int index = 0;
    Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        // Obtener url si se ha guardado previamente.
        this.url = getSharedPreferences("com.isscroberto.filedispatcher", 0).getString("url", "");
        if(!this.url.equals("")){
            this.layoutConfiguracion.setVisibility(View.GONE);
            DescargarArchivo();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.adapter.close();
    }

    @Override
    public void onSuccess(String url, String destinationPath) {
        this.adapter = new PDFPagerAdapter(this, destinationPath);
        this.adapter.notifyDataSetChanged();
        this.viewPdf.setAdapter(this.adapter);
    }

    @Override
    public void onFailure(Exception e) {

    }

    @Override
    public void onProgressUpdate(int progress, int total) {

    }

    private void DescargarArchivo() {
        new Thread(new Runnable()
        {
            public void run()
            {
                try {
                    // Crear URL para la descarga del archivo.
                    URL url = new URL(MainActivity.this.url);

                    // Leer texto del archivo.
                    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                    String str;
                    while ((str = in.readLine()) != null) {
                        if(!str.trim().equals("")) {
                            Archivo archivo = new Archivo();
                            archivo.setUrl(str.split(",")[0]);
                            archivo.setSegundos(Integer.parseInt(str.split(",")[1]));

                            archivos.add(archivo);
                        }
                    }
                    in.close();
                } catch (MalformedURLException e) {
                } catch (IOException e) { }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        IniciarTimer(0);
                    }
                });
            }
        }).start();
    }

    private void IniciarTimer(int segundos) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                // Carga de archivo.
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    // Limpiar contenedor de imagen y pdf.
                    imageMain.setImageResource(0);
                    viewPdf.setAdapter(null);

                    MainActivity.this.CargarArchivo(index);

                    // Reinicio de timer con nuevo intervalo.
                    timer.cancel();
                    IniciarTimer(archivos.get(index).getSegundos());

                    // Incremento de index.
                    index++;
                    // Validación de index.
                    if(index == archivos.size()) index = 0;
                    }
                });
            }
        }, segundos * 1000);
    }

    private void CargarArchivo(int index) {
        Archivo archivo = archivos.get(index);
        // Verificar si el archivo es pdf.
        if(archivo.getUrl().contains(".pdf")) {
            CargarPdf(archivo);
        }
        if(archivo.getUrl().contains(".jpeg") || archivo.getUrl().contains(".jpg") || archivo.getUrl().contains(".png")) {
            CargarImg(archivo);
        }
        if(archivo.getUrl().contains(".mp4")) {
            CargarVid(archivo);
        }
    }

    private void CargarPdf(Archivo archivo) {
        // Ocultar configuración y vista de imagen.
        this.viewPdf.setVisibility(View.VISIBLE);
        this.layoutConfiguracion.setVisibility(View.GONE);
        this.imageMain.setVisibility(View.GONE);
        this.videoMain.setVisibility(View.GONE);

        // Actualizar hora de última actualización.
        Calendar localCalendar = Calendar.getInstance();
        SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("HH:mm");
        getSupportActionBar().setTitle("Última Actualización: " + localSimpleDateFormat.format(localCalendar.getTime()));

        // Cargar y mostrar pdf.
        this.viewPdf.setVisibility(View.VISIBLE);
        this.viewPager = new RemotePDFViewPager(this, archivo.getUrl(), this);
    }

    private void CargarImg(Archivo archivo) {
        // Ocultar configuración.
        this.imageMain.setVisibility(View.VISIBLE);
        this.layoutConfiguracion.setVisibility(View.GONE);
        this.viewPdf.setVisibility(View.GONE);
        this.videoMain.setVisibility(View.GONE);

        // Actualizar hora de última actualización.
        Calendar localCalendar = Calendar.getInstance();
        SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("HH:mm");
        getSupportActionBar().setTitle("Última Actualización: " + localSimpleDateFormat.format(localCalendar.getTime()));

        // Cargar y mostrar imagen.
        this.imageMain.setVisibility(View.VISIBLE);
        Picasso.get().load(archivo.getUrl()).memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE).into(imageMain);
    }

    private void CargarVid(Archivo archivo) {
        // Ocultar configuración.
        this.imageMain.setVisibility(View.GONE);
        this.layoutConfiguracion.setVisibility(View.GONE);
        this.viewPdf.setVisibility(View.GONE);
        this.videoMain.setVisibility(View.VISIBLE);

        // Actualizar hora de última actualización.
        Calendar localCalendar = Calendar.getInstance();
        SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("HH:mm");
        getSupportActionBar().setTitle("Última Actualización: " + localSimpleDateFormat.format(localCalendar.getTime()));

        // Cargar y mostrar video.
        this.videoMain.setVisibility(View.VISIBLE);
        this.videoMain.setVideoPath(archivo.getUrl());
        this.videoMain.start();
    }

    @OnClick(R.id.button_aceptar)
    public void buttonAceptarOnClick() {
        this.url = this.textUrl.getText().toString();

        SharedPreferences.Editor editor = getSharedPreferences("com.isscroberto.filedispatcher", 0).edit();
        editor.putString("url", this.url);
        editor.apply();

        DescargarArchivo();
    }
}
