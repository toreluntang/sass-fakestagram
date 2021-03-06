package dk.itu.sass.teame.boundary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

import dk.itu.sass.teame.controller.AccountController;
import dk.itu.sass.teame.controller.FileController;
import dk.itu.sass.teame.entity.Account;
import dk.itu.sass.teame.entity.File;

@Path("protected/file")
public class FileResource {

	// private final String FILE_LOCATION = "";

	// private final String FILE_LOCATION =
	// "\\Irina\\ITU\\Sem_1\\Security\\wildfly-10.0.0.Final\\Pictures";
	// private final String FILE_LOCATION =
	// "/home/neoot/wildfly-10.0.0.Final/Pictures";
	// private final String FILE_LOCATION = "/var/www/html/";
	private final String FILE_LOCATION = "/Users/Alexander/Code/Servers/wildfly-10-sass/bin/pictures";

	@Inject
	FileController fc;

	@GET
	public Response getFile(@QueryParam("id") String id) {
		try {
			JsonObject json = new JsonObject();
			Long fid = null;
			try {
				fid = Long.parseLong(id);
			} catch (Exception e) {
				json.addProperty("Error", "Wrong file id: " + id);
				return Response.status(Status.BAD_REQUEST).entity(json.toString()).build();
			}
			File file = fc.getFile(fid);

			if (file == null)
				Response.status(Status.INTERNAL_SERVER_ERROR).build();

			JsonObject jsonResponse = fileToJsonObject(file);
			return Response.ok().entity(jsonResponse.toString()).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadFile(MultipartFormDataInput input) {

		Map<String, List<InputPart>> maps = input.getFormDataMap();
		
		String userId = null;
		try {
			userId = maps.get("userid").get(0).getBodyAsString();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			JsonObject json = new JsonObject();
			System.out.println("ALEX2");
			Long uid = null;
			try {
				uid = Long.parseLong(userId);
			} catch (Exception e) {
				json.addProperty("Error", "Wrong user id: " + userId);
				return Response.status(Status.BAD_REQUEST).entity(json.toString()).build();
			}
			
			Account user = AccountController.getAccountById(uid);

			List<InputPart> f = maps.get("file");

			MultivaluedMap<String, String> mv = f.get(0).getHeaders();

			// Hijacking filenames xD Timestamp/uuid to fix - but it's a cool
			// feature.
			String filename = getFileName(mv);

			java.nio.file.Path sti = null;
			if (filename.contains(".exe") || filename.contains(".sh") || filename.contains(".class")
					|| filename.contains(".jsp")) {
				return Response.status(Status.FORBIDDEN).build();
			}
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
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
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
		json.addProperty("path", f.getPath().getFileName().toString());
		json.addProperty("timestamp", f.getTimestamp().toString());
		return json;
	}

	@GET
	@Path("getallimages/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllImagesFromUser(@PathParam("id") String id) {
		try {
			if (id.isEmpty() || id == null) {
				Response.status(Response.Status.BAD_REQUEST).build();
			}

			String json = fc.getFilesByUser(Long.parseLong(id));
			return Response.status(Response.Status.ACCEPTED).entity(json).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("shareimage")
	public Response shareImage(@FormParam("imageId") String imageId, @FormParam("author") String authorId,
			@FormParam("victim") String shareWithId) {
		try {
			JsonObject o = new JsonObject();
			o.addProperty("author", authorId);
			o.addProperty("sharedwith", shareWithId);
			o.addProperty("imageid", imageId);

			if ((imageId.isEmpty() || imageId == null) || (authorId.isEmpty() || authorId == null)
					|| (shareWithId.isEmpty() || shareWithId == null)) {
				Response.status(Response.Status.BAD_REQUEST).build();
			}
			long accountId = Long.parseLong(authorId);
			Account acc = AccountController.getAccountById(accountId);
			boolean b = fc.shareImage(Long.parseLong(imageId), accountId, Long.parseLong(shareWithId));

			if (b)
				return Response.status(Response.Status.ACCEPTED).entity(o.toString()).build();
			else
				return Response.status(Response.Status.BAD_REQUEST).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
