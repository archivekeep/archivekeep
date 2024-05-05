{ pkgs ? import <nixpkgs> {}}:

pkgs.mkShell {
  OPENJDK_17_HOME = "${pkgs.openjdk17}/lib/openjdk";
  OPENJDK_21_HOME = "${pkgs.openjdk21}/lib/openjdk";
  TEMURIN_17_HOME = "${pkgs.temurin-bin-17}";
  TEMURIN_21_HOME = "${pkgs.temurin-bin-21}";
  GRAALVM_HOME = "${pkgs.graalvm-ce}";

  buildInputs = with pkgs; [
    openjdk17
    openjdk21
    temurin-bin-17
    temurin-bin-21
    graalvm-ce
  ];

  packages = with pkgs; [
    # use older JDK - Gradle/Groovy is not compatible with version 22 provided by GraalVM
    openjdk17

    # still install GraalVM, but additionally - not a primary JDK
    musl
    graalvmCEPackages.graalvm-ce-musl

    # docs stuff
    asciidoctor
    hugo
  ];
}
