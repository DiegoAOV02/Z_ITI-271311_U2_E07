@echo off
"C:\\Users\\Danie\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HC:\\Users\\Danie\\AndroidStudioProjects\\Z_ITI-271311_U2_E07\\sdk\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=C:\\Users\\Danie\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\Danie\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\Danie\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\Danie\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=C:\\Users\\Danie\\AndroidStudioProjects\\Z_ITI-271311_U2_E07\\sdk\\build\\intermediates\\cxx\\Debug\\2246z4h2\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=C:\\Users\\Danie\\AndroidStudioProjects\\Z_ITI-271311_U2_E07\\sdk\\build\\intermediates\\cxx\\Debug\\2246z4h2\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BC:\\Users\\Danie\\AndroidStudioProjects\\Z_ITI-271311_U2_E07\\sdk\\.cxx\\Debug\\2246z4h2\\armeabi-v7a" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
