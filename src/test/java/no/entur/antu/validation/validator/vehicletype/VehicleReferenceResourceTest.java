package no.entur.antu.validation.validator.vehicletype;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class VehicleReferenceResourceTest {

  private String mockResponseBody() {
    return """
            [
              "NMR:Vehicle:1", "NMR:VehicleType:2" ,"RUT:DeckPlan:1", "RUT:VehicleType:30"
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
  void testGetVehicleReferences() {
    String mockedResponseBody = mockResponseBody();
    WebClient webClient = mockWebClient(HttpStatus.OK, mockedResponseBody);
    VehicleReferenceResource vehicleReferenceResource =
      new VehicleReferenceResource(webClient);
    Set<String> vehicleReferences =
      vehicleReferenceResource.getVehicleAndVehicleTypesRef();
    assertTrue(
      vehicleReferences.containsAll(
        List.of(
          "NMR:Vehicle:1",
          "NMR:VehicleType:2",
          "RUT:DeckPlan:1",
          "RUT:VehicleType:30"
        )
      )
    );
  }

  @Test
  void testGetVehicleReferencesFailure() {
    String mockedResponseBody = mockResponseBody();
    WebClient webClient = mockWebClient(
      HttpStatus.BAD_REQUEST,
      mockedResponseBody
    );
    VehicleReferenceResource vehicleReferenceResource =
      new VehicleReferenceResource(webClient);
    assertThrows(
      WebClientResponseException.class,
      vehicleReferenceResource::getVehicleAndVehicleTypesRef
    );
  }
}
