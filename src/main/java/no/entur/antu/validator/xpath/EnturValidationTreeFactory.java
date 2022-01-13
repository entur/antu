package no.entur.antu.validator.xpath;

import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.xpath.rules.ValidateAuthorityId;

/**
 * Build the tree of XPath validation rules with Entur-specific rules.
 */
public class EnturValidationTreeFactory extends DefaultValidationTreeFactory {

    private final OrganisationRepository organisationRepository;

    public EnturValidationTreeFactory(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    @Override
    protected ValidationTree getResourceFrameValidationTree(String path) {
        ValidationTree resourceFrameValidationTree = super.getResourceFrameValidationTree(path);
        resourceFrameValidationTree.addValidationRule(new ValidateAuthorityId(organisationRepository));
        return resourceFrameValidationTree;
    }
}
