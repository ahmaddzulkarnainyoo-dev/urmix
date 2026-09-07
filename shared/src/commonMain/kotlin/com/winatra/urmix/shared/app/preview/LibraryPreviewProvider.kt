/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.winatra.urmix.shared.app.model.Developer
import com.winatra.urmix.shared.app.model.Library

/**
 * Preview provider for composable working with [Library]
 */
class LibraryPreviewProvider : PreviewParameterProvider<Library> {

    override val values: Sequence<Library>
        get() = sequenceOf(
            Library(
                id = "com.winatra.urmix.shared.extractor",
                name = "NewPipe Extractor",
                developers = listOf(
                    Developer(
                        name = "Team NewPipe",
                        organisationUrl = "https://winatra.com/urmix"
                    )
                ),
                licenses = listOf("GPL-3.0-or-later"),
                website = "https://github.com/TeamNewPipe/NewPipeExtractor/"
            )
        )
}
