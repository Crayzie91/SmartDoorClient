package smartdoor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.PropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinition;
import com.thingworx.metadata.annotations.ThingworxEventDefinitions;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.types.primitives.IPrimitiveType;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Implementation of the Remote ClientThing.
 * This Class implements and handles the Properties, Services, Events and Subscriptions of the ClientThing.
 * It also implements processScanRequest to handle periodic actions.
 */
@SuppressWarnings("serial")
public class ClientThing extends VirtualThing {

	private static final Logger LOG = LoggerFactory.getLogger(ClientThing.class);
    private static String RepositoryName = "SmartDoorRepository";

	/**
	 * A custom constructor. The Constructor is needed to call initializeFromAnnotations,
	 * which processes all of the VirtualThing's annotations and applies them to the
	 * object.
	 * 
	 * @param name The name of the thing.
	 * @param description A description of the thing.
	 * @param client The client that this thing is associated with.
	 * 
	 * @see: https://developer.thingworx.com/resources/guides/thingworx-java-sdk-quickstart/creating-data-model
	 */
	public ClientThing(String name, String description, ConnectedThingClient client) {
		super(name, description, client);
		this.initializeFromAnnotations();
	}
	
	/**	
	 * This method will get called when a connect or reconnect happens
	 * The called functions synchronize the state and the properties of the virtual thing
	 */
	@Override
	public void synchronizeState() {
		super.synchronizeState();
		// Send the property values to ThingWorx when a synchronization is required
		super.syncProperties();
	}
	
	/**
	 * This method provides a common interface amongst the VirtualThings for processing
	 * periodic requests. It is an opportunity to access data sources, update 
	 * property values, push new values to the server, and take other actions.
	 * 
	 * @see VirtualThing#processScanRequest()
	 */
	@Override
	public void processScanRequest() {
		try {	
			this.updateSubscribedProperties(1000);
			this.updateSubscribedEvents(1000);
		}
		catch(Exception eProcessing) {
			System.out.println("Error Processing Scan Request");
			}	
	}
	
	/**
	 * This Method handles the property writes from the server
 	 * 
     * @param property
     * @param value
     * @throws java.lang.Exception
 	 * @see VirtualThing#processPropertyWrite(PropertyDefinition, IPrimitiveType)
 	 */
	@Override
	public void processPropertyWrite(PropertyDefinition property, @SuppressWarnings("rawtypes") IPrimitiveType value) throws Exception {
		String propName = property.getName();
		setProperty(propName,value);		
		LOG.info("{} was set. New Value: {}", propName, value);
	}
		
	/**
	 * This Method is used to read a Property of a Thing on the Thingworx Platform.
	 * 
	 * @param PropertyName	Name of the Property to change
	 * @return Returns Object that contains the read value
	 */
	public Object getClientProperty(String PropertyName) {
		Object var = getProperty(PropertyName).getValue().getValue();	
		LOG.info("{} was read. Value: {}", PropertyName, var);
		return var;
	}
	
	/**
	 * This Method is used to write a Property of a Thing on the Thingworx Platform.
	 * Value is casted to a generic type for further use.
	 * 
	 * @param PropertyName	Name of the Property to change
	 * @param value	New Value of the Property
	 * @throws Exception
	 */
	public void setClientProperty(String PropertyName, Object value) throws Exception{
		setProperty(PropertyName, value);		
		LOG.info("{} was set. New Value: {}", this.getBindingName(), value);
	}
	
	/**
	 * This function handles the entry of an unknown Person.
	 * 
	 * @return TRUE if execution was successful 
	 * @throws Exception
	 */
    @ThingworxServiceDefinition(name="UnknownEntry", description="Function to remotely handle the door.")
	@ThingworxServiceResult(name="result", description="Path to Image in the repository.", baseType="STRING")
	public String UnknownEntry() throws Exception {            
            String timeStamp = new SimpleDateFormat("ddMM_HHmm").format(Calendar.getInstance().getTime());
            String cmd = "sudo raspistill -o ./Images/"+timeStamp+".jpg";
            String path2repository = "/"+this.getBindingName()+"/"+timeStamp+".jpg";
            String path2image = "./Images/"+timeStamp+".jpg";
            
            
            callCMD(cmd);    
            
            FileTransferThing transfer = new FileTransferThing(RepositoryName, this.getClient()); 
            transfer.createFolder("/"+this.getBindingName());
            boolean success=transfer.uploadImage(path2repository, path2image);
            
            cmd = "rm "+path2image;
            callCMD(cmd);
            
            LOG.info("Photo was saved in repository. ./Images/"+timeStamp+".jpg");
            
            return path2repository;
	}
        
	/** 
	 * This Method sets the door status of the client remotely.
	 * 
	 * The following annotation makes a method available to the ThingWorx Server for remote invocation.  
     * 
     * @param status New status of the door
     * @return TRUE if execution was successful
     * @throws Exception 
     * @see https://developer.thingworx.com/resources/guides/thingworx-java-sdk-quickstart/creating-data-model
     */
	@ThingworxServiceDefinition(name="remoteDoor", description="Function to remotely handle the door.")
	@ThingworxServiceResult(name="result", description="TRUE if excecution was successfull.", baseType="BOOLEAN")
	public boolean remoteDoor(
                @ThingworxServiceParameter( name="Status", description="Status of Door to set.", baseType="STRING" ) String status) throws Exception {		
            String cmd = "python ./../../trigger.py "+this.getBindingName()+" "+status;
            callCMD(cmd);
                
            LOG.info("{}'s Door was set to {}.", this.getBindingName(), status);
            return true;
	}
	
	/**
	 * This functions calls a cmd in the cli of the client.
	 * 
	 * @param cmd Command that should be called
	 */
	public void callCMD(String cmd){        
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            if(cmd.contains("raspistill"))
                p.waitFor();
	} catch (Exception e) {
            LOG.error("{} couldn't be called. {}", cmd, e);
            }
	}
}


