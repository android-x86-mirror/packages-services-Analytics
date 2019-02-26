# Utilities for Android-x86 Analytics

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_MODULE := analytics-utils
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := $(PLATFORM_SDK_VERSION)

include $(BUILD_STATIC_JAVA_LIBRARY)
