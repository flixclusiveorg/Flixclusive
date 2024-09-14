package com.flixclusive.feature.mobile.preferences.component

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.util.common.GithubConstant.GITHUB_LATEST_RELEASE
import com.flixclusive.core.locale.UiText
import com.flixclusive.feature.mobile.preferences.R
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun ShareHeader() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .padding(
                horizontal = 15.dp,
                vertical = 20.dp
            )
            .background(
                LocalContentColor.current.onMediumEmphasis(emphasis = 0.2F),
                RoundedCornerShape(15)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = LocaleR.string.share_the_app),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(1F)
                .padding(start = 15.dp)
        )

        HeaderButton(
            iconId = com.flixclusive.core.ui.common.R.drawable.round_content_copy_24,
            contentDescription = stringResource(id = LocaleR.string.copy_button),
            onClick =  {
                clipboardManager.setText(AnnotatedString(GITHUB_LATEST_RELEASE))
            }
        )

        HeaderButton(
            iconId = R.drawable.round_share_24,
            contentDescription = stringResource(LocaleR.string.share_button),
            onClick =  {
                val type = "text/plain"
                val subject =  UiText.StringResource(LocaleR.string.share_message_subject).asString(context)
                val extraText = UiText.StringResource(LocaleR.string.share_message, GITHUB_LATEST_RELEASE).asString(context)
                val shareWith = "ShareWith"

                val intent = Intent(Intent.ACTION_SEND)
                    .apply {
                        this.type = type
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, extraText)
                    }

                ContextCompat.startActivity(
                    context,
                    Intent.createChooser(intent, shareWith),
                    null
                )
            }
        )
    }
}