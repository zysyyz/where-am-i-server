package me.nunum.whereami.facade;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import me.nunum.whereami.controller.LocalizationController;
import me.nunum.whereami.framework.dto.DTO;
import me.nunum.whereami.framework.response.TheMediaType;
import me.nunum.whereami.model.exceptions.*;
import me.nunum.whereami.model.request.NewLocalizationRequest;
import me.nunum.whereami.model.request.NewPredictionRequest;
import me.nunum.whereami.model.request.UpdatePredictionRequest;

import javax.annotation.security.PermitAll;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Api("localization")
@Path("localization")
@Singleton
@PermitAll
public class LocalizationResource {

    @Context
    SecurityContext securityContext;

    private static final Logger LOGGER = Logger.getLogger("LocalizationResource");

    @GET
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Produces({TheMediaType.APPLICATION_JSON})
    public Response retrieveLocalizations(@QueryParam("page") Integer page,
                                          @QueryParam("name") String localizationName,
                                          @QueryParam("trained") String trained,
                                          @QueryParam("owner") Boolean onlyUserLocalizations) {

        try (final LocalizationController controller = new LocalizationController()) {
            final List<DTO> dtos = controller.localizations(securityContext.getUserPrincipal(),
                    Optional.ofNullable(page),
                    Optional.ofNullable(localizationName),
                    Optional.ofNullable(trained),
                    Optional.ofNullable(onlyUserLocalizations)
            );

            return Response.ok(dtos.stream().map(DTO::dtoValues).collect(Collectors.toList())).build();

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, "Unable to response with localizations", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({TheMediaType.APPLICATION_JSON})
    public Response newLocalization(@Valid NewLocalizationRequest localizationRequest) {

        try (final LocalizationController controller = new LocalizationController()) {

            final DTO localization = controller.newLocalization(securityContext.getUserPrincipal(), localizationRequest);

            return Response.ok(localization.dtoValues()).build();

        } catch (EntityAlreadyExists e) {

            LOGGER.log(Level.SEVERE, "Localization already exists", e);

            return Response.status(Response.Status.CONFLICT).build();

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, "Unable to create new localization", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Path("{id}")
    @Produces({TheMediaType.APPLICATION_JSON})
    public Response deleteLocalization(@PathParam("id") Long id) {

        try (final LocalizationController controller = new LocalizationController()) {

            final DTO localizationDto = controller.deleteLocalizationRequest(securityContext.getUserPrincipal(), id);

            return Response.ok(localizationDto.dtoValues()).build();

        } catch (EntityNotFoundException e) {

            LOGGER.log(Level.SEVERE, "Localization not found", e);

            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (ForbiddenEntityDeletionException e) {

            LOGGER.log(Level.SEVERE, "Localization not belongs to requester", e);

            return Response.status(Response.Status.FORBIDDEN).build();

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, "Unable to create new localization", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Path("{id}/predict")
    @Produces({TheMediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response startPrediction(@PathParam("id") Long id, NewPredictionRequest request) {

        try (final LocalizationController controller = new LocalizationController()) {

            final List<DTO> dtos = controller.requestNewPrediction(securityContext.getUserPrincipal(), id, request);

            return Response.ok(dtos.stream().map(DTO::dtoValues).collect(Collectors.toList())).build();

        } catch (EntityNotFoundException e) {

            LOGGER.log(Level.SEVERE, "Localization not found", e);

            return Response.status(Response.Status.NOT_FOUND).build();

        } catch (ForbiddenEntityAccessException e) {

            LOGGER.log(Level.SEVERE, "Localization not belongs to requester", e);

            return Response.status(Response.Status.FORBIDDEN).build();

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, "Some error", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PUT
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Path("{id}/predict/{prediction}")
    @Produces({TheMediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response predictionFeedbackRequest(@PathParam("id") Long localizationId,
                                              @PathParam("prediction") Long predictionId,
                                              UpdatePredictionRequest request) {

        try (final LocalizationController controller = new LocalizationController()) {


            final DTO dto = controller.processPredictionFeedback(securityContext.getUserPrincipal(),
                    localizationId,
                    predictionId,
                    request);

            return Response.ok().entity(dto.dtoValues()).build();

        } catch (EntityNotFoundException e) {

            LOGGER.log(Level.SEVERE, "Localization not found", e);

            return Response.status(Response.Status.NOT_FOUND).build();

        } catch (ForbiddenEntityModificationException e) {

            LOGGER.log(Level.SEVERE, "Localization not belongs to prediction", e);

            return Response.status(Response.Status.FORBIDDEN).build();

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, "Some error", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Path("{id}/spam")
    public LocalizationReportResource reportResource(@PathParam("id") Long localizationId) throws Exception {
        try (final LocalizationController controller = new LocalizationController()) {
            final Principal principal = securityContext.getUserPrincipal();
            return new LocalizationReportResource(principal, controller.localization(principal, localizationId), controller);
        }
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Path("{id}/train")
    public TrainResource trainResource(@PathParam("id") Long id) throws Exception {
        try (final LocalizationController controller = new LocalizationController()) {
            return new TrainResource(controller.localization(securityContext.getUserPrincipal(), id), securityContext);
        }
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-APP", value = "App Instance", required = true, dataType = "string", paramType = "header")
    })
    @Path("{id}/position")
    public PositionResource positionResource(@PathParam("id") Long id) throws Exception {
        try (final LocalizationController controller = new LocalizationController()) {
            return new PositionResource(controller.localization(securityContext.getUserPrincipal(), id), securityContext);
        }
    }
}
