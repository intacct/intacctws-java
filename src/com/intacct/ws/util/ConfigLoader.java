package com.intacct.ws.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;

import com.intacct.ws.APIUtil;
import com.intacct.ws.exception.IntacctSDKRuntimeException;

/**
 * Simple config file loader for Intacct
 * Will load the config parameters from the env and if not defined will attempt to load from the config file 
 * Exception if not found  
 * 
 * @author habrudan
 *
 */
public final class ConfigLoader {
	
	private static ConfigLoader instance = null; 
	private static Map<String, String> env = System.getenv();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ConfigLoader.printProperties();

	}
	
	private static Properties prop = new Properties();
	private static InputStream input = null;

	// static initialization (one time at startup)
	public static synchronized  ConfigLoader getInstance() throws IntacctSDKRuntimeException {
		if (instance == null) {
			instance = new ConfigLoader();
		}
		
		return instance; 
	}
	
	public static String getProperty(String key){
		String configValue = env.get(key); 
		if (configValue == null) {
			configValue = getFromFile(key);
		}
		return configValue;
	}
	
	private static String getFromFile(String key){
		if (input == null) {
			try {
				input = new FileInputStream(".\\config\\config.properties");

				// load a properties file
				prop.load(input);

			} catch (IOException ex) {
				ex.printStackTrace();
				throw new IntacctSDKRuntimeException(
		    				"#### ConfigLoader::static intializaer  ------ Unable to read properties file ------");
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return prop.getProperty(key); 
	}
	
	public static void printProperties(){
		for (Object key : prop.keySet())
			System.out.println("---- Property key: " + key + "   Value:"  + prop.getProperty((String)key));
	}
}
