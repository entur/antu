package no.entur.antu.agreement;

public class SimpleAgreementRepository implements AgreementRepository {
    public SimpleAgreementRepository() {}

    @Override
    public void refreshCache() {}

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Boolean organisationExists(String organisationId) {
        return false;
    }
}
