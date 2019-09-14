package main;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainScrapper {

    public static final String ID_KEY = "documento";
    public static final String PASS_KEY = "password";
    public static final String COOKIE = "Cookie";

    public static void main( String[] args ) throws IOException {

        PropertyService credentials = new PropertyService( "credentials.properties" );
        credentials.getValue( ID_KEY );

        OkHttpClient client = new OkHttpClient();

        Request getCookie = new Request.Builder()
                .url( "https://www.cadiemfondos.com.py/clientes/iniciar" )
                .method( "Post", null )
                .build();

        Response response = client.newCall( getCookie ).execute();
        System.out.println( "Successfully logged in" );
        String cookie = response.header( "Set-Cookie" );
        String sessionString = cookie.split( ";" )[0];

        RequestBody loginRequestPayload = new MultipartBody.Builder()
                .setType( MultipartBody.FORM )
                .addFormDataPart( ID_KEY, credentials.getValue( ID_KEY ) )
                .addFormDataPart( PASS_KEY, credentials.getValue( PASS_KEY ) )
                .build();

        Request login = new Request.Builder()
                .addHeader( COOKIE, sessionString )
                .url( "https://www.cadiemfondos.com.py/clientes/verificaLogin" )
                .post( loginRequestPayload )
                .build();

        Response response2 = client.newCall( login ).execute();

        System.out.println( response2.body().string() );

        RequestBody selectAccountPayload = new MultipartBody.Builder()
                .setType( MultipartBody.FORM )
                .addFormDataPart( "id_cuenta", "1903" )
                .addFormDataPart( "id_fondo", "1" )
                .addFormDataPart( "participe", "1" )
                .build();

        Request selectAccount = new Request.Builder()
                .addHeader( COOKIE, sessionString )
                .url( "https://www.cadiemfondos.com.py/clientes/sessionar_cuenta" )
                .post( selectAccountPayload )
                .build();

        Response response3 = client.newCall( selectAccount ).execute();

        System.out.println( response3.body().string() );

        String getReportBaseUrl = "https://www.cadiemfondos.com.py/clientes/generarPDF";
        HttpUrl.Builder httpBuilder = HttpUrl.parse( getReportBaseUrl ).newBuilder();

        httpBuilder.addQueryParameter( "rangoFecha", "01/08/2000 - 13/09/2019" );

        Request getReport = new Request.Builder()
                .addHeader( COOKIE, sessionString )
                .url( httpBuilder.build() )
                .method( "get", null )
                .build();

        Response response4 = client.newCall( getReport ).execute();
        if (!response4.isSuccessful()) {
            throw new IOException( "Unexpected code " + response );
        }

        File myDir = new File( "/home/augusto/IdeaProjects/finance-reports/src/test/output/" );
        myDir.mkdirs();
        String fname = "test.pdf";

        File file = new File( myDir, fname );
        if (file.exists()) {
            file.delete();
        }

        InputStream is = response4.body().byteStream();
        BufferedInputStream bis = new BufferedInputStream( is );
        ByteArrayBuffer baf = new ByteArrayBuffer( 50 );
        int current = 0;
        while (( current = bis.read() ) != -1) {
            baf.append( (byte) current );
        }
        FileOutputStream fos = new FileOutputStream( file );
        fos.write( baf.toByteArray() );
        fos.close();

    }
}
