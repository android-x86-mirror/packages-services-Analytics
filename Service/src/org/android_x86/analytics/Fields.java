/*
 * Copyright 2016 Jide Technology Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.android_x86.analytics;

class Fields {

    enum Metric {
        METRIC_POWER_ON_NOT_INCLUDE_SLEEP(1);

        private final int value;
        Metric(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    enum FieldEnum {
        // Google analytics fields, see http://goo.gl/M6dK2U
        // Common fields
        APP_ID(1),
        APP_VERSION(2),
        APP_NAME(3),
        SCREEN_NAME(4);

        private final int value;
        FieldEnum(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    // Custom dimension
    enum Dimension {
        DIMENSION_BUILD_TYPE(1),
        DIMENSION_BUILD_FLAVOR(2),
        DIMENSION_DEVICE(3),
        DIMENSION_MODEL(4),
        DIMENSION_BUILD_VERSION(5),
        DIMENSION_POWER_TYPE(6),
        DIMENSION_INPUT_TYPE(7),
        DIMENSION_DISPLAY_TYPE(8),
        DIMENSION_TAG(9),
        DIMENSION_NETWORK_TYPE(10),
        DIMENSION_RESERVED(11),
        DIMENSION_RESOLUTION(12),
        DIMENSION_DENSITY(13);

        private final int value;
        Dimension(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    enum Type {
        // Use int_key, see FieldEnum
        DEFAULT(0),
        // Use int_key, correspond to google analytics' custom dimension
        CUSTOM_DIMENSION(1),
        // Use int_key, correspond to google analytics' custom metric
        CUSTOM_METRIC(2),
        // Use keys
        CUSTOM_GENERAL_LOG(3);

        private final int value;
        Type(int value) { this.value = value; }
        public int getValue() { return value; }
    }
}
