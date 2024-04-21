{ pkgs ? import <nixpkgs> {}}:

pkgs.mkShell {
  packages = with pkgs; [
    # app stuff
    openjdk17
    #graalvm-ce
    musl
    graalvmCEPackages.graalvm-ce-musl

    # docs stuff
    asciidoctor
    hugo
  ];
}
