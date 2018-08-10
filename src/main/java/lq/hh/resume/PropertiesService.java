package lq.hh.resume;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import lq.hh.exception.NoPropertiesException;

public class PropertiesService {
	
	private static final String propertiesPath = "./updater.properties";

	public static final String CLIENT_SECRET = "client_secret";
	public static final String CLIENT_ID = "client_id";
	public static final String HEADHUNT_USERNAME = "username";
	public static final String HEADHUNT_PASSWORD = "password";
	
	
	public  static Properties getProps() throws NoPropertiesException{
		// First try loading from the current directory
		Properties props = new Properties();
		InputStream is = null;

		try {
			File f = new File(propertiesPath);
			is = new FileInputStream(f);
		} catch (Exception e) {
			is = null;
		}
		try{
			if (is == null) {
				// Try loading from classpath
				is = props.getClass().getResourceAsStream(propertiesPath);
			}
			// Try loading properties from the file (if found)
			props.load(is);
		}catch(IOException ioe){
			throw new NoPropertiesException();
		}catch(NullPointerException npe){
			throw new NoPropertiesException();
		}
		return props;
	}
	
	public static  String loadProperty(String key) throws NoPropertiesException {
		Properties props = getProps();
		
		String token = props.getProperty(key);
		return token;
	}
	
	public static void storeProperty(String key, String value) {
		try {
			Properties props;
			try{
				props = PropertiesService.getProps();
			} catch(NoPropertiesException npe){
				props = new Properties();
			}
			props.setProperty(key, value);
			File f = new File(propertiesPath); 
			OutputStream out = new FileOutputStream(f);
			props.store(out, "");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
