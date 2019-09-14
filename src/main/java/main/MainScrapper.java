package main;

import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.util.ByteArrayBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        OkHttpClient client = new OkHttpClient();
        PropertyService credentials = new PropertyService( "credentials.properties" );

        String cookie = getCookie( client );
        String sessionString;
        if ( cookie != null ) {
            sessionString = cookie.split( ";" )[0];
        } else {
            throw new RuntimeException( "Can't retrieve unsigned cookie" );
        }

        if ( login( client, sessionString, credentials.getValue( ID_KEY ), credentials.getValue( PASS_KEY ) ) ) {
            System.out.println( "Successfully logged in" );
        } else {
            throw new RuntimeException( "Can't log in as user:" );
        }

        if ( selectAccount( client, sessionString, "1903", "1" ) ) {
            System.out.println( "Successfully selected account: [1903], fund: [1]" );
        } else {
            throw new RuntimeException( "Can't select account: [1903], fund: [1]" );
        }

        Response response4 = getReportResponse( client, sessionString );
        if ( !response4.isSuccessful() ) {
            throw new IOException( "Unexpected code " + response4 );
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

    @NotNull
    private static Response getReportResponse( OkHttpClient client, String sessionString ) throws IOException {
        String getReportBaseUrl = "https://www.cadiemfondos.com.py/clientes/generarPDF";
        HttpUrl.Builder httpBuilder = HttpUrl.parse( getReportBaseUrl ).newBuilder();

        httpBuilder.addQueryParameter( "rangoFecha", "01/08/2000 - 13/09/2019" );

        Request getReport = new Request.Builder()
                .addHeader( COOKIE, sessionString )
                .url( httpBuilder.build() )
                .method( "GET", null )
                .build();

        return client.newCall( getReport ).execute();
    }

    private static boolean selectAccount( OkHttpClient client, String sessionString, String accountId, String fundId ) throws IOException {
        RequestBody accountSelectionPayload = new MultipartBody.Builder()
                .setType( MultipartBody.FORM )
                .addFormDataPart( "id_cuenta", accountId )
                .addFormDataPart( "id_fondo", fundId )
                .addFormDataPart( "participe", "1" )
                .build();

        Request accountSelectionRequest = new Request.Builder()
                .addHeader( COOKIE, sessionString )
                .url( "https://www.cadiemfondos.com.py/clientes/sessionar_cuenta" )
                .method("POST", accountSelectionPayload )
                .build();

        Response accountSelectionResponse = client.newCall( accountSelectionRequest ).execute();

        return accountSelectionResponse.isSuccessful();
    }

    private static boolean login( OkHttpClient client, String sessionString, String userId, String password ) throws IOException {


        RequestBody loginRequestPayload = new MultipartBody.Builder()
                .setType( MultipartBody.FORM )
                .addFormDataPart( ID_KEY, userId )
                .addFormDataPart( PASS_KEY, password )
                .build();

        Request login = new Request.Builder()
                .addHeader( COOKIE, sessionString )
                .url( "https://www.cadiemfondos.com.py/clientes/verificaLogin" )
                .method("POST", loginRequestPayload )
                .build();

        Response loginResponse = client.newCall( login ).execute();

        return loginResponse.isSuccessful();
    }

    @Nullable
    private static String getCookie( OkHttpClient client ) throws IOException {
        Request getCookie = new Request.Builder()
                .url( "https://www.cadiemfondos.com.py/clientes/iniciar" )
                .method( "GET", null )
                .build();

        Response response = client.newCall( getCookie ).execute();

        if ( response.isSuccessful() ) {
            return response.header( "Set-Cookie" );
        } else {
            return null;
        }
    }
}
