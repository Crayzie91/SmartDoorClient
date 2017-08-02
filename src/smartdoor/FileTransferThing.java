package smartdoor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.filetransfer.FileTransferVirtualThing;
import com.thingworx.relationships.RelationshipTypes.ThingworxEntityTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.ImagePrimitive;
import com.thingworx.types.primitives.StringPrimitive;

public class FileTransferThing {

	private static final Logger LOG = LoggerFactory.getLogger(FileTransferThing.class);
	
	private final FileTransferVirtualThing FileThing;
	private final ConnectedThingClient client;
	private final String FileThingName;
	
	public FileTransferThing(String name, ConnectedThingClient client) throws Exception {
		FileThing = new FileTransferVirtualThing(name, "Smart Door Project Repository", client);
		FileThingName = name;
		this.client = client;
		client.bindThing(FileThing);
	}
	
	/**
	 * This function creates a folder in the FileRepository.
	 * 
	 * @param path Path of folder
	 * @return True if folder was created.
	 */
	public boolean createFolder(String path){
		ValueCollection payload = new ValueCollection();
		payload.put("path", new StringPrimitive(path));
		try {	
			client.invokeService(ThingworxEntityTypes.Things, FileThingName, "CreateFolder", payload, 5000);
			LOG.info("Folder {} created.",path);
		} catch (Exception e) {
			LOG.error("Folder already exists. Error: {}",e);
			return false;
		}
		return true;
	}
	
	/**
	 * This function deletes a folder in the FileRepository.
	 * 
	 * @param path Path of folder
	 * @return True if folder was deleted.
	 */
	public boolean deleteFolder(String path){
		ValueCollection payload = new ValueCollection();
		payload.put("path", new StringPrimitive(path));
		try {	
			client.invokeService(ThingworxEntityTypes.Things, FileThingName, "DeleteFolder", payload, 5000);
			LOG.info("Folder {} deleted.",path);
		} catch (Exception e) {
			LOG.error("Folder doesn't exist. Error: {}",e);
			return false;
		}
		return true;
	}
	
	/**
	 * This functions uploads an image to the FileRepository.
	 *  
	 * @param path2repository Path the image should be uploaded to in the respository.
	 * @param path2image Path to image.
	 * @return True if image was uploaded.
	 */
	public boolean uploadImage(String path2repository, String path2image) {
		byte[] bin = Img2Bin(path2image);
		
		ValueCollection payload = new ValueCollection();
		payload.put("content", new ImagePrimitive(bin));
		payload.put("path", new StringPrimitive(path2repository));
		
		try {
			client.invokeService(ThingworxEntityTypes.Things, "SmartDoorRepository", "SaveImage", payload, 5000);
			LOG.info("Image {} was uploaded to {}.",path2image, path2repository);
		} catch (Exception e) {
			LOG.error("Image couldn't be uploaded. Error: {}",e);
			return false;
		}
		return true;
	}
	
	/**
	 * This functions uploads an image to the FileRepository.
	 * 
	 * @param path2repository Path of Image to download from the repository.
	 * @param path2save Path to save the downloaded image.
	 * @return File handle of the image.
	 */
	public File downloadImage(String path2repository, String path2save) {	
		File imgfile = new File(path2save);
		
		ValueCollection payload = new ValueCollection();
		payload.put("path", new StringPrimitive(path2repository));
		
		try {
			InfoTable info = client.invokeService(ThingworxEntityTypes.Things, "SmartDoorRepository", "LoadImage", payload, 5000);
			ImagePrimitive bin = (ImagePrimitive) info.getLastRow().getPrimitive("Content");
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(bin.getValue()));		
			imgfile.getParentFile().mkdirs();
			imgfile.createNewFile();
			ImageIO.write(img, "jpg", new File(path2save));
			LOG.info("Image {} was downloaded to {}.",path2repository, path2save);
		} catch (Exception e) {
			LOG.error("Image couldn't be uploaded. Error: {}",e);
		}
		return imgfile;
	}
	
	public String getLinktoFile (String path2repository, String NameofFile) {
		String link = null;
		
		ValueCollection payload = new ValueCollection();
		payload.put("path", new StringPrimitive(path2repository));
		
		try {
			InfoTable info = client.invokeService(ThingworxEntityTypes.Things, "SmartDoorRepository", "GetFileListingWithLinks", payload, 5000);
			link = (String) info.getLastRow().getValue("downloadLink");
			LOG.error("Link to File {} was fetched. {}",NameofFile,link);
		} catch (Exception e) {
			LOG.error("Link couldn't be retrieved. Error: {}",e);
		}
		
		return link;
	}
	
	/**
	 * This function transforms an image to binary.
	 * 
	 * @param path Path to image that should be converted.
	 * @return Binary Byte Array of image.
	 * @throws IOException
	 */
	public byte[] Img2Bin (String path){	    
		ByteArrayOutputStream bin = new ByteArrayOutputStream();
		
		try {
			BufferedImage image = ImageIO.read(new File(path));
			ImageIO.write(image, "jpg", bin);
		} catch (IOException e) {
			LOG.error("Img2Bin didn't succeed. Error: {}",e);
			return null;
		}

	    return bin.toByteArray();
	}

}
