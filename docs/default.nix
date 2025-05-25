{}:

let
  pkgs = import (builtins.fetchTarball {
    # for Hugo '0.111.3'
    # search with https://lazamar.co.uk/nix-versions/
    url = "https://github.com/NixOS/nixpkgs/archive/8cad3dbe48029cb9def5cdb2409a6c80d3acfe2e.tar.gz";
  }) {};

in pkgs.mkShellNoCC {
  packages = with pkgs; [
    # docs stuff
    asciidoctor
    gcc
    glibc
    hugo
  ];

  shellHook = ''
    unset PYTHONPATH
    unset LD_LIBRARY_PATH
  '';
}
