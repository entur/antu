package no.entur.antu.flex.validation.validator;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * XPath's validation tree for flexible transport timetable data, imported through OperatørPortalen.
 */
public class EnturImportFlexTimetableDataValidationTreeFactory extends EnturFlexTimetableDataValidationTreeFactory {
    public EnturImportFlexTimetableDataValidationTreeFactory(OrganisationRepository organisationRepository,
                                                             CommonDataRepository commonDataRepository,
                                                             StopPlaceRepository stopPlaceRepository) {
        super(organisationRepository, commonDataRepository, stopPlaceRepository);
    }

    @Override
    protected ValidationTree getServiceFrameValidationTreeForLineFile(String path) {
        ValidationTree serviceFrameValidationTreeForLineFile = super.getServiceFrameValidationTreeForLineFile(path);
        serviceFrameValidationTreeForLineFile.addValidationRule(
                new ValidateNotExist("lines/Line", "Line not allowed in imported flexible line files", "LINE_10")
        );
        return serviceFrameValidationTreeForLineFile;
    }
}