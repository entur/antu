package no.entur.antu.job;

import java.util.List;

/**
 * One unit of work on the antu job queue. The subtypes enumerate every job antu knows how to run,
 * so {@link JobDispatcher} can switch over them exhaustively instead of matching on a string.
 */
public sealed interface AntuJob {
  /**
   * The Nordic NeTEx Profile names the files holding objects shared between lines with this prefix.
   * Which files are common decides the order the pipeline validates them in.
   */
  String COMMON_FILE_PREFIX = "_";

  JobType type();

  static boolean isCommonFile(String netexFileName) {
    return netexFileName.startsWith(COMMON_FILE_PREFIX);
  }

  /**
   * A job that belongs to a dataset validation. Cache refresh jobs do not.
   */
  sealed interface ValidationJob extends AntuJob {
    ValidationContext context();
  }

  /**
   * Explode a NeTEx dataset archive into single files and create a validation job per file.
   */
  record SplitDataset(ValidationContext context) implements ValidationJob {
    @Override
    public JobType type() {
      return JobType.SPLIT;
    }
  }

  /**
   * Validate a single NeTEx file.
   *
   * @param nbNetexFiles      total number of NeTEx files in the dataset, the target of the report
   *                          aggregation barrier.
   * @param nbCommonFiles     number of common files in the dataset, the target of the common files
   *                          barrier. Only meaningful for common files.
   * @param allNetexFileNames every NeTEx file name in the dataset. Only carried on common files,
   *                          which need it to create the line file jobs once the barrier opens.
   */
  record ValidateFile(
    ValidationContext context,
    String netexFileName,
    int nbNetexFiles,
    int nbCommonFiles,
    List<String> allNetexFileNames
  )
    implements ValidationJob {
    public ValidateFile {
      allNetexFileNames = List.copyOf(allNetexFileNames);
    }

    public boolean isCommonFile() {
      return AntuJob.isCommonFile(netexFileName);
    }

    @Override
    public JobType type() {
      return JobType.VALIDATE;
    }
  }

  /**
   * All common files are validated: create a validation job for every line file.
   */
  record CreateLineFileJobs(
    ValidationContext context,
    List<String> allNetexFileNames
  )
    implements ValidationJob {
    public CreateLineFileJobs {
      allNetexFileNames = List.copyOf(allNetexFileNames);
    }

    @Override
    public JobType type() {
      return JobType.CREATE_LINE_FILE_JOBS;
    }
  }

  /**
   * All files are validated: merge the individual reports into one dataset report.
   */
  record AggregateReports(
    ValidationContext context,
    List<String> netexFileNames
  )
    implements ValidationJob {
    public AggregateReports {
      netexFileNames = List.copyOf(netexFileNames);
    }

    @Override
    public JobType type() {
      return JobType.AGGREGATE_REPORTS;
    }
  }

  /**
   * Run the validators that need the whole dataset at once, on top of the merged report.
   */
  record ValidateDataset(ValidationContext context) implements ValidationJob {
    @Override
    public JobType type() {
      return JobType.VALIDATE_DATASET;
    }
  }

  /**
   * Publish the final report and notify the validation client.
   */
  record CompleteValidation(ValidationContext context)
    implements ValidationJob {
    @Override
    public JobType type() {
      return JobType.COMPLETE_VALIDATION;
    }
  }

  record RefreshStopPlaceCache() implements AntuJob {
    @Override
    public JobType type() {
      return JobType.REFRESH_STOP_PLACE_CACHE;
    }
  }

  record RefreshOrganisationAliasCache() implements AntuJob {
    @Override
    public JobType type() {
      return JobType.REFRESH_ORGANISATION_ALIAS_CACHE;
    }
  }
}
