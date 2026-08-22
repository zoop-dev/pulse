{
  description = "Gadgetbridge Android development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    {
      self,
      nixpkgs,
    }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
        config.android_sdk.accept_license = true;
      };

      android-composition = pkgs.androidenv.composeAndroidPackages {
        cmdLineToolsVersion = "latest";
        platformVersions = [ "37" ];
        buildToolsVersions = [ "36.0.0" ];
        includeEmulator = false;
        includeNDK = false;
        includeSources = false;
        includeSystemImages = false;
      };

    in
    {
      devShells.${system} = {
        default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk21
            android-composition.androidsdk
            adb-sync
            scrcpy
          ];

          shellHook = ''
            echo "🤖 Gadgetbridge development environment"

            # Set JAVA_HOME for Gradle
            export JAVA_HOME=${pkgs.jdk21}/lib/openjdk

            # Set Android SDK path
            export ANDROID_SDK_ROOT=${android-composition.androidsdk}/libexec/android-sdk
            export ANDROID_HOME=$ANDROID_SDK_ROOT

            # Add Android tools to PATH
            LATEST_BUILD_TOOLS=$(ls -1 $ANDROID_SDK_ROOT/build-tools/ | sort -V | tail -1)
            if [ -z "$LATEST_BUILD_TOOLS" ]; then
              echo "Failed to find latest build tools"
              exit 1
            fi
            export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
            export PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin
            export PATH=$PATH:$ANDROID_SDK_ROOT/build-tools/$LATEST_BUILD_TOOLS

            # Gradle configuration
            export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/$LATEST_BUILD_TOOLS/aapt2"

            echo "Java version: $(java -version 2>&1 | head -n1)"

            # Remove sdk.dir from local.properties if present, otherwise aidl
            # will fail to run
            if [ -f local.properties ]; then
              sed -i -E '/^sdk\.dir=/d' local.properties
            fi

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
