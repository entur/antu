package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.validator.xpath.rules.ValidateAllowedBookingAccessProperty;
import no.entur.antu.validator.xpath.rules.ValidateAllowedBookingMethodProperty;
import no.entur.antu.validator.xpath.rules.ValidateAllowedBookingWhenProperty;
import no.entur.antu.validator.xpath.rules.ValidateAllowedBuyWhenProperty;
import no.entur.antu.validator.xpath.rules.ValidateAllowedCodespaces;
import no.entur.antu.validator.xpath.rules.ValidateAllowedFlexibleLineType;
import no.entur.antu.validator.xpath.rules.ValidateAllowedFlexibleServiceType;
import no.entur.antu.validator.xpath.rules.ValidateAtLeastOne;
import no.entur.antu.validator.xpath.rules.ValidateAuthorityId;
import no.entur.antu.validator.xpath.rules.ValidateExactlyOne;
import no.entur.antu.validator.xpath.rules.ValidateMandatoryBookingProperty;
import no.entur.antu.validator.xpath.rules.ValidateNSRCodespace;
import no.entur.antu.validator.xpath.rules.ValidateNotExist;
import no.entur.antu.validator.xpath.rules.ValidatedAllowedTransportMode;
import no.entur.antu.validator.xpath.rules.ValidatedAllowedTransportSubMode;
import no.entur.antu.xml.XMLParserUtil;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static no.entur.antu.xml.XMLParserUtil.selectNodeSet;

/**
 * Run XPath validation rules against the dataset.
 */
public class XPathValidator {

    private final OrganisationRepository organisationRepository;
    private final ValidationTree topLevelValidationTree;

    public XPathValidator(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
        this.topLevelValidationTree = getTopLevelValidationTree();
    }

    public List<ValidationReportEntry> validate(String codespace, String fileName, byte[] content) throws XMLStreamException, SaxonApiException {
        XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
        ValidationContext validationContext = new ValidationContext(document, XMLParserUtil.getXPathCompiler(), codespace, fileName);
        return this.validate(validationContext);

    }

    public List<ValidationReportEntry> validate(ValidationContext validationContext) {
        return topLevelValidationTree.validate(validationContext);
    }

    private ValidationTree getTopLevelValidationTree() {
        ValidationTree validationTree = new ValidationTree("PublicationDelivery", "/");
        validationTree.addSubTree(getCommonFileValidationTree());
        validationTree.addSubTree(getLineFileValidationTree());
        return validationTree;
    }

    private ValidationTree getCommonFileValidationTree() {
        ValidationTree commonFileValidationTree = new ValidationTree("Common file", "/", validationContext -> validationContext.getFileName().startsWith("_"));
        commonFileValidationTree.addValidationRule(new ValidateAllowedCodespaces());
        commonFileValidationTree.addSubTree(getCompositeFrameValidationTreeForCommonFile());
        commonFileValidationTree.addSubTree(getSingleFramesValidationTreeForCommonFile());

        return commonFileValidationTree;
    }

    private ValidationTree getLineFileValidationTree() {
        ValidationTree lineFileValidationTree = new ValidationTree("Line file", "/", validationContext -> !validationContext.getFileName().startsWith("_"));
        lineFileValidationTree.addValidationRule(new ValidateAllowedCodespaces());
        lineFileValidationTree.addSubTree(getCompositeFrameValidationTreeForLineFile());
        lineFileValidationTree.addSubTree(getSingleFramesValidationTreeForLineFile());
        return lineFileValidationTree;
    }

