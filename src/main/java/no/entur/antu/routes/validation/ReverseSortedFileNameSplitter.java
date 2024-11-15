package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.FILENAME_DELIMITER;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.camel.Exchange;

/**
 * Sort the list in reverse order to get the common files first.
 */
class ReverseSortedFileNameSplitter {

  public static List<String> split(Exchange exchange) {
    String fileNameList = exchange.getMessage().getBody(String.class);
    return Arrays
      .stream(fileNameList.split(FILENAME_DELIMITER))
      .sorted(Collections.reverseOrder())
      .toList();
  }
}
