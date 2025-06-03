{ pkgs ? import <nixpkgs> {}}:

let
  libs  = with pkgs; [
    # Kotlin Compose Desktop
    fontconfig
    libGL
    xorg.libX11
  ];

  lib-path = with pkgs; lib.makeLibraryPath libs;

in pkgs.mkShell {
  OPENJDK_17_HOME = "${pkgs.openjdk17}/lib/openjdk";
  OPENJDK_21_HOME = "${pkgs.openjdk21}/lib/openjdk";
  TEMURIN_17_HOME = "${pkgs.temurin-bin-17}";
  TEMURIN_21_HOME = "${pkgs.temurin-bin-21}";
  GRAALVM_HOME = "${pkgs.graalvm-ce}";

  JAVA_HOME = "${pkgs.openjdk17}/lib/openjdk";

  buildInputs = with pkgs; [
    openjdk17
    openjdk21
    temurin-bin-17
    temurin-bin-21
    graalvm-ce
  ] ++ libs;

  packages = with pkgs; [
    temurin-bin-17

    jq

    flatpak-builder
    appstream
  ];

  shellHook = ''
    export "LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${lib-path}"
  '';
}
