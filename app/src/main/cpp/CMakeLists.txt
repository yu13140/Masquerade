cmake_minimum_required(VERSION 3.22)
   project(nativehook)

   add_library(nativehook SHARED
               native_hook.cpp)

   find_library(log-lib log)
   target_link_libraries(nativehook
                        ${log-lib}
                        dl)