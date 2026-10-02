{
  description = "ItszuLib dev shell for NixOS: run the dev client without extra setup";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};

      # Native libraries Minecraft's LWJGL natives dlopen at runtime (the set Prism Launcher uses). NixOS keeps the GPU
      # drivers in /run/opengl-driver/lib and puts no GL dispatcher on the loader path, so without these `runClient`
      # fails with "GLX: Failed to load GLX". LWJGL bundles its own GLFW, so nixpkgs' glfw is left out.
      libs = with pkgs; [
        libglvnd
        libGL
        openal
        alsa-lib
        libpulseaudio
        pipewire
        udev
        vulkan-loader
        libx11
        libxext
        libxcursor
        libxrandr
        libxxf86vm
        libxi
        wayland
        libxkbcommon
        flite # narrator
      ];
    in {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          # Starts gradlew. The JDK 25 and 21 toolchains the build uses are found through
          # org.gradle.java.installations.paths in ~/.gradle/gradle.properties (AGENTS.md, Machine notes).
          pkgs.jdk25
        ];

        JAVA_HOME = "${pkgs.jdk25}";
        LD_LIBRARY_PATH = "${pkgs.lib.makeLibraryPath libs}:/run/opengl-driver/lib";
      };
    };
}
