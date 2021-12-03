package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.xml.XMLParserUtil;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

import static no.entur.antu.xml.XMLParserUtil.selectNodeSet;

/**
 * Run XPath validation rules against the dataset.
 */
public class XPathValidator {

    private final OrganisationRepository organisationRepository;
    private final ValidationTree commonFileValidationTree;
    private final ValidationTree lineFileValidationTree;

    public XPathValidator(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
        this.commonFileValidationTree = getCommonFileValidationTree();
        this.lineFileValidationTree = getLineFileValidationTree();
    }

    public List<ValidationReportEntry> validate(String codespace, String fileName, byte[] content) throws XMLStreamException, SaxonApiException {
        XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
        ValidationContext validationContext = new ValidationContext(document, XMLParserUtil.getXPathCompiler(), codespace, fileName);
        if (fileName.startsWith("_")) {
            return commonFileValidationTree.validate(validationContext);
        } else {
            return lineFileValidationTree.validate(validationContext);
        }
    }


    private ValidationTree getCommonFileValidationTree() {
        ValidationTree validationTree = new ValidationTree("Common file", "/");
        validationTree.addSubTree(getCompositeFrameValidationTreeForCommonFile());
        validationTree.addSubTree(getSingleFramesValidationTreeForCommonFile());

        return validationTree;
    }