    private ValidationTree getSingleFramesValidationTreeForCommonFile() {
        ValidationTree validationTree = new ValidationTree("Single frames in common file", "PublicationDelivery/dataObjects",
                validationContext -> selectNodeSet("CompositeFrame", validationContext.getxPathCompiler(), validationContext.getXmlNode()).isEmpty());


        validationTree.addValidationRule(new ValidateNotExist("SiteFrame", "Unexpected element SiteFrame. It will be ignored", "Composite Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("TimetableFrame", "Timetable frame not allowed in common files", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateAtLeastOne("ServiceFrame[validityConditions] | ServiceCalendarFrame[validityConditions]", "Neither ServiceFrame nor ServiceCalendarFrame defines ValidityConditions", "Single Frames", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateNotExist("ResourceFrame[not(validityConditions) and count(//ResourceFrame) > 1]", "Multiple ResourceFrames without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("ServiceFrame[not(validityConditions) and count(//ServiceFrame) > 1]", "Multiple ServiceFrames without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("ServiceCalendarFrame[not(validityConditions) and count(//ServiceCalendarFrame) > 1]", "Multiple ServiceCalendarFrames without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));


        validationTree.addSubTree(getResourceFrameValidationTree("ResourceFrame"));
        validationTree.addSubTree(getServiceFrameValidationTreeForCommonFile("ServiceFrame"));
        validationTree.addSubTree(getServiceCalendarFrameValidationTree("ServiceCalendarFrame"));
        validationTree.addSubTree(getVehicleScheduleFrameValidationTree("VehicleScheduleFrame"));

        return validationTree;
    }

    private ValidationTree getCompositeFrameValidationTreeForLineFile() {
        ValidationTree compositeFrameValidationTree = new ValidationTree("Composite frame in line file", "PublicationDelivery/dataObjects/CompositeFrame");

        compositeFrameValidationTree.addValidationRules(getCompositeFrameBaseValidationRules());

        compositeFrameValidationTree.addSubTree(getResourceFrameValidationTree("frames/ResourceFrame"));
        compositeFrameValidationTree.addSubTree(getServiceCalendarFrameValidationTree("frames/ServiceCalendarFrame"));
        compositeFrameValidationTree.addSubTree(getVehicleScheduleFrameValidationTree("frames/VehicleScheduleFrame"));

        compositeFrameValidationTree.addSubTree(getServiceFrameValidationTreeForLineFile("frames/ServiceFrame"));
        compositeFrameValidationTree.addSubTree(getTimetableFrameValidationTree("frames/TimetableFrame"));

        return compositeFrameValidationTree;
    }

    private ValidationTree getSingleFramesValidationTreeForLineFile() {
        ValidationTree validationTree = new ValidationTree("Single frames in line file", "PublicationDelivery/dataObjects",
                validationContext ->
                        selectNodeSet("CompositeFrame", validationContext.getxPathCompiler(), validationContext.getXmlNode()).isEmpty());

        validationTree.addValidationRule(new ValidateExactlyOne("ResourceFrame", "Exactly one ResourceFrame should be present", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("SiteFrame", "Unexpected element SiteFrame. It will be ignored", "Single Frames", ValidationReportEntrySeverity.WARNING));

        validationTree.addValidationRule(new ValidateAtLeastOne("ServiceFrame[validityConditions] | ServiceCalendarFrame[validityConditions] | TimetableFrame[validityConditions]", "Neither ServiceFrame, ServiceCalendarFrame nor TimetableFrame defines ValidityConditions", "Single Frames", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateNotExist("ServiceFrame[not(validityConditions) and count(//ServiceFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("ServiceCalendarFrame[not(validityConditions) and count(//ServiceCalendarFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("TimetableFrame[not(validityConditions) and count(//TimetableFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("VehicleScheduleFrame[not(validityConditions) and count(//VehicleScheduleFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));


        validationTree.addSubTree(getResourceFrameValidationTree("ResourceFrame"));
        validationTree.addSubTree(getServiceCalendarFrameValidationTree("ServiceCalendarFrame"));
        validationTree.addSubTree(getTimetableFrameValidationTree("TimetableFrame"));
        validationTree.addSubTree(getVehicleScheduleFrameValidationTree("VehicleScheduleFrame"));

        validationTree.addSubTree(getServiceFrameValidationTreeForLineFile("ServiceFrame"));

        return validationTree;
    }

    private ValidationTree getTimetableFrameValidationTree(String path) {
        ValidationTree validationTree = new ValidationTree("Timetable frame", path);

        validationTree.addValidationRule(new ValidateAtLeastOne("vehicleJourneys/ServiceJourney", "There should be at least one ServiceJourney", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/calls", "Element Call not allowed", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[not(passingTimes)]", "The ServiceJourney does not specify any TimetabledPassingTimes", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/passingTimes/TimetabledPassingTime[not(DepartureTime) and not(ArrivalTime)]", "TimetabledPassingTime contains neither DepartureTime nor ArrivalTime", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[not(passingTimes/TimetabledPassingTime[1]/DepartureTime)]", "All TimetabledPassingTime except last call must have DepartureTime", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[count(passingTimes/TimetabledPassingTime[last()]/ArrivalTime) = 0]", "Last TimetabledPassingTime must have ArrivalTime", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/passingTimes/TimetabledPassingTime[DepartureTime = ArrivalTime]", "ArrivalTime is identical to DepartureTime", "Timetable Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/passingTimes/TimetabledPassingTime[not(@id)]", "Missing id on TimetabledPassingTime", "Timetable Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/passingTimes/TimetabledPassingTime[not(@version)]", "Missing version on TimetabledPassingTime", "Timetable Frame", ValidationReportEntrySeverity.WARNING));


        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[not(JourneyPatternRef)]", "The ServiceJourney does not refer to a JourneyPattern", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[(TransportMode and not(TransportSubmode))  or (not(TransportMode) and TransportSubmode)]", "If overriding Line TransportMode or TransportSubmode on a ServiceJourney, both elements must be present", "Timetable Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[not(OperatorRef) and not(//ServiceFrame/lines/*[self::Line or self::FlexibleLine]/OperatorRef)]", "Missing OperatorRef on ServiceJourney (not defined on Line)", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[not(dayTypes/DayTypeRef) and not(@id=//TimetableFrame/vehicleJourneys/DatedServiceJourney/ServiceJourneyRef/@ref)]", "The ServiceJourney does not refer to DayTypes nor DatedServiceJourneys", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[dayTypes/DayTypeRef and @id=//TimetableFrame/vehicleJourneys/DatedServiceJourney/ServiceJourneyRef/@ref]", "The ServiceJourney references both DayTypes and DatedServiceJourneys", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("for $a in vehicleJourneys/ServiceJourney return if(count(//ServiceFrame/journeyPatterns/*[@id = $a/JourneyPatternRef/@ref]/pointsInSequence/StopPointInJourneyPattern) != count($a/passingTimes/TimetabledPassingTime)) then $a else ()", "ServiceJourney does not specify passing time for all StopPointInJourneyPattern", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney[@id = preceding-sibling::ServiceJourney/@id]", "ServiceJourney is repeated with a different version", "Timetable Frame", ValidationReportEntrySeverity.WARNING));

        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DatedServiceJourney[not(OperatingDayRef)]", "Missing OperatingDayRef on DatedServiceJourney", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DatedServiceJourney[not(ServiceJourneyRef)]", "Missing ServiceJourneyRef on DatedServiceJourney", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DatedServiceJourney[count(ServiceJourneyRef) > 1]", "Multiple ServiceJourneyRef on DatedServiceJourney", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DatedServiceJourney[@id = preceding-sibling::DatedServiceJourney/@id]", "DatedServiceJourney is repeated with a different version", "Timetable Frame", ValidationReportEntrySeverity.WARNING));

        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DeadRun[not(passingTimes)]", "The Dead run does not reference passing times", "Timetable Frame", ValidationReportEntrySeverity.INFO));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DeadRun[not(JourneyPatternRef)]", "The Dead run does not reference a journey pattern", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/DeadRun[not(dayTypes/DayTypeRef)]", "The Dead run does not reference day types", "Timetable Frame", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/FlexibleServiceProperties[not(@id)]", "Missing id on FlexibleServiceProperties", "Timetable Frame", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("vehicleJourneys/ServiceJourney/FlexibleServiceProperties[not(@version)]", "Missing version on FlexibleServiceProperties", "Timetable Frame", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateAllowedFlexibleServiceType());
        validationTree.addValidationRule(new ValidateAllowedBookingWhenProperty("vehicleJourneys/ServiceJourney/FlexibleServiceProperties"));
        validationTree.addValidationRule(new ValidateAllowedBuyWhenProperty("vehicleJourneys/ServiceJourney/FlexibleServiceProperties"));
        validationTree.addValidationRule(new ValidateAllowedBookingMethodProperty("vehicleJourneys/ServiceJourney/FlexibleServiceProperties"));
        validationTree.addValidationRule(new ValidateAllowedBookingAccessProperty("vehicleJourneys/ServiceJourney/FlexibleServiceProperties"));

        validationTree.addValidationRule(new ValidateNotExist("journeyInterchanges/ServiceJourneyInterchange[Advertised or Planned]", "The 'Planned' and 'Advertised' properties of an Interchange should not be specified", "Timetable Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("journeyInterchanges/ServiceJourneyInterchange[Guaranteed='true' and  (MaximumWaitTime='PT0S' or MaximumWaitTime='PT0M') ]", "Guaranteed Interchange should not have a maximum wait time value of zero", "Timetable Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("journeyInterchanges/ServiceJourneyInterchange[MaximumWaitTime > xs:dayTimeDuration('PT1H')]", "The maximum waiting time after planned departure for the interchange consumer journey (MaximumWaitTime) should not be longer than one hour", "Timetable Frame", ValidationReportEntrySeverity.WARNING));

        validationTree.addSubTree(getNoticesValidationTree());
        validationTree.addSubTree(getNoticeAssignmentsValidationTree());

        return validationTree;
    }

    private ValidationTree getCompositeFrameValidationTreeForCommonFile() {
        ValidationTree compositeFrameValidationTree = new ValidationTree("Composite frame in common file", "PublicationDelivery/dataObjects/CompositeFrame");

        compositeFrameValidationTree.addValidationRules(getCompositeFrameBaseValidationRules());
        compositeFrameValidationTree.addValidationRule(new ValidateNotExist("frames/TimetableFrame", "Timetable frame not allowed in common files", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        compositeFrameValidationTree.addSubTree(getResourceFrameValidationTree("frames/ResourceFrame"));
        compositeFrameValidationTree.addSubTree(getServiceCalendarFrameValidationTree("frames/ServiceCalendarFrame"));
        compositeFrameValidationTree.addSubTree(getVehicleScheduleFrameValidationTree("frames/VehicleScheduleFrame"));

        compositeFrameValidationTree.addSubTree(getServiceFrameValidationTreeForCommonFile("frames/ServiceFrame"));

        return compositeFrameValidationTree;
    }

    /**
     * CompositeFrame validation rules that apply both to Line files and common files.
     *
     * @return
     */
    private List<ValidationRule> getCompositeFrameBaseValidationRules() {
        List<ValidationRule> validationRules = new ArrayList<>();
        validationRules.add(new ValidateNotExist("frames/SiteFrame", "Unexpected element SiteFrame. It will be ignored", "Composite Frame", ValidationReportEntrySeverity.WARNING));

        validationRules.add(new ValidateNotExist(".[not(validityConditions)]", "A CompositeFrame must define a ValidityCondition valid for all data within the CompositeFrame", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("frames//validityConditions", "ValidityConditions defined inside a frame inside a CompositeFrame", "Composite Frame", ValidationReportEntrySeverity.WARNING));
        validationRules.add(new ValidateNSRCodespace());

        validationRules.add(new ValidateNotExist("//ValidBetween[not(FromDate) and not(ToDate)]", "ValidBetween missing either or both of FromDate/ToDate", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("//ValidBetween[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on ValidBetween", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        validationRules.add(new ValidateNotExist("//AvailabilityCondition[not(FromDate) and not(ToDate)]", "AvailabilityCondition must have either FromDate or ToDate or both present", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("//AvailabilityCondition[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on AvailabilityCondition", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        return validationRules;

    }

    private ValidationTree getServiceFrameValidationTreeForCommonFile(String path) {
        ValidationTree serviceFrameValidationTree = new ValidationTree("Service frame in common file", path);
        serviceFrameValidationTree.addValidationRules(getServiceFrameBaseValidationRules());

        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/Line", "Line not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route", "Route not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern | journeyPatterns/ServiceJourneyPattern", "JourneyPattern not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));

        serviceFrameValidationTree.addSubTree(getNoticesValidationTree());

        return serviceFrameValidationTree;
    }


    private ValidationTree getResourceFrameValidationTree(String path) {
        ValidationTree resourceFrameValidationTree = new ValidationTree("Resource frame", path);

        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(CompanyNumber) or normalize-space(CompanyNumber) = '']", "Missing CompanyNumber element on Operator", "Resource Frame", ValidationReportEntrySeverity.INFO));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(Name) or normalize-space(Name) = '']", "Missing Name on Operator", "Resource Frame", ValidationReportEntrySeverity.ERROR));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(LegalName) or normalize-space(LegalName) = '']", "Missing LegalName element on Operator", "Resource Frame", ValidationReportEntrySeverity.INFO));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(ContactDetails)]", "Missing ContactDetails element on Operator", "Resource Frame", ValidationReportEntrySeverity.WARNING));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator/ContactDetails[(not(Email) or normalize-space(Email) = '') and (not(Phone) or normalize-space(Phone) = '') and (not(Url) or normalize-space(Url) = '')]", "At least one of Url, Phone or Email must be defined for ContactDetails on Operator", "Resource Frame", ValidationReportEntrySeverity.WARNING));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(CustomerServiceContactDetails)]", "Missing CustomerServiceContactDetails element on Operator", "Resource Frame", ValidationReportEntrySeverity.WARNING));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator/CustomerServiceContactDetails[not(Url) or normalize-space(Url) = '']", "Missing Url element for CustomerServiceContactDetails on Operator", "Resource Frame", ValidationReportEntrySeverity.WARNING));

        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Authority[not(CompanyNumber) or normalize-space(CompanyNumber) = '']", "Missing CompanyNumber element on Authority", "Resource Frame", ValidationReportEntrySeverity.INFO));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Authority[not(Name) or normalize-space(Name) = '']", "Missing Name element on Authority", "Resource Frame", ValidationReportEntrySeverity.ERROR));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Authority[not(LegalName) or normalize-space(LegalName) = '']", "Missing LegalName element on Authority", "Resource Frame", ValidationReportEntrySeverity.INFO));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Authority[not(ContactDetails)]", "Missing ContactDetails on Authority", "Resource Frame", ValidationReportEntrySeverity.ERROR));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Authority/ContactDetails[not(Url) or not(starts-with(Url, 'http://') or (starts-with(Url, 'https://')) )]", "The Url must be defined for ContactDetails on Authority and it must start with 'http://' or 'https://'", "Resource Frame", ValidationReportEntrySeverity.ERROR));

        resourceFrameValidationTree.addValidationRule(new ValidateAuthorityId(organisationRepository));

        return resourceFrameValidationTree;
    }


    private ValidationTree getServiceCalendarFrameValidationTree(String path) {
        ValidationTree serviceCalendarFrameValidationTree = new ValidationTree("Service Calendar frame", path);

        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//DayType[not(//DayTypeAssignment/DayTypeRef/@ref = @id)]", "The DayType is not assigned to any calendar dates or periods", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(dayTypes) and not(dayTypeAssignments)]", "ServiceCalendar does not contain neither DayTypes nor DayTypeAssignments", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(ToDate)]", "Missing ToDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(FromDate)]", "Missing FromDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.ERROR));

        return serviceCalendarFrameValidationTree;
    }

    private ValidationTree getVehicleScheduleFrameValidationTree(String path) {
        ValidationTree serviceCalendarFrameValidationTree = new ValidationTree("Vehicle Schedule frame", path);

        serviceCalendarFrameValidationTree.addValidationRule(new ValidateAtLeastOne("blocks/Block", "At least one Block required in VehicleScheduleFrame", "VehicleSchedule Frame", ValidationReportEntrySeverity.ERROR));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("blocks/Block[not(journeys)]", "At least one Journey must be defined for Block", "VehicleSchedule Frame", ValidationReportEntrySeverity.ERROR));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("blocks/Block[not(dayTypes)]", "At least one DayType must be defined for Block", "VehicleSchedule Frame", ValidationReportEntrySeverity.ERROR));

        return serviceCalendarFrameValidationTree;
    }

    /**
     * Validation rules that apply both to Line files and Common files.
     *
     * @return
     */
    private List<ValidationRule> getServiceFrameBaseValidationRules() {
        List<ValidationRule> validationRules = new ArrayList<>();
        validationRules.add(new ValidateNotExist("Network[not(AuthorityRef)]", "Missing AuthorityRef on Network", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("routePoints/RoutePoint[not(projections)]", "Missing Projection on RoutePoint", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("Network[not(Name) or normalize-space(Name) = '']", "Missing Name element on Network", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("Network/groupsOfLines/GroupOfLines[not(Name)  or normalize-space(Name) = '']", "Missing Name element on GroupOfLines", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("groupsOfLines", "Unexpected element groupsOfLines outside of Network", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("timingPoints", "Unexpected element timingPoints. Content ignored", "Service Frame", ValidationReportEntrySeverity.WARNING));

        validationRules.add(new ValidateNotExist("stopAssignments/PassengerStopAssignment[not(ScheduledStopPointRef)]", "Missing ScheduledStopPointRef on PassengerStopAssignment", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("stopAssignments/PassengerStopAssignment[not(QuayRef)]", "Missing QuayRef on PassengerStopAssignment", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("stopAssignments/PassengerStopAssignment[QuayRef/@ref = following-sibling::PassengerStopAssignment/QuayRef/@ref]", "The same quay is assigned more than once in PassengerStopAssignments", "Service Frame", ValidationReportEntrySeverity.WARNING));

        validationRules.add(new ValidateNotExist("serviceLinks/ServiceLink[not(FromPointRef)]", "Missing FromPointRef on ServiceLink", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("serviceLinks/ServiceLink[not(ToPointRef)]", "Missing ToPointRef on ServiceLink", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("serviceLinks/ServiceLink/projections/LinkSequenceProjection/g:LineString/g:posList[not(normalize-space(text()))]", "Missing projections element on ServiceLink", "Service Frame", ValidationReportEntrySeverity.ERROR));

        validationRules.add((new ValidateNotExist("destinationDisplays/DestinationDisplay[not(FrontText) or normalize-space(FrontText) = '']", "Missing FrontText on DestinationDisplay", "Service Frame", ValidationReportEntrySeverity.ERROR)));
        validationRules.add((new ValidateNotExist("destinationDisplays/DestinationDisplay/vias/Via[not(DestinationDisplayRef)]", "Missing DestinationDisplayRef on Via", "Service Frame", ValidationReportEntrySeverity.ERROR)));


        return validationRules;

    }


    private ValidationTree getNoticesValidationTree() {
        ValidationTree noticesValidationTree = new ValidationTree("Notices", "notices");

        noticesValidationTree.addValidationRule(new ValidateNotExist("Notice[not(Text) or normalize-space(Text/text()) = '']", "Missing element Text for Notice", "Notices", ValidationReportEntrySeverity.ERROR));
        noticesValidationTree.addValidationRule(new ValidateNotExist("Notice/alternativeTexts/AlternativeText[not(Text) or normalize-space(Text/text()) = '']", "Missing or empty element Text for Notice Alternative Text", "Notices", ValidationReportEntrySeverity.ERROR));
        noticesValidationTree.addValidationRule(new ValidateNotExist("Notice/alternativeTexts/AlternativeText/Text[not(@lang)]", "Missing element Lang for Notice Alternative Text", "Notices", ValidationReportEntrySeverity.ERROR));
        noticesValidationTree.addValidationRule(new ValidateNotExist("Notice/alternativeTexts/AlternativeText[Text/@lang = following-sibling::AlternativeText/Text/@lang or Text/@lang = preceding-sibling::AlternativeText/Text/@lang]", "The Notice has two Alternative Texts with the same language", "Notices", ValidationReportEntrySeverity.ERROR));

        return noticesValidationTree;
    }


    private ValidationTree getNoticeAssignmentsValidationTree() {
        ValidationTree noticesAssignmentsValidationTree = new ValidationTree("Notices Assignments", "noticeAssignments");
        noticesAssignmentsValidationTree.addValidationRule(new ValidateNotExist("NoticeAssignment[for $a in following-sibling::NoticeAssignment return if(NoticeRef/@ref= $a/NoticeRef/@ref and NoticedObjectRef/@ref= $a/NoticedObjectRef/@ref) then $a else ()]", "The notice is assigned multiple times to the same object", "Notices", ValidationReportEntrySeverity.WARNING));

        return noticesAssignmentsValidationTree;
    }

    private ValidationTree getServiceFrameValidationTreeForLineFile(String path) {
        ValidationTree serviceFrameValidationTree = new ValidationTree("Service frame in line file", path);
        serviceFrameValidationTree.addValidationRules(getServiceFrameBaseValidationRules());

        serviceFrameValidationTree.addValidationRule(new ValidateExactlyOne("lines/*[self::Line or self::FlexibleLine]", "There must be either Lines or Flexible Lines", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/*[self::Line or self::FlexibleLine][not(Name) or normalize-space(Name) = '']", "Missing Name on Line", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/*[self::Line or self::FlexibleLine][not(PublicCode) or normalize-space(PublicCode) = '']", "Missing PublicCode on Line", "Service Frame", ValidationReportEntrySeverity.WARNING));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/*[self::Line or self::FlexibleLine][not(TransportMode)]", "Missing TransportMode on Line", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/*[self::Line or self::FlexibleLine][not(TransportSubmode)]", "Missing TransportSubmode on Line", "Service Frame", ValidationReportEntrySeverity.WARNING));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/*[self::Line or self::FlexibleLine]/routes/Route", "Routes should not be defined within a Line or FlexibleLine", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/*[self::Line or self::FlexibleLine][not(RepresentedByGroupRef)]", "A Line must refer to a GroupOfLines or a Network through element RepresentedByGroupRef", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidatedAllowedTransportMode());
        serviceFrameValidationTree.addValidationRule(new ValidatedAllowedTransportSubMode());

        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/FlexibleLine[not(FlexibleLineType)]", "Missing FlexibleLineType on FlexibleLine", "Flexible Line", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateMandatoryBookingProperty("BookingMethods"));
        serviceFrameValidationTree.addValidationRule(new ValidateMandatoryBookingProperty("BookingContact"));
        serviceFrameValidationTree.addValidationRule(new ValidateMandatoryBookingProperty("BookWhen"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedFlexibleLineType());
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBookingWhenProperty("lines/FlexibleLine"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBuyWhenProperty("lines/FlexibleLine"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBookingMethodProperty("lines/FlexibleLine"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBookingAccessProperty("lines/FlexibleLine"));

        serviceFrameValidationTree.addValidationRule((new ValidateAtLeastOne("routes/Route", "There should be at least one Route", "Service Frame", ValidationReportEntrySeverity.ERROR)));

        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route[not(Name) or normalize-space(Name) = '']", "Missing Name on Route", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route[not(LineRef) and not(FlexibleLineRef)]", "Missing lineRef on Route", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route[not(pointsInSequence)]", "Missing pointsInSequence on Route", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route/DirectionRef", "DirectionRef not allowed on Route (use DirectionType)", "Service Frame", ValidationReportEntrySeverity.WARNING));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route/pointsInSequence/PointOnRoute[@order = preceding-sibling::PointOnRoute/@order]", "Several points on route have the same order", "Service Frame", ValidationReportEntrySeverity.WARNING));

        serviceFrameValidationTree.addValidationRule((new ValidateNotExist("journeyPatterns/ServiceJourneyPattern", "ServiceJourneyPattern not allowed", "Service Frame", ValidationReportEntrySeverity.ERROR)));
        serviceFrameValidationTree.addValidationRule((new ValidateAtLeastOne("journeyPatterns/JourneyPattern", "No JourneyPattern defined in the Service Frame", "Service Frame", ValidationReportEntrySeverity.ERROR)));

        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern[not(RouteRef)]", "Missing RouteRef on JourneyPattern", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern[1][not(DestinationDisplayRef)]", "Missing DestinationDisplayRef on first StopPointInJourneyPattern", "Service Frame", ValidationReportEntrySeverity.WARNING));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern[last()][DestinationDisplayRef]", "DestinationDisplayRef not allowed on last StopPointInJourneyPattern", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern[ForAlighting = 'false' and ForBoarding = 'false']", "StopPointInJourneyPattern neither allows boarding nor alighting", "Stop point in journey pattern", ValidationReportEntrySeverity.WARNING));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern[DestinationDisplayRef/@ref = preceding-sibling::StopPointInJourneyPattern[1]/DestinationDisplayRef/@ref and number(@order) >  number(preceding-sibling::StopPointInJourneyPattern[1]/@order)]", "StopPointInJourneyPattern declares reference to the same DestinationDisplay as previous StopPointInJourneyPattern", "Stop point in journey pattern", ValidationReportEntrySeverity.ERROR));

        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBookingWhenProperty("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBuyWhenProperty("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBookingMethodProperty("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern"));
        serviceFrameValidationTree.addValidationRule(new ValidateAllowedBookingAccessProperty("journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern"));

        serviceFrameValidationTree.addSubTree(getNoticesValidationTree());
        serviceFrameValidationTree.addSubTree(getNoticeAssignmentsValidationTree());
        return serviceFrameValidationTree;
    }

    public String describe() {
        return topLevelValidationTree.describe();
    }

    public Set<String> getRuleMessages() {
        return topLevelValidationTree.getRuleMessages();
    }


}
