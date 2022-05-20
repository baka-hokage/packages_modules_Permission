/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.safetycenter.cts.testing

import android.Manifest.permission.READ_DEVICE_CONFIG
import android.Manifest.permission.WRITE_DEVICE_CONFIG
import android.content.Context
import android.content.res.Resources
import android.provider.DeviceConfig
import android.provider.DeviceConfig.NAMESPACE_PRIVACY
import android.provider.DeviceConfig.Properties
import com.android.compatibility.common.util.SystemUtil.callWithShellPermissionIdentity

/** A class that facilitates working with Safety Center flags. */
// TODO(b/219553295): Add timeout flags.
object SafetyCenterFlags {

    /** Name of the flag that determines whether SafetyCenter is enabled. */
    private const val PROPERTY_SAFETY_CENTER_ENABLED = "safety_center_is_enabled"

    /** Returns whether the device supports Safety Center. */
    fun Context.deviceSupportsSafetyCenter() =
        resources.getBoolean(
            Resources.getSystem().getIdentifier("config_enableSafetyCenter", "bool", "android"))

    /** A property that allows getting and modifying [PROPERTY_SAFETY_CENTER_ENABLED]. */
    var isEnabled: Boolean
        get() =
            callWithShellPermissionIdentity(
                {
                    DeviceConfig.getBoolean(
                        NAMESPACE_PRIVACY, PROPERTY_SAFETY_CENTER_ENABLED, /* defaultValue */ false)
                },
                READ_DEVICE_CONFIG)
        set(value) {
            callWithShellPermissionIdentity(
                { setSafetyCenterEnabledWithoutPermission(value) }, WRITE_DEVICE_CONFIG)
        }

    /**
     * Sets the Safety Center device config flag to the given boolean [value], but without holding
     * the [WRITE_DEVICE_CONFIG] permission.
     *
     * [callWithShellPermissionIdentity] mutates a global state, so it is not possible to modify
     * [isEnabled] within another call to [callWithShellPermissionIdentity].
     */
    fun setSafetyCenterEnabledWithoutPermission(value: Boolean) {
        val valueWasSet =
            DeviceConfig.setProperty(
                NAMESPACE_PRIVACY,
                PROPERTY_SAFETY_CENTER_ENABLED,
                /* value = */ value.toString(),
                /* makeDefault = */ false)
        require(valueWasSet) { "Could not set Safety Center flag value to: $value" }
    }

    /**
     * Returns a snapshot of all the Safety Center flags.
     *
     * This snapshot is only taken once and cached afterwards. This must be called at least once
     * prior to modifying any flag.
     */
    val snapshot: Properties by lazy {
        callWithShellPermissionIdentity(
            { DeviceConfig.getProperties(NAMESPACE_PRIVACY) }, READ_DEVICE_CONFIG)
    }

    /** Resets the Safety Center flags based on the given [snapshot]. */
    fun reset(snapshot: Properties) {
        callWithShellPermissionIdentity(
            { DeviceConfig.setProperties(snapshot) }, WRITE_DEVICE_CONFIG)
    }

    /** Returns the [PROPERTY_SAFETY_CENTER_ENABLED] of the Safety Center flags snapshot. */
    fun Properties.isSafetyCenterEnabled() =
        getBoolean(PROPERTY_SAFETY_CENTER_ENABLED, /* defaultValue */ false)
}
