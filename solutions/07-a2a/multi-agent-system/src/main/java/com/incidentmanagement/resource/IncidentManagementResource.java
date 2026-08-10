package com.incidentmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.langchain4j.data.message.ImageContent;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

import com.incidentmanagement.service.IncidentManagementService;

/**
 * REST resource for incident management operations.
 * Uses blocking processing for AI agent workflows.
 */
@Path("/incident-management")
public class IncidentManagementResource {

    @Inject
    IncidentManagementService incidentManagementService;

    /**
     * Process an incident report.
     * This is a blocking operation due to AI agent processing.
     *
     * @param incidentNumber The incident number
     * @param report Optional report about the incident
     * @param logImage Optional screenshot of logs (multipart form data)
     * @return Uni that completes with the result
     */
    @POST
    @Path("/process/{incidentNumber}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Blocking
    public Uni<Response> processIncident(Integer incidentNumber, @RestForm String report, @RestForm FileUpload logImage) {
        ImageContent imageContent = toImageContent(logImage);

        return incidentManagementService.processIncident(incidentNumber, report != null ? report : "", imageContent)
            .onItem().transform(result -> Response.ok(result).build())
            .onFailure().recoverWithItem(e -> {
                Log.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Error processing incident: " + e.getMessage())
                        .build();
            });
    }

    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    public Response report() {
        return Response.ok(incidentManagementService.report()).build();
    }

    private ImageContent toImageContent(FileUpload fileUpload) {
        if (fileUpload == null || fileUpload.filePath() == null) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(fileUpload.filePath());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mimeType = fileUpload.contentType();
            return new ImageContent(base64, mimeType);
        } catch (IOException e) {
            Log.error("Failed to read uploaded log image", e);
            return null;
        }
    }
}
