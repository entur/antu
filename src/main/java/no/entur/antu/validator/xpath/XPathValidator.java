package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.xml.XMLParserUtil;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.List;

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

    public List<ValidationReportEntry> validate(String codespace, String fileName, byte[] content) throws XMLStreamException, SaxonApiException, XPathExpressionException {
        XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
        ValidationContext validationContext = new ValidationContext(document, XMLParserUtil.getXPathCompiler(), codespace, fileName);
        if (fileName.startsWith("_")) {
            return commonFileValidationTree.validate(validationContext);
        } else {
            return lineFileValidationTree.validate(validationContext);
        }
    }


    private ValidationTree getCommonFileValidationTree() {
        ValidationTree validationTree = new ValidationTree("/");
        validationTree.addSubTree(getCompositeFrameForCommonFileValidationTree());
        return validationTree;
    }

    private ValidationTree getLineFileValidationTree() {
        ValidationTree validationTree = new ValidationTree("/");
        validationTree.addSubTree(getCompositeFrameForLineFileValidationTree());
        return validationTree;
    }

    private ValidationTree getCompositeFrameForLineFileValidationTree() {
        ValidationTree compositeFrameValidationTree = new ValidationTree("PublicationDelivery/dataObjects/CompositeFrame");

        compositeFrameValidationTree.addValidationRules(getCompositeFrameValidationRules());

        compositeFrameValidationTree.addSubTree(getResourceFrameValidationTree());
        compositeFrameValidationTree.addSubTree(getServiceCalendarFrameValidationTree());
        compositeFrameValidationTree.addSubTree(getServiceFrameForLineFileValidationTree());

        return compositeFrameValidationTree;
    }

    private ValidationTree getCompositeFrameForCommonFileValidationTree() {
        ValidationTree compositeFrameValidationTree = new ValidationTree("PublicationDelivery/dataObjects/CompositeFrame");

        compositeFrameValidationTree.addValidationRules(getCompositeFrameValidationRules());

        compositeFrameValidationTree.addSubTree(getResourceFrameValidationTree());
        compositeFrameValidationTree.addSubTree(getServiceCalendarFrameValidationTree());
        compositeFrameValidationTree.addSubTree(getServiceFrameForCommonFileValidationTree());

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

    private ValidationTree getServiceFrameForCommonFileValidationTree() {
        ValidationTree serviceFrameValidationTree = new ValidationTree("frames/ServiceFrame");

        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("lines/Line", "Line not allowed in  common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("routes/Route", "Route not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("journeyPatterns/JourneyPattern | journeyPatterns/ServiceJourneyPattern", "JourneyPattern not allowed in common files", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("destinationDisplays/DestinationDisplay[not(FrontText) or normalize-space(FrontText) = '']", "Missing FrontText on DestinationDisplay", "Service Frame", ValidationReportEntrySeverity.ERROR));
        serviceFrameValidationTree.addValidationRule(new ValidateNotExist("destinationDisplays/DestinationDisplay/vias/Via[not(DestinationDisplayRef)]", "Missing DestinationDisplayRef on Via", "Service Frame", ValidationReportEntrySeverity.ERROR));

        return serviceFrameValidationTree;
    }


    private ValidationTree getServiceFrameForLineFileValidationTree() {
        ValidationTree serviceFrameValidationTree = new ValidationTree("frames/ServiceFrame");
        return serviceFrameValidationTree;
    }


    private ValidationTree getResourceFrameValidationTree() {
        ValidationTree resourceFrameValidationTree = new ValidationTree("frames/ResourceFrame");

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


    private ValidationTree getServiceCalendarFrameValidationTree() {
        ValidationTree serviceCalendarFrameValidationTree = new ValidationTree("frames/ServiceCalendarFrame");

        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//DayType[not(//DayTypeAssignment/DayTypeRef/@ref = @id)]", "DayType %{source_objectid} is not assigned to any calendar dates or periods", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(dayTypes) and not(dayTypeAssignments)]", "ServiceCalendar does not contain neither DayTypes nor DayTypeAssignments", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(ToDate)]", " Missing ToDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[not(FromDate)]", "Missing FromDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.WARNING));
        serviceCalendarFrameValidationTree.addValidationRule(new ValidateNotExist("//ServiceCalendar[FromDate and ToDate and ToDate < FromDate]", "FromDate cannot be after ToDate on ServiceCalendar", "Service Calendar Frame", ValidationReportEntrySeverity.ERROR));

        return serviceCalendarFrameValidationTree;

    }

}
