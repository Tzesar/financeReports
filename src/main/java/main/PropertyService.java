package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyService {
    private Properties properties;

    public PropertyService( String propertyFileName ) throws IOException {
        properties = new Properties();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream( propertyFileName );

        if (inputStream != null) {
            properties.load( inputStream );
        } else {
            throw new FileNotFoundException("Property file '" + propertyFileName + "' not found in the classpath");
        }
        inputStream.close();
    }

    public String getValue(String key) {
        return properties.getProperty( key );
    }
}
