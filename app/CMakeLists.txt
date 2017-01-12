# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

include_directories(
    src/main/cpp/config
    src/main/cpp/ogg/include
    src/main/cpp/flac/include
    src/main/cpp/flac/src/libFLAC/include
)

add_library( # Sets the name of the library.
             audioboo-ogg
             # Sets the library as a shared library.
             STATIC
             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
              src/main/cpp/ogg/src/bitwise.c
              src/main/cpp/ogg/src/framing.c)



              	add_library(
              	            audioboo-flac
              	            STATIC
              	            src/main/cpp/flac/src/libFLAC/bitmath.c
                            	src/main/cpp/flac/src/libFLAC/bitreader.c
                            	src/main/cpp/flac/src/libFLAC/cpu.c
                            	src/main/cpp/flac/src/libFLAC/crc.c
                            	src/main/cpp/flac/src/libFLAC/fixed.c
                            	src/main/cpp/flac/src/libFLAC/float.c
                            	src/main/cpp/flac/src/libFLAC/format.c
                            	src/main/cpp/flac/src/libFLAC/lpc.c
                            	src/main/cpp/flac/src/libFLAC/md5.c
                            	src/main/cpp/flac/src/libFLAC/memory.c
                            	src/main/cpp/flac/src/libFLAC/metadata_iterators.c
                            	src/main/cpp/flac/src/libFLAC/metadata_object.c
                            	src/main/cpp/flac/src/libFLAC/ogg_decoder_aspect.c
                            	src/main/cpp/flac/src/libFLAC/ogg_encoder_aspect.c
                            	src/main/cpp/flac/src/libFLAC/ogg_helper.c
                            	src/main/cpp/flac/src/libFLAC/ogg_mapping.c
                            	src/main/cpp/flac/src/libFLAC/stream_decoder.c
                            	src/main/cpp/flac/src/libFLAC/stream_encoder.c
                            	src/main/cpp/flac/src/libFLAC/stream_encoder_framing.c
                            	src/main/cpp/flac/src/libFLAC/window.c
                            	src/main/cpp/flac/src/libFLAC/bitwriter.c

              	)

              	add_library(
              	            audioboo-native
              	            SHARED
              	            src/main/cpp/jni/FLACStreamEncoder.cpp
                            	src/main/cpp/jni/FLACStreamDecoder.cpp
                            	src/main/cpp/jni/util.cpp
              	)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       audioboo-native

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} audioboo-ogg audioboo-flac )
