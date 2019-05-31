package me.nunum.whereami.controller;

import me.nunum.whereami.framework.dto.DTO;
import me.nunum.whereami.model.Device;
import me.nunum.whereami.model.Localization;
import me.nunum.whereami.model.LocalizationSpamReport;
import me.nunum.whereami.model.exceptions.EntityNotFoundException;
import me.nunum.whereami.model.exceptions.ForbiddenEntityDeletionException;
import me.nunum.whereami.model.persistance.DeviceRepository;
import me.nunum.whereami.model.persistance.LocalizationRepository;
import me.nunum.whereami.model.persistance.LocalizationSpamRepository;
import me.nunum.whereami.model.persistance.jpa.DeviceRepositoryJpa;
import me.nunum.whereami.model.persistance.jpa.LocalizationRepositoryJpa;
import me.nunum.whereami.model.persistance.jpa.LocalizationSpamRepositoryJpa;
import me.nunum.whereami.model.request.LocalizationSpamRequest;
import me.nunum.whereami.model.request.NewLocalizationRequest;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LocalizationController implements AutoCloseable {

    private final LocalizationRepository repository;
    private final DeviceRepository deviceRepository;
    private final LocalizationSpamRepository spamRepository;

    /**
     * Constructor
     */
    public LocalizationController() {
        this.repository = new LocalizationRepositoryJpa();
        this.deviceRepository = new DeviceRepositoryJpa();
        this.spamRepository = new LocalizationSpamRepositoryJpa();
    }

    /**
     * Paginate localizations
     *
     * @param principal        See {@link Principal}
     * @param page             Nullable Page
     * @param localizationName Nullable name for search
     * @return List of {@link me.nunum.whereami.model.dto.LocalizationDTO}
     */
    public List<DTO> localizations(final Principal principal,
                                   final Optional<Integer> page,
                                   final Optional<String> localizationName) {

        final Device requester = this.deviceRepository.findOrPersist(principal);

        return this.repository
                .searchWithPagination(requester, page, localizationName)
                .stream()
                .map(e -> e.toDTO(requester))
                .collect(Collectors.toList());
    }

    /**
     * Create new localization
     *
     * @param principal See {@link Principal}
     * @param request   See {@link NewLocalizationRequest}
     * @return See  {@link me.nunum.whereami.model.dto.LocalizationDTO}
     * @throws me.nunum.whereami.model.exceptions.EntityAlreadyExists Try to persist the same localization name
     *                                                                for a given user
     */
    public DTO newLocalization(final Principal principal,
                               final NewLocalizationRequest request) {

        final Device device = this.deviceRepository.findOrPersist(principal);

        return this.repository.save(request.buildLocalization(device)).toDTO(device);
    }


    /**
     * Delete a specific localization, this will cascade, meaning, all associated positions
     * and training requests will be deleted.
     *
     * @param userPrincipal  See {@link Principal}
     * @param localizationId Id of the localization
     * @return See  {@link me.nunum.whereami.model.dto.LocalizationDTO}
     * @throws EntityNotFoundException          Localization does not exists
     * @throws ForbiddenEntityDeletionException Requester is not the owner of entity
     */
    public DTO deleteLocalizationRequest(final Principal userPrincipal, final Long localizationId) {

        final Optional<Localization> someLocalization = this.repository.findById(localizationId);

        if (!someLocalization.isPresent()) {
            throw new EntityNotFoundException(
                    String.format("Spam report for localization %d requested by %s does not exists",
                            localizationId,
                            userPrincipal.getName())
            );
        }

        final Device requester = this.deviceRepository.findOrPersist(userPrincipal);

        final Localization theLocalization = someLocalization.get();

        if (!theLocalization.isOwner(requester)) {

            throw new ForbiddenEntityDeletionException(
                    String.format("Device %s request localization deletion without permission",
                            userPrincipal.getName())
            );
        }

        this.repository.delete(theLocalization);

        return theLocalization.toDTO();
    }


    /**
     * Report a specific localization
     *
     * @param userPrincipal See {@link Principal}
     * @param spamRequest   See {@link LocalizationSpamRequest}
     * @return See {@link me.nunum.whereami.model.dto.LocalizationReportDTO}
     * @throws EntityNotFoundException Localization does not exists
     */
    public DTO newSpamReport(final Principal userPrincipal,
                             final LocalizationSpamRequest spamRequest) {

        final Optional<Localization> someLocalization = this.repository.findById(spamRequest.getId());

        if (!someLocalization.isPresent()) {
            throw new EntityNotFoundException(
                    String.format("Spam report for localization %d requested by %s does not exists",
                            spamRequest.getId(),
                            userPrincipal.getName())
            );
        }

        final Localization theLocalization = someLocalization.get();

        final LocalizationSpamReport localizationSpamReport = this.spamRepository.findOrCreateByLocalization(theLocalization);

        localizationSpamReport.newReport(this.deviceRepository.findOrPersist(userPrincipal));

        return this.spamRepository.save(localizationSpamReport).toDTO();
    }


    /**
     * Retrieve localization by their Id
     *
     * @param localizationId Localization Id
     * @return see {@link Localization}
     * @throws EntityNotFoundException If localization not exists
     */
    public Localization localization(final Long localizationId) {
        final Optional<Localization> someLocalization = this.repository.findById(localizationId);

        if (!someLocalization.isPresent()) {
            throw new EntityNotFoundException(
                    String.format("Localization %d does not exists", localizationId)
            );
        }

        return someLocalization.get();
    }

    @Override
    public void close() throws Exception {
        this.repository.close();
    }
}
