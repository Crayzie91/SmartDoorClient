package smartdoor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.PropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.types.primitives.IPrimitiveType;

/**
Aspects:	
  	dataChangeType:
  	Specifies when a DataChange Event may be triggered for a Property value change. The options are as follows:
	Always:  Fire the event to subscribers for any property value change
	Never:  Do not fire a change event
	On:  For most values, any change will trigger this.  For more complex data types, such as InfoTables, refer to the specific product documentation.
	Off:  Fire the event if the new value evaluates to a Boolean false
	Value:  For Numbers, if the new value has changed by more than the threshold value, fire the change event.  For non-Numbers, this setting behaves the same as Always except in the case of logging to value streams. 
			Non-Number properties will only log to value streams using the Value data change type if the value actually changes from the previous value.
	
	dataChangeThreshold:
	Specifies if the property value should be persisted through a Thing or system restart. Properties that are not persistent will reset their values to their default after a Thing or system restart.
	
	cacheTime:
	
	isPersistent:
	Specifies if the property value should be persisted through a Thing or system restart. Properties that are not persistent will reset their values to their default after a Thing or system restart.
	
	isReadOnly:
	Specifies if the property value is read-only. If true, the property value can not be changed.
	
	pushType:
	Edge Thing Properties have the ability to push their value changes to the server. This ability is configurable via the server property binding. The configuration choices are as follows:
	Value:  Pushed based on Value Change - you can also configure a value change threshold
	Never:  Never pushed
	Always:  Always pushed every change
	
	defaultValue:
	If the property is configured to have a default value, this value will be present at Thing initialization unless otherwise overwritten by a Persistent value set before system shutdown.		                    		 	

	 */
@SuppressWarnings("serial")
@ThingworxPropertyDefinitions(properties = {
	// This property is setup for collecting time series data. Each value
	// that is collected will be pushed to the platfrom from within the
	// processScanRequest() method.
	@ThingworxPropertyDefinition(name="ClientsConnected",       
			                     description="Number of connected CLients",
			                     baseType="NUMBER",
			                     aspects={"dataChangeType:VALUE",
			                    		  "dataChangeThreshold:0",
			                    		  "cacheTime:0", 
			                    		  "isPersistent:FALSE", 
			                    		  "isReadOnly:FALSE", 
			                    		  "pushType:ALWAYS", 
			                              "defaultValue:0"}),
})


/**
 * Implementation of the Remote ClientThing.
 * This Class implements and handles the Properties, Services, Events and Subscriptions of the ClientThing.
 * It also implements processScanRequest to handle periodic actions.
 */
public class ClientThing extends VirtualThing {

	private static final Logger LOG = LoggerFactory.getLogger(ClientThing.class);
	private final String ClientName;


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
		ClientName=name;
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
	 * This function handles the property writes from the server
 	 * 
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
	 * @param ClientName Name of the ClientThing
	 * @param PropertyName	Name of the Property to change
	 * @return Returns Object that contains the read value
	 * @throws Exception
	 */
	public Object getClientProperty(String PropertyName) {
		Object var = getProperty(PropertyName).getValue().getValue();	
		LOG.info("{} was set. New Value: {}", this.ClientName, var);
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
		LOG.info("{} was set. New Value: {}", this.ClientName, value);
	}
	
}
