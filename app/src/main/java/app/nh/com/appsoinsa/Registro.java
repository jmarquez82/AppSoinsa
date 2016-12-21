package app.nh.com.appsoinsa;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.nh.com.appsoinsa.cls.Functions;
import app.nh.com.appsoinsa.cls.Imagenes;
import app.nh.com.appsoinsa.cls.LocationGps;
import app.nh.com.appsoinsa.cls.OutObject;
import app.nh.com.appsoinsa.cls.Preobra;


public class Registro extends AppCompatActivity implements OnMapReadyCallback {
    TextView idApp, txtGps;
    String idRegistro = "";
    EditText txtDirecc, txtDesc, txtAlias;
    LinearLayout btnEnviar;
    Button btnPhoto;

    byte[] inputData;
    String pathFile;

    public String latitud = "";
    public String longitud = "";

    public static GoogleMap mapa;

    private String APP_DIRECTORY = "SoinsaMedia/";
    private String MEDIA_DIRECTORY = APP_DIRECTORY + "media";
    String path_;

    private final int PHOTO_CODE = 100;
    private final int SELECT_PICTURE = 200;

    LinearLayout contentImg;
    List<Imagenes> imgs = new ArrayList<Imagenes>();

    final static Handler hh = new Handler();

    ScrollView scrollme;
    boolean statusMap = false;


    Geocoder geocoder;
    List<Address> addresses;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        idRegistro = "";

        idApp = (TextView) findViewById(R.id.idAppRegister);
        txtGps = (TextView) findViewById(R.id.txtGps);
        txtAlias = (EditText) findViewById(R.id.txtAlias);
        txtDesc = (EditText) findViewById(R.id.txtDesc);
        txtDirecc = (EditText) findViewById(R.id.txtDirecc);
        btnEnviar = (LinearLayout) findViewById(R.id.btnEnviar);
        btnPhoto = (Button) findViewById(R.id.btnPhoto);
        contentImg = (LinearLayout) findViewById(R.id.contentImg);
        scrollme = (ScrollView) findViewById(R.id.scrollme);

