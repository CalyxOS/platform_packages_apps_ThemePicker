#
# Copyright (C) 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


LOCAL_PATH := $(call my-dir)
WALLPAPER_PATH := ../WallpaperPicker2

#
# Build rule for ThemePicker app.
#
include $(CLEAR_VARS)
LOCAL_USE_AAPT2 := true
LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_ANDROID_LIBRARIES := WallpaperPicker2CommonDepsLib
LOCAL_STATIC_JAVA_LIBRARIES := styleprotosnano SystemUI-statsd SystemUISharedLib
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := ThemePicker
LOCAL_PRIVILEGED_MODULE := true
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_OVERRIDES_PACKAGES := WallpaperPicker WallpaperPicker2

ifneq (,$(wildcard frameworks/base))
  LOCAL_PRIVATE_PLATFORM_APIS := true
else
  LOCAL_SDK_VERSION := system_current
  LOCAL_STATIC_JAVA_LIBRARIES += libSharedWallpaper
endif

LOCAL_SRC_FILES := $(call all-java-files-under, $(WALLPAPER_PATH)/src) \
    $(call all-java-files-under, src) \
    $(call all-java-files-under, src_override)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_MANIFEST_FILE := AndroidManifest.xml

LOCAL_REQUIRED_MODULES := default_permissions_com.android.wallpaper.xml privapp_whitelist_com.android.wallpaper.xml

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_MODULE := default_permissions_com.android.wallpaper.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_SYSTEM_EXT_ETC)/default-permissions
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := privapp_whitelist_com.android.wallpaper.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_SYSTEM_EXT_ETC)/permissions
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

# ==================================================
include $(call all-makefiles-under,$(LOCAL_PATH))
