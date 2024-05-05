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
    # add GraalVM - must be default JDK to work properly
    # gradle will detect other JDK(s) to compile classes
    graalvmCEPackages.graalvm-ce-musl
    musl

    # docs stuff
    asciidoctor
    hugo
  ];
}