        //ViewTreeObserver obs = new ViewTreeObserver();
        //scrollme.getViewTreeObserver().removeOnPreDrawListener(this);
        scrollme.setScrollY(500);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            idRegistro = (String) extras.get("idAppRegister");
            idApp.setText(idRegistro);
            getValues(idRegistro);
            statusMap = false;
        }else{
            statusMap = true;
        }

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOption();
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Preobra p = new Preobra();
                p.setAlias(txtAlias.getText().toString());
                p.setBodegaId("1");
                p.setDireccion(txtDirecc.getText().toString());
                p.setEmpresaId("1");
                p.setEstadoId("0");
                p.setFechaApk("");

                p.setDescripcion(txtDesc.getText().toString());
                p.setIdPreObra("");
                p.setLatitud(latitud);
                p.setLongitud(longitud);
                p.setImgs(imgs);

                if (idRegistro.equals("")) {
                    p.setFechaCreacion(Functions.datenow());
                    p.setIdApp(Functions.keygen());
                    addRegister(p);

                } else {
                    p.setIdApp(idRegistro);
                    update(p);
                }
            }
        });

         /*GPS*/
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //MyLocationListener mlocListener = new MyLocationListener(this);
        LocationGps gps = new LocationGps(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Requiere permisos para Android 6.0
            Log.e("Location", "No se tienen permisos necesarios!, se requieren.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 225);
            return;
        }else{
            Log.i("Location", "Permisos necesarios OK!.");
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) gps);
        }

        SupportMapFragment mfragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mfragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mapa = map;

        LatLng santiago = new LatLng(-33.400601, -70.651336);
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(santiago,15));
    }


    public void addImgs(String path){
        Imagenes img = new Imagenes();
        img.setImg(path);
        imgs.add(img);
    }

    public String update(Preobra po){
        String result = "";
        List<Preobra> pl = SplashScreen.obj.getListObject();
        int i = 0;
        for( Preobra a : pl) {
            String id = po.getIdApp();
            if(a.getIdApp() != null) {
                if (a.getIdApp().equals(id)) {
                    SplashScreen.obj.getListObject().get(i).setDescripcion(po.getDescripcion());
                    SplashScreen.obj.getListObject().get(i).setDireccion(po.getDireccion());
                    SplashScreen.obj.getListObject().get(i).setAlias(po.getAlias());
                    SplashScreen.obj.getListObject().get(i).getImgs().addAll(po.getImgs());

                }
            }
            i++;
        }
        Functions.desSerialize();
        finish();
        return result;
    }

    public void addRegister(Preobra po){
            SplashScreen.obj.getListObject().add(po);
            /*Actualiza el Json*/
            Functions.desSerialize();
            Toast.makeText(Registro.this,"Registro Guardado", Toast.LENGTH_LONG).show();
            finish();
    }

    public List<Preobra> getValues(String find){
        List<Preobra> pl = SplashScreen.obj.getListObject();
        List<Preobra> secondList = new ArrayList<Preobra>();
        for( Preobra a : pl) {

            if(a.getIdApp() != null) {
                if (a.getIdApp().equals(find)) {
                    secondList.add(a);
                    txtAlias.setText(a.getAlias());
                    txtDirecc.setText(a.getDireccion());
                    txtDesc.setText(a.getDescripcion());
                    if(a.getLatitud().trim().equals("")){
                        statusMap = true;
                    }else {
                        statusMap = false;
                        listenGpsStatic(a.getLatitud(),a.getLongitud());
                    }

                    if(a.getImgs() != null) {
                        for (Imagenes im : a.getImgs()) {
                            File file = new File(im.getImg());
                            Uri ur = Uri.fromFile(file);
                            addImgLayout(ur);
                        }
                    }
                }
            }
        }
        return secondList;
    }


    public void listenGps(String lat, String lon){
        if(statusMap) {
            this.longitud = lon;
            this.latitud = lat;
            String cadena = "v1. long:" + lon + " lat:" + lat;
            txtGps.setText(cadena);
            setLocationPoint(lat, lon);
            getAddress(lat,lon);
        }
    }

    public void listenGpsStatic(String lat, String lon){

            this.longitud = lon;
            this.latitud = lat;
            String cadena = "v2. long:" + lon + " lat:" + lat;
            txtGps.setText(cadena);
            setLocationPoint(lat, lon);
    }

    public void getAddress(String lat, String lon){


        Double long_ = Double.parseDouble(lon);
        Double lat_ = Double.parseDouble(lat);
        geocoder = new Geocoder(this,Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(lat_, long_, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            String dir = address + ", " + city;
            txtDirecc.setText(dir);
        }catch (Exception ex){
            //Toast.makeText(Registro.this,"No se pudo",Toast.LENGTH_LONG).show();
        }



    }

    public void setLocationPoint(String lat, String lng){

        Double long_ = Double.parseDouble(lng);
        Double lat_ = Double.parseDouble(lat);

        LatLng pos = new LatLng(lat_,long_);
        mapa.clear();

        mapa.addMarker(new MarkerOptions()
                .position(pos)
                .title("Punto GPS"));

        mapa.moveCamera(CameraUpdateFactory.newLatLng(pos));
    }


    /*FOTOGRAFIA FOTO!GALERIA*/
    public void showOption(){
        final CharSequence[] options = {"Tomar Foto","Elegir de Galeria","Salir"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(Registro.this);
        builder.setTitle("Opciones");
        builder.setItems(options, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int seleccion){

                if(options[seleccion]=="Tomar Foto"){
                    openCamara();
                }else if(options[seleccion]=="Elegir de Galeria"){
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");

                    startActivityForResult(intent.createChooser(intent,"Selecciona IMG"), SELECT_PICTURE);
                }else if(options[seleccion]=="Salir"){
                    dialog.dismiss();
                }
            }
        } );
        builder.show();
    }

    /*Abre camara y obtiene path de la imagen tomada*/
    public void openCamara(){

        File file = new File(Environment.getExternalStorageDirectory(),MEDIA_DIRECTORY);
        file.mkdirs();
        path_ = Environment.getExternalStorageDirectory() + File.separator
                + MEDIA_DIRECTORY
                + File.separator;

        path_ = path_ + generateName();
        File newfile = new File(path_);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(newfile));
        startActivityForResult(intent, PHOTO_CODE);
    }

    /*Cambia imagen (InputStream) a byte[]*/
    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    /*Genera un nombre estandar, que sera asignado al archivo*/
    public String generateName(){
        Long timestamp = System.currentTimeMillis() / 1000;
        String name = "IMG_" + timestamp.toString() + ".jpg";
        return name;
    }

    /*Resultado de camara o galeria*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        pathFile = "";
        switch (requestCode) {
            //Si es tomada por camara
            case PHOTO_CODE:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(Registro.this, "Cargando Imágen", Toast.LENGTH_SHORT).show();
                    MediaScannerConnection.scanFile(Registro.this, new String[]{path_}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                        Log.i("Path Img PHOTO CODE ", path);

                                }


                            });
                    addImgs(path_);
                    File filex = new File(path_);
                    Uri ur = Uri.fromFile(filex);
                    addImgLayout(ur);

                }
                break;
            //Si es tomada desde la galeria
            case SELECT_PICTURE:

                if (resultCode == RESULT_OK) {
                    Toast.makeText(Registro.this, "Generando imágen", Toast.LENGTH_SHORT).show();
                    addImgs(data.getData().getPath());
                    Uri imgUri = data.getData();
                    addImgLayout(imgUri);
                    Log.i("Path Img SELECT PICTURE", data.getData().getPath());

                }
                break;
        }

    }


    public void addImgLayout(Uri uri){
        ImageView imgAdd = new ImageView(this);
        imgAdd.setImageURI(uri);

        LinearLayout relativeLayout1 = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(60,60);
        layoutParams.setMargins(5,5,5,5);
        //layoutParams.
        relativeLayout1.setBackgroundColor(Color.parseColor("#CCCCCC"));
        relativeLayout1.addView(imgAdd);


        contentImg.addView(relativeLayout1,layoutParams);

    }


}
