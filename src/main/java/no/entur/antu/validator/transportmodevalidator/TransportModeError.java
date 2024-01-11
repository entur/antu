package no.entur.antu.validator.transportmodevalidator;

import no.entur.antu.validator.ValidationError;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

public record TransportModeError(
        RuleCode ruleCode,
        AllVehicleModesOfTransportEnumeration mode,
        String serviceJourneyId

) implements ValidationError {

    @Override
    public String getRuleCode() {
        return ruleCode.toString();
    }

    @Override
    public String validationReportEntryMessage() {
        return String.format(
                "Invalid transport mode %s found in service journey with id %s",
                mode(),
                serviceJourneyId()
        );
    }

    enum RuleCode implements no.entur.antu.validator.RuleCode {
        NETEX_TRANSPORT_MODE_1("Invalid transport mode");
        private final String errorMessage;

        RuleCode(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
