package no.entur.antu.validation.validator.organisation;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class AgreementResourceTest {

  private String mockResponseBody() {
    return """
            [
              { "roleIds": ["KOL:Operator:301", "KOL:Operator:302"], "aliases": ["KOL:Operator:303", "KOL:Operator:304"] },
              { "roleIds": ["RUT:Operator:301", "RUT:Operator:302"], "aliases": ["RUT:Operator:303", "RUT:Operator:304"] }
            ]
        """;
  }

  private WebClient mockWebClient(HttpStatus status, String responseBody) {
    ClientResponse mockResponse = ClientResponse
      .create(status)
      .header("Content-Type", "application/json")
      .body(responseBody)
      .build();
    ExchangeFunction exchangeFunction = request -> Mono.just(mockResponse);
    return WebClient.builder().exchangeFunction(exchangeFunction).build();
  }

  @Test
  void testGetOrganisationAliases() {
    String mockedResponseBody = mockResponseBody();
    WebClient webClient = mockWebClient(HttpStatus.OK, mockedResponseBody);
    AgreementResource agreementResource = new AgreementResource(webClient);
    Set<String> organisationAliases =
      agreementResource.getOrganisationAliases();
    Assertions.assertTrue(
      organisationAliases.containsAll(
        List.of(
          "KOL:Operator:301",
          "KOL:Operator:302",
          "KOL:Operator:303",
          "KOL:Operator:304",
          "RUT:Operator:301",
          "RUT:Operator:302",
          "RUT:Operator:303",
          "RUT:Operator:304"
        )
      )
    );
  }

  @Test
  void testGetOrganisationAliasesFailure() {
    String mockedResponseBody = mockResponseBody();
    WebClient webClient = mockWebClient(
      HttpStatus.BAD_REQUEST,
      mockedResponseBody
    );
    AgreementResource agreementResource = new AgreementResource(webClient);
    Assertions.assertThrows(
      WebClientResponseException.class,
      () -> agreementResource.getOrganisationAliases()
    );
  }
}
