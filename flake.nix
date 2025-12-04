{
  description = "Gadgetbridge Android development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android-nixpkgs = {
      url = "github:tadfisher/android-nixpkgs";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    {
      self,
      nixpkgs,
      android-nixpkgs,
    }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };

      android-composition = android-nixpkgs.sdk.${system} (
        sdkPkgs: with sdkPkgs; [
          cmdline-tools-latest
          build-tools-36-0-0
          platform-tools
          platforms-android-36
        ]
      );

    in
    {
      devShells.${system} = {
        # Main development environment - simple shell with Android tools
        default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk21
            android-composition
            adb-sync
            scrcpy
          ];

          shellHook = ''
            echo "🤖 Gadgetbridge development environment"

            # Set JAVA_HOME for Gradle
            export JAVA_HOME=${pkgs.jdk21}/lib/openjdk

            # Set Android SDK path
            export ANDROID_SDK_ROOT=${android-composition}/share/android-sdk
            export ANDROID_HOME=$ANDROID_SDK_ROOT

            # Add Android tools to PATH
            export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
            export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin
            export PATH=$PATH:$ANDROID_SDK_ROOT/build-tools/36.0.0

            # Gradle configuration
            export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2"

            echo "Java version: $(java -version 2>&1 | head -n1)"
            echo "✅ Environment ready!"
            echo "• JAVA_HOME: $JAVA_HOME"
            echo "• ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT"
            echo "• Available commands: ./gradlew (alias), adb, aapt2"
            echo ""
            echo "🚀 Quick start:"
            echo "  ./gradlew assembleMainlineDebug    # Build debug APK"
            echo "  ./gradlew installMainlineDebug     # Install to connected device"
            echo "  ./gradlew test                     # Run tests"
            echo "  ./gradlew lint                     # Run lint checks"
          '';
        };
      };
    };
}
