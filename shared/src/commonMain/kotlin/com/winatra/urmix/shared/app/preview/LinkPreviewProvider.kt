/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.winatra.urmix.shared.app.model.Link

/**
 * Preview provider for composable working with [Link]
 */
class LinkPreviewProvider : PreviewParameterProvider<Link> {

    override val values: Sequence<Link>
        get() = sequenceOf(
            Link(
                title = "About URMIX",
                description = "URMIX is developed by WINATRA and built on the open-source architecture and engine of NewPipe Core.",
                action = "View WINATRA",
                url = "https://winatra.com/urmix"
            )
        )
}
