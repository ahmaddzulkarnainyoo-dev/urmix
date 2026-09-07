package com.winatra.urmix.shared.app.platform

import org.koin.core.annotation.Singleton

@Singleton(binds = [BuildInfo::class])
class JVMBuildInfo : BuildInfo {
    override val isReleaseApk: Boolean = false
    override val isDebug: Boolean = false
}