    private ValidationTree getSingleFramesValidationTreeForCommonFile() {
        ValidationTree validationTree = new ValidationTree("Single frames in common file", "PublicationDelivery/dataObjects",
                validationContext -> selectNodeSet("CompositeFrame", validationContext.getxPathCompiler(), validationContext.getXmlNode()).isEmpty());

        validationTree.addValidationRule(new ValidateNotExist("SiteFrame", "Unexpected element SiteFrame. It will be ignored", "Composite Frame", ValidationReportEntrySeverity.WARNING));
        validationTree.addValidationRule(new ValidateNotExist("TimetableFrame", "Timetable frame not allowed in common files", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateExist("ServiceFrame[validityConditions] | ServiceCalendarFrame[validityConditions]", "Neither ServiceFrame nor ServiceCalendarFrame defines ValidityConditions", "Single Frames", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateNotExist("ResourceFrame[not(validityConditions) and count(//ResourceFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("ServiceFrame[not(validityConditions) and count(//ServiceFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("ServiceCalendarFrame[not(validityConditions) and count(//ServiceCalendarFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));


        validationTree.addSubTree(getResourceFrameValidationTree("ResourceFrame"));
        validationTree.addSubTree(getServiceFrameForCommonFileValidationTree("ServiceFrame"));
        validationTree.addSubTree(getServiceCalendarFrameValidationTree("ServiceCalendarFrame"));

        return validationTree;
    }

    private ValidationTree getLineFileValidationTree() {
        ValidationTree validationTree = new ValidationTree("Line file", "/");
        validationTree.addSubTree(getCompositeFrameValidationTreeForLineFile());
        validationTree.addSubTree(getSingleFramesValidationTreeForLineFile());
        return validationTree;
    }

    private ValidationTree getCompositeFrameValidationTreeForLineFile() {
        ValidationTree compositeFrameValidationTree = new ValidationTree("Composite frame in line file", "PublicationDelivery/dataObjects/CompositeFrame");

        compositeFrameValidationTree.addValidationRules(getCompositeFrameValidationRules());

        compositeFrameValidationTree.addSubTree(getResourceFrameValidationTree("frames/ResourceFrame"));
        compositeFrameValidationTree.addSubTree(getServiceCalendarFrameValidationTree("frames/ServiceCalendarFrame"));
        compositeFrameValidationTree.addSubTree(getVehicleScheduleFrameValidationTree("frames/VehicleScheduleFrame"));

        compositeFrameValidationTree.addSubTree(getServiceFrameForLineFileValidationTree("frames/ServiceFrame"));

        return compositeFrameValidationTree;
    }

    private ValidationTree getSingleFramesValidationTreeForLineFile() {
        ValidationTree validationTree = new ValidationTree("Single frames in line file", "PublicationDelivery/dataObjects",
                validationContext ->
                        selectNodeSet("CompositeFrame", validationContext.getxPathCompiler(), validationContext.getXmlNode()).isEmpty());


        validationTree.addValidationRule(new ValidateNotExist("SiteFrame", "Unexpected element SiteFrame. It will be ignored", "Composite Frame", ValidationReportEntrySeverity.WARNING));

        validationTree.addValidationRule(new ValidateExist("ServiceFrame[validityConditions] | ServiceCalendarFrame[validityConditions] | TimetableFrame[validityConditions]", "Neither ServiceFrame, ServiceCalendarFrame nor TimetableFrame defines ValidityConditions", "Single Frames", ValidationReportEntrySeverity.ERROR));

        validationTree.addValidationRule(new ValidateNotExist("ServiceFrame[not(validityConditions) and count(//ServiceFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("ServiceCalendarFrame[not(validityConditions) and count(//ServiceCalendarFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("TimetableFrame[not(validityConditions) and count(//TimetableFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));
        validationTree.addValidationRule(new ValidateNotExist("VehicleScheduleFrame[not(validityConditions) and count(//VehicleScheduleFrame) > 1]", "Multiple frames of same type without validity conditions", "Single Frames", ValidationReportEntrySeverity.ERROR));


        validationTree.addSubTree(getResourceFrameValidationTree("ResourceFrame"));
        validationTree.addSubTree(getServiceCalendarFrameValidationTree("ServiceCalendarFrame"));
        validationTree.addSubTree(getTimetableFrameValidationTree("TimetableFrame"));

        validationTree.addSubTree(getServiceFrameForLineFileValidationTree("ServiceFrame"));

        return validationTree;
    }

    private ValidationTree getTimetableFrameValidationTree(String path) {
        ValidationTree validationTree = new ValidationTree("Timetable frame", path);
        return validationTree;
    }

    private ValidationTree getCompositeFrameValidationTreeForCommonFile() {
        ValidationTree compositeFrameValidationTree = new ValidationTree("Composite frame in common file", "PublicationDelivery/dataObjects/CompositeFrame");

        compositeFrameValidationTree.addValidationRules(getCompositeFrameValidationRules());
        compositeFrameValidationTree.addValidationRule(new ValidateNotExist("frames/TimetableFrame", "Timetable frame not allowed in common files", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        compositeFrameValidationTree.addSubTree(getResourceFrameValidationTree("frames/ResourceFrame"));
        compositeFrameValidationTree.addSubTree(getServiceCalendarFrameValidationTree("frames/ServiceCalendarFrame"));
        compositeFrameValidationTree.addSubTree(getVehicleScheduleFrameValidationTree("frames/VehicleScheduleFrame"));

        compositeFrameValidationTree.addSubTree(getServiceFrameForCommonFileValidationTree("frames/ServiceFrame"));

        return compositeFrameValidationTree;
    }

    private List<ValidationRule> getCompositeFrameValidationRules() {
        List<ValidationRule> validationRules = new ArrayList<>();
        validationRules.add(new ValidateNotExist("frames/SiteFrame", "Unexpected element SiteFrame. It will be ignored", "Composite Frame", ValidationReportEntrySeverity.WARNING));

        validationRules.add(new ValidateNotExist(".[not(validityConditions)]", "A CompositeFrame must define a ValidityCondition valid for all data within the CompositeFrame", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("frames//validityConditions", "ValidityConditions defined inside a frame inside a CompositeFrame", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNSRCodespaceExist());

        validationRules.add(new ValidateNotExist("//ValidBetween[not(FromDate) and not(ToDate)]", "ValidBetween missing either or both of FromDate/ToDate", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("//ValidBetween[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on ValidBetween", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        validationRules.add(new ValidateNotExist("//AvailabilityCondition[not(FromDate) and not(ToDate)]", "AvailabilityCondition must have either FromDate or ToDate or both present", "Composite Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("//AvailabilityCondition[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on AvailabilityCondition", "Composite Frame", ValidationReportEntrySeverity.ERROR));

        return validationRules;

    }

    private ValidationTree getServiceFrameForCommonFileValidationTree(String path) {
        ValidationTree serviceFrameValidationTree = new ValidationTree("Service frame in common file", path);
        serviceFrameValidationTree.addValidationRules(getServiceFrameValidationRules());

        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/Line", "Line not allowed in  common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route", "Route not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern | journeyPatterns/ServiceJourneyPattern", "JourneyPattern not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("destinationDisplays/DestinationDisplay[not(FrontText) or normalize-space(FrontText) = '']", "Missing FrontText on DestinationDisplay", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("destinationDisplays/DestinationDisplay/vias/Via[not(DestinationDisplayRef)]", "Missing DestinationDisplayRef on Via", "Service Frame", ValidationReportEntrySeverity.ERROR));

        return serviceFrameValidationTree;
    }


    private ValidationTree getServiceFrameForLineFileValidationTree(String path) {
        ValidationTree serviceFrameValidationTree = new ValidationTree("Service frame in line file", path);
        serviceFrameValidationTree.addValidationRules(getServiceFrameValidationRules());
        return serviceFrameValidationTree;
    }


    private ValidationTree getResourceFrameValidationTree(String path) {
        ValidationTree resourceFrameValidationTree = new ValidationTree("Resource frame", path);

        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(CompanyNumber) or normalize-space(CompanyNumber) = '']", "Missing CompanyNumber element on Operator", "Resource Frame", ValidationReportEntrySeverity.INFO));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(Name) or normalize-space(Name) = '']", "Missing Name on Operator", "Resource Frame", ValidationReportEntrySeverity.ERROR));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(LegalName) or normalize-space(LegalName) = '']", "Missing LegalName element on Operator", "Resource Frame", ValidationReportEntrySeverity.INFO));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator[not(ContactDetails)]", "Missing ContactDetails element on Operator", "Resource Frame", ValidationReportEntrySeverity.WARNING));
        resourceFrameValidationTree.addValidationRule(new ValidateNotExist("organisations/Operator/ContactDetails[(not(Email) or normalize-space(Email) = '') and (not(Phone) or normalize-space(Phone) = '') and (not(Url) or normalize-space(Url) = '')]", "At least one of Url, Phone or Email must be defined for ContactDetails on Operator", "Resource Frame", ValidationReportEntrySeverity.ERROR));
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

        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//DayType[not(//DayTypeAssignment/DayTypeRef/@ref = @id)]", "DayType %{source_objectid} is not assigned to any calendar dates or periods", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(dayTypes) and not(dayTypeAssignments)]", "ServiceCalendar does not contain neither DayTypes nor DayTypeAssignments", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(ToDate)]", "Missing ToDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(FromDate)]", "Missing FromDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.ERROR));

        return serviceCalendarFrameValidationTree;
    }

    private ValidationTree getVehicleScheduleFrameValidationTree(String path) {
        ValidationTree serviceCalendarFrameValidationTree = new ValidationTree("Service Calendar frame", path);

        serviceCalendarFrameValidationTree.addValidationRule(new ValidateExist("blocks/Block", "At least one Block required in VehicleScheduleFrame", "VehicleSchedule Frame", ValidationReportEntrySeverity.ERROR));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("blocks/Block[not(journeys)]", "At least one Journey must be defined for Block", "VehicleSchedule Frame", ValidationReportEntrySeverity.ERROR));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("blocks/Block[not(dayTypes)]", " At least one DayType must be defined for Block", "VehicleSchedule Frame", ValidationReportEntrySeverity.ERROR));

        return serviceCalendarFrameValidationTree;
    }

    private List<ValidationRule> getServiceFrameValidationRules() {
        List<ValidationRule> validationRules = new ArrayList<>();
        validationRules.add(new ValidateNotExist("Network[not(AuthorityRef)]", "Missing AuthorityRef on Network", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("routePoints/RoutePoint[not(projections)]", "Missing Projection on RoutePoint", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("Network[not(Name) or normalize-space(Name) = '']", "Missing Name element on Network", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("Network/groupsOfLines/GroupOfLines[not(Name)  or normalize-space(Name) = '']", "Missing Name element on GroupOfLines", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("groupsOfLines", "Unexpected element groupsOfLines outside of Network", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("timingPoints", "Unexpected element timingPoints. Content ignored", "Service Frame", ValidationReportEntrySeverity.WARNING));

        validationRules.add(new ValidateNotExist("stopAssignments/PassengerStopAssignment[not(ScheduledStopPointRef)]", "Missing ScheduledStopPointRef on PassengerStopAssignment", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("stopAssignments/PassengerStopAssignment[not(QuayRef)]", "Missing QuayRef on PassengerStopAssignment","Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("stopAssignments/PassengerStopAssignment[QuayRef/@ref = following-sibling::PassengerStopAssignment/QuayRef/@ref]", "The same quay is assigned more than once in PassengerStopAssignments", "Service Frame", ValidationReportEntrySeverity.WARNING));

        validationRules.add(new ValidateNotExist("serviceLinks/ServiceLink[not(FromPointRef)]", "Missing FromPointRef on ServiceLink", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("serviceLinks/ServiceLink[not(ToPointRef)]", "Missing ToPointRef on ServiceLink", "Service Frame", ValidationReportEntrySeverity.ERROR));
        validationRules.add(new ValidateNotExist("serviceLinks/ServiceLink/projections/LinkSequenceProjection/g:LineString/g:posList[not(normalize-space(text()))]", "Missing projections element on ServiceLink", "Service Frame", ValidationReportEntrySeverity.ERROR));


        return validationRules;

    }

}


