{ pkgs ? import <nixpkgs> {}}:

pkgs.mkShell {
  packages = with pkgs; [
    # app stuff
    graalvm-ce
    openjdk17

    # docs stuff
    asciidoctor
    hugo
  ];
}
