package main;

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
    public static void main( String[] args ) {

        OkHttpClient client = new OkHttpClient();

        Request getCookie = new Request.Builder()
                .url( "https://www.cadiemfondos.com.py/clientes/iniciar" )
                .method( "Post", null )
                .build();

        try (Response response = client.newCall( getCookie ).execute()) {
            String cookie = response.header( "Set-Cookie" );
            String sessionString = cookie.split( ";" )[0];

            String sessionName = sessionString.split( "=" )[0];
            String sessionId = sessionString.split( "=" )[1];

            System.out.println( sessionString );
            System.out.println( sessionName );
            System.out.println( sessionId );

            RequestBody loginRequestPayload = new MultipartBody.Builder()
                    .setType( MultipartBody.FORM )
                    .addFormDataPart( "documento", "3968344" )
                    .addFormDataPart( "password", "Lrd696Dac355" )
                    .build();

            Request login = new Request.Builder()
                    .addHeader( "Cookie", sessionString )
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
                    .addHeader( "Cookie", sessionString )
                    .url( "https://www.cadiemfondos.com.py/clientes/sessionar_cuenta" )
                    .post( selectAccountPayload )
                    .build();

            Response response3 = client.newCall( selectAccount ).execute();

            System.out.println( response3.body().string() );

            Request getReport = new Request.Builder()
                    .addHeader( "Cookie", sessionString )
                    .url( "https://www.cadiemfondos.com.py/clientes/generarPDF/?rangoFecha=01/08/2000%20-%2013/09/2019" )
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

//            System.out.println( response4.body().string() );

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}