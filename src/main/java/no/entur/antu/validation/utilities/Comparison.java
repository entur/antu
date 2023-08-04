package no.entur.antu.validation.utilities;

public record Comparison<C>(C expected, C actual) {
  public static <C> Comparison<C> of(C expected, C actual) {
    return new Comparison<>(expected, actual);
  }
}
