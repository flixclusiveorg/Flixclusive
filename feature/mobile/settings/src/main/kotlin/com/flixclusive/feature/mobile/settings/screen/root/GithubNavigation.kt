package com.flixclusive.feature.mobile.settings.screen.root

import com.flixclusive.core.util.common.GithubConstant.GITHUB_REPOSITORY_URL

internal enum class GithubNavigation(
    val uri: String,
) {
    FEATURE_REQUEST(
        uri = "$GITHUB_REPOSITORY_URL/issues/new?assignees=&labels=enhancement&projects=&template=request_feature.yml"
    ),
    BUG_REPORT(
        uri = "$GITHUB_REPOSITORY_URL/issues/new?assignees=&labels=bug&projects=&template=report_issue.yml"
    ),
    REPOSITORY(
        uri = GITHUB_REPOSITORY_URL
    ),
}
