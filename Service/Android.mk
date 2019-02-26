LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_JAVA_LIBRARIES := \
    analytics-utils \
    googleanalytics \
    org.apache.http.legacy \

LOCAL_SRC_FILES := \
    $(call all-java-files-under, src) \

LOCAL_PROTOC_OPTIMIZE_TYPE := lite

LOCAL_PACKAGE_NAME := AnalyticsService
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)
