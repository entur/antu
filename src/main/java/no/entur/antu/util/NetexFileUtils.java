package no.entur.antu.util;

import static no.entur.antu.Constants.FILENAME_DELIMITER;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class NetexFileUtils {

  private NetexFileUtils() {}

  public static boolean isCommonFile(String fileName) {
    return fileName.startsWith("_");
  }

  public static Set<String> buildFileNamesListFromString(String fileNameList) {
    return Arrays
      .stream(fileNameList.split(FILENAME_DELIMITER))
      .collect(Collectors.toUnmodifiableSet());
  }

  public static String buildFileNamesListFromSet(Set<String> netexFileNames) {
    return netexFileNames
      .stream()
      .sorted()
      .collect(Collectors.joining(FILENAME_DELIMITER));
  }
}
