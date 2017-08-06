package smartdoor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.ConnectionException;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.relationships.RelationshipTypes.ThingworxEntityTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.NumberPrimitive;
import com.thingworx.types.primitives.StringPrimitive;

public class ClientThingClient extends ConnectedThingClient {

	private static final Logger LOG = LoggerFactory.getLogger(ClientThingClient.class);
	
	private static String ThingName = "ClientThing";
	private static int ID;
	private static String RepositoryName = "SmartDoorRepository";

	public ClientThingClient(ClientConfigurator config) throws Exception {
		super(config);
	}
	
	/**
	 * This functions calls the "CreateNewThing" service of the server to create a new thing on the platform.
	 * 
	 * @param client Client handle used to call the service
	 * @throws TimeoutException
	 * @throws ConnectionException
	 * @throws Exception
	 */
	public static void CreateNewClientThing(ConnectedThingClient client) throws TimeoutException, ConnectionException, Exception {
		//Get ID for new Client
		ID = (int)client.invokeService(ThingworxEntityTypes.Things, "ServerThing", "addClient", new ValueCollection(), 10000).getLastRow().getValue("result");
		ID++;
		ThingName=ThingName+"_"+ID;
		
		//Build ValueCollection of parameters
		ValueCollection payload = new ValueCollection();
		payload.put("name", new StringPrimitive(ThingName));
		payload.put("description", new StringPrimitive("Remote created ClientThing"));
		payload.put("thingTemplateName", new StringPrimitive("ClientThingTemplate"));
		payload.put("ID", new IntegerPrimitive(ID));
		
		//Call CreateThing Service from the platforms EntityServices
		client.invokeService(ThingworxEntityTypes.Resources, "EntityServices", "CreateThing", payload, 10000);
		LOG.info("{} was created.", ThingName);
		
		//Enable and restart thing to set it active
		client.invokeService(ThingworxEntityTypes.Things, ThingName, "EnableThing", payload, 10000);
		client.invokeService(ThingworxEntityTypes.Things, ThingName, "RestartThing", payload, 10000);
		//Set ClientThing ID to identify the client
		client.writeProperty(ThingworxEntityTypes.Things, ThingName, "ID", new IntegerPrimitive(ID), 10000);
	}
	
	/**
	 * Main Routine of the client thing.
	 * 
	 * @param args CLI arguments
	 * @see https://developer.thingworx.com/resources/guides/thingworx-java-sdk-tutorial/understanding-example-client-connection
	 */
	public static void main(String[] args) {
	
		ClientConfigurator config = new ClientConfigurator();
		String uri="http://34.227.165.169:80/Thingworx/WS";
		String AppKey="ce22e9e4-2834-419c-9656-ef9f844c784c";
	
		// Set the URI of the server that we are going to connect to
		config.setUri(uri);
		
		// Set the ApplicationKey. This will allow the client to authenticate with the server.
		// It will also dictate what the client is authorized to do once connected.
		config.setAppKey(AppKey);
		
		// This will allow us to test against a server using a self-signed certificate.
		// This should be removed for production systems.
		config.ignoreSSLErrors(true); // All self signed certs
	
		try {						
			// Create the Edge Client for the ClientThing.
			ClientThingClient client = new ClientThingClient(config);
			
			// Connect an authenticate to the server by starting the client.
			client.start();
			
			// Wait for the client to connect.
			if (client.waitForConnection(30000)) {
				LOG.info("The client is now connected.");
				
				//Create a new ClientThing on the Thingworx platform
				CreateNewClientThing(client);
								
				// Create a new VirtualThing to connect to a thing on the Thingworx platform
				ClientThing thing = new ClientThing(ThingName, "A basic client thing", client);
				
				/* Create the FileTransferThing to handle data in the repository
				FileTransferThing transfer = new FileTransferThing(RepositoryName, client);
				String timeStamp = new SimpleDateFormat("ddMM_HHmm").format(Calendar.getInstance().getTime());			
				transfer.createFolder("/"+ThingName);
				transfer.uploadImage("/"+ThingName+"/"+timeStamp+".jpg", "./Images/Test.jpg");
				File image = transfer.downloadImage("/"+ThingName+"/"+timeStamp+".jpg", "./Images/Downloads/"+timeStamp+".jpg");
				transfer.getLinktoFile("/"+ThingName, "/"+ThingName+"/"+timeStamp+".jpg");
				transfer.deleteFolder("/");*/
						
				// Bind the VirtualThing to the client. This will tell the Platform that
				// the RemoteThing is now connected and that it is ready to receive requests.
				client.bindThing(thing);

				while (!client.isShutdown()) {
					
					Thread.sleep(1000);
					
					// This loop iterates to all VirtualThings connected to the Client and starts 
					// a routine functions.
					for (VirtualThing vt : client.getThings().values()) {
						vt.processScanRequest();
					}
				}
				
			} else {
				// Log this as a warning. In production the application could continue
				// to execute, and the client would attempt to reconnect periodically.
				LOG.warn("Client did not connect within 30 seconds. Exiting");
			}
			
		} catch (Exception e) {
			LOG.error("An exception occured during execution.", e);
		}	
		LOG.info("SimpleThingClient is done. Exiting");
	}
}
