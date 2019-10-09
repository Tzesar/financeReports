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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainScrapper {

    public static final String ID_KEY = "documento";
    public static final String PASS_KEY = "password";
    public static final String COOKIE = "Cookie";

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static DateTimeFormatter PRINT_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy", Locale.ENGLISH);

    public static void main( String[] args ) throws IOException {
        OkHttpClient client = new OkHttpClient();
        PropertyService credentials = new PropertyService( "credentials.properties" );

        LocalDate seedDate = LocalDate.parse("01/07/2019", FORMATTER );
        LocalDate today = LocalDate.now();

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

        Map<String, String> accountNameMap = new HashMap<>(  );
        accountNameMap.put( "1903", "DISP" );
        accountNameMap.put( "495", "CREC" );

        Map<String, String> fundByAccountIdMap = new HashMap<>(  );
        fundByAccountIdMap.put( "1903", "1" );
        fundByAccountIdMap.put( "495", "2" );

        for ( String accountId : accountNameMap.keySet() ) {
            if (selectAccount( client, sessionString, accountId, fundByAccountIdMap.get( accountId ) )) {
                String successMessage = String.format( "Successfully selected account: [%s], fund: [%s]", accountId, fundByAccountIdMap.get( accountId ) );
                System.out.println( successMessage );
            } else {
                String exceptionMessage = String.format( "Can't select account: [%s], fund: [%s]", accountId, fundByAccountIdMap.get( accountId ) );
                throw new RuntimeException( exceptionMessage );
            }

            LocalDate beginDate = LocalDate.from( seedDate );
            while (beginDate.plusMonths( 1L ).isBefore( today )) {
                LocalDate endDate = beginDate.plusMonths( 1L ).minusDays( 1L );
                Response reportResponse = getReportResponse( client, sessionString, beginDate, endDate );
                if (!reportResponse.isSuccessful()) {
                    throw new IOException( "Unexpected code " + reportResponse );
                } else {
                    String successMessage = String.format( "Successfully retrieved report from range [%s - %s]", beginDate.format( FORMATTER ), endDate.format( FORMATTER ) );
                    System.out.println( successMessage );
                    String reportName = String.format( "%s_report_from_[%s-%s].pdf", accountNameMap.get( accountId ), beginDate.format( PRINT_FORMATTER ), endDate.format( PRINT_FORMATTER ) );
                    writeReportFromResponse( reportResponse, reportName );
                }

                beginDate = beginDate.plusMonths( 1L );
            }
        }

    }

    private static void writeReportFromResponse( Response reportResponse, String reportName ) throws IOException {
        File myDir = new File( "/home/augusto/IdeaProjects/finance-reports/src/test/output/" );
        myDir.mkdirs();

        File file = new File( myDir, reportName );
        if (file.exists()) {
            file.delete();
        }

        InputStream is = reportResponse.body().byteStream();
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
    private static Response getReportResponse( OkHttpClient client, String sessionString, LocalDate reportStart, LocalDate reportEnd ) throws IOException {
        String getReportBaseUrl = "https://www.cadiemfondos.com.py/clientes/generarPDF";
        HttpUrl.Builder httpBuilder = HttpUrl.parse( getReportBaseUrl ).newBuilder();

        String dateRange = String.format( "%s - %s", reportStart.format( FORMATTER ), reportEnd.format( FORMATTER ) );
        httpBuilder.addQueryParameter( "rangoFecha", dateRange );

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
