package dk.itu.sass.teame.boundary;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.google.gson.JsonObject;

import dk.itu.sass.teame.controller.FileController;
import dk.itu.sass.teame.entity.File;
import net.jalg.hawkj.AuthHeaderParsingException;
import net.jalg.hawkj.AuthorizationHeader;

@Path("file")
public class FileResource {

	private final String FILE_LOCATION = "\\Irina\\ITU\\Sem_1\\Security\\SASS\\sass-fakestagram\\src\\main\\src\\assets";

	@Inject
	FileController fc;

	@GET
	public Response getFile(@QueryParam("id") String id, @HeaderParam("Server-Authorization") String serverAuth) {
		
		AuthorizationHeader header = null;
		try {
			header = AuthorizationHeader.authorization(serverAuth);
		} catch (AuthHeaderParsingException e1) {
			e1.printStackTrace();
		}	
		
		JsonObject json = new JsonObject();
		
		Long fid = null;
		try {
			fid = Long.parseLong(id);
		} catch (Exception e) {
			json.addProperty("Error", "Wrong file id: " + id);
			return Response.status(Status.BAD_REQUEST).entity(json.toString()).build();
		}

		File file = fc.getFile(fid);
		
		if(file==null)
			Response.status(Status.INTERNAL_SERVER_ERROR).build();
		
		JsonObject jsonResponse = fileToJsonObject(file);
		jsonResponse.addProperty("auth", serverAuth);
		jsonResponse.addProperty("id", header.getId());
		jsonResponse.addProperty("dlg", header.getDlg());
		jsonResponse.addProperty("hash", header.getHash());
		jsonResponse.addProperty("mac", header.getMac());
		jsonResponse.addProperty("ts", header.getTs());

		return Response.ok().entity(jsonResponse.toString()).build();
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadFile(MultipartFormDataInput input, @QueryParam("userid") String userId) {

		JsonObject json = new JsonObject();

		Long uid = null;
		try {
			uid = Long.parseLong(userId);
		} catch (Exception e) {
			json.addProperty("Error", "Wrong user id: " + userId);
			return Response.status(Status.BAD_REQUEST).entity(json.toString()).build();
		}

		Map<String, List<InputPart>> maps = input.getFormDataMap();
		List<InputPart> f = maps.get("file");

		MultivaluedMap<String, String> mv = f.get(0).getHeaders();

		// Hijacking filenames xD Timestamp/uuid to fix - but it's a cool
		// feature.
		String filename = getFileName(mv);

		java.nio.file.Path sti = null;

		try {
			InputStream is = f.get(0).getBody(InputStream.class, null);
			byte[] barr = IOUtils.toByteArray(is);
			java.nio.file.Path p = Paths.get(FILE_LOCATION, filename);// #fail
			Files.deleteIfExists(p); // #fail
			Files.createDirectories(p.getParent());// #fail
			sti = Files.write(p, barr, StandardOpenOption.CREATE_NEW);// #fail
		} catch (Exception e) {
			json.addProperty("Error", "Couldn't write file");
			json.addProperty("Trace", e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(json.toString()).build();
		}

		File file = fc.uploadFile(uid, sti);

		return Response.created(file.getPath().toUri()).entity(fileToJsonObject(file).toString()).build();
	}

	private String getFileName(MultivaluedMap<String, String> header) {

		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

		for (String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {

				String[] name = filename.split("=");

				String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}
	
	private JsonObject fileToJsonObject(File f) {
		JsonObject json = new JsonObject();
		json.addProperty("id", f.getId());
		json.addProperty("userId", f.getUserId());
		json.addProperty("path", f.getPath().toString());
		json.addProperty("timestamp", f.getTimestamp().toString());
		return json;
	}
	
	@GET
	@Path("getallimages")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllImagesFromUser(@QueryParam("id") String id){
		
		if(id.isEmpty() || id == null) {
			Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		String json = fc.getFilesByUser(Long.parseLong(id));
		return Response.status(Response.Status.ACCEPTED).entity(json).build();		
	}
	
	@GET
	@Path("shareimage")
	public Response shareImage(@QueryParam("imageId") String imageId, 
							   @QueryParam("author") String authorId, 
							   @QueryParam("victim") String shareWithId){
		
		JsonObject o = new JsonObject();
		o.addProperty("author", authorId);
		o.addProperty("sharedwith", shareWithId);
		o.addProperty("imageid", imageId);
		
		if((imageId.isEmpty()  || imageId == null ) || 
		   (authorId.isEmpty() || authorId == null ) || 
		   (shareWithId.isEmpty() || shareWithId == null) ){
			Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		boolean b = fc.shareImage(Long.parseLong(imageId), Long.parseLong(authorId), Long.parseLong(shareWithId));
		
		if(b) return Response.status(Response.Status.ACCEPTED).entity(o).build();
		else return Response.status(Response.Status.BAD_REQUEST).build();
	}

}
