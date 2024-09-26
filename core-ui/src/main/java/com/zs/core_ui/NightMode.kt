package com.zs.core_ui

enum class NightMode {
    /**
     * Night mode which uses always uses a light mode, enabling {@code notnight} qualified
     * resources regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    YES,

    /**
     * Night mode which uses always uses a dark mode, enabling {@code night} qualified
     * resources regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    NO,

    /**
     * Mode which uses the system's night mode setting to determine if it is night or not.
     *
     * @see #setLocalNightMode(int)
     */
    FOLLOW_SYSTEM,

    /**
     * Night mode which uses a dark mode when the system's 'Battery Saver' feature is enabled,
     * otherwise it uses a 'light mode'. This mode can help the device to decrease power usage,
     * depending on the display technology in the device.
     *
     * <em>Please note: this mode should only be used when running on devices which do not
     * provide a similar device-wide setting.</em>
     *
     * @see #setLocalNightMode(int)
     */
    AUTO_BATTER,

    /**
     * Night mode which switches between dark and light mode depending on the time of day
     * (dark at night, light in the day).
     *
     * The calculation used to determine whether it is night or not makes use of the location
     * APIs (if this app has the necessary permissions). This allows us to generate accurate
     * sunrise and sunset times. If this app does not have permission to access the location APIs
     * then we use hardcoded times which will be less accurate.
     */
    AUTO_TIME
}