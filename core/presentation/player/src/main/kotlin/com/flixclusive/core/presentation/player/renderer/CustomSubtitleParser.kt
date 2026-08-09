package com.flixclusive.core.presentation.player.renderer

import androidx.annotation.OptIn
import androidx.compose.ui.util.fastMap
import androidx.media3.common.Format
import androidx.media3.common.text.Cue
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.ssa.SsaParser
import com.flixclusive.core.presentation.player.CuesProvider
import com.flixclusive.core.presentation.player.model.CueWithTiming.Companion.toCue
import com.flixclusive.core.presentation.player.util.SubtitleFormatSniffer
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.infoLog
import org.mozilla.universalchardet.UniversalDetector

/**
 *
 * Code from: [Cloudstream3 TextRenderer](https://github.com/recloudstream/cloudstream/blob/743527aa4060eddb6649a61b01fb009b3d77a9d2/app/src/main/java/com/lagradost/cloudstream3/ui/subtitles/SubtitlesFragment.kt#L55)
 *
 *
 * @param fallbackFormat used to create a decoder based on mimetype if the subtitle string is not
 * enough to identify the subtitle format.
 **/
@OptIn(UnstableApi::class)
@Suppress("ktlint:standard:max-line-length")
internal class CustomSubtitleParser(
    private val fallbackFormat: Format?,
    private val cuesProvider: CuesProvider,
) : SubtitleParser {
    private var realDecoder: SubtitleParser? = null

    /**
     * Try to convert the byte array to a readable string using a custom charset detection
     *
     * @return Pair of the decoded string and the charset used
     * */
    private fun ByteArray.toReadableSubtitle(): String {
        val encoding =
            try {
                val detector = UniversalDetector()
                detector.handleData(this, 0, size)
                detector.dataEnd()

                val encoding = detector.detectedCharset // "windows-1256"

                infoLog("Detected encoding with charset $encoding")
                encoding ?: UTF_8
            } catch (e: Exception) {
                errorLog("Failed to detect encoding throwing error")
                e.printStackTrace()
                UTF_8
            }

        return try {
            String(this, charset(encoding))
        } catch (e: Exception) {
            errorLog("Failed to parse using encoding $encoding")
            e.printStackTrace()
            decodeToString()
        }
    }

    /**
     * This way we read the subtitle file and decide what decoder to use instead of relying fully on mimetype.
     *
     * Detection itself lives in [SubtitleFormatSniffer], shared with [com.flixclusive.core.presentation.player.util.MimeTypeParser]'s
     * pre-playback guess.
     * */
    private fun getSubtitleParser(data: String): SubtitleParser? {
        val trimmedText = SubtitleFormatSniffer.trimInvisible(data)
        val mimeType = SubtitleFormatSniffer.sniff(trimmedText) ?: fallbackFormat?.sampleMimeType
        return SubtitleFormatSniffer.toParser(mimeType, fallbackFormat?.initializationData)
    }

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>,
    ) {
        val customOutput =
            Consumer<CuesWithTiming> { data ->
                val updatedCues = data.cues.fastMap { cue ->
                    // See https://github.com/google/ExoPlayer/issues/7934

                    // Personal note:
                    // Some VTTs already has a set line position
                    // In order to make it even more consistent, all cues could
                    // Be set to DIMEN_UNSET instead and use bottomPaddingFraction()
                    // to mutate the bottom padding of the cues
                    cue
                        .buildUpon()
                        .apply {
                            if (cue.line == -1f) {
                                setLine(Cue.DIMEN_UNSET, Cue.LINE_TYPE_NUMBER)
                            }
                        }.setSize(Cue.DIMEN_UNSET)
                        .build()
                }

                cuesProvider.addCue(data.toCue())

                output.accept(
                    CuesWithTiming(
                        // cues =
                        updatedCues,
                        // startTimeUs =
                        data.startTimeUs,
                        // durationUs =
                        data.durationUs,
                    )
                )
            }

        try {
            val inputString = data.toReadableSubtitle()
            infoLog("Current subtitle to preview: ${inputString.take(30)}")

            if (inputString.isNotBlank()) {
                var str = inputString.trimStr()
                realDecoder = realDecoder
                    ?: getSubtitleParser(inputString)
                        .also { infoLog("Parser selected: $it") }

                if (realDecoder !is SsaParser) {
                    // TODO: Apply styles there's a need to do so in the future.
                    //       For example, if user wants uppercased subtitles
                    //       Or if user wants to remove certain styles

                    // TODO: Make this optional in the future if user wants to keep bloat.
                    //       Add this feature in DataStore.
                    bloatRegex.forEach { rgx ->
                        str = str.replace(rgx, "\n")
                    }
                }

                val array = str.toByteArray()
                realDecoder?.parse(
                    // data =
                    array,
                    // offset =
                    minOf(array.size, offset),
                    // length =
                    minOf(array.size, length),
                    // outputOptions =
                    outputOptions,
                    // output =
                    customOutput,
                )
            }
        } catch (e: Exception) {
            errorLog(e)
        }
    }

    override fun getCueReplacementBehavior(): Int {
        // CUE_REPLACEMENT_BEHAVIOR_REPLACE seems most compatible, change if required
        return realDecoder?.cueReplacementBehavior ?: Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE
    }

    override fun reset() {
        super.reset()
        cuesProvider.clearCues()
    }

    internal companion object {
        private const val UTF_8 = "UTF-8"

        /**
         * A list of regex patterns to identify and remove common bloat/ad text found in subtitles from subtitle sources.
         * */
        private val bloatRegex =
            listOf(
                Regex(
                    pattern = """Support\s+us\s+and\s+become\s+VIP\s+member\s+to\s+remove\s+all\s+ads\s+from\s+(www\.|)OpenSubtitles(\.org|)""",
                    option = RegexOption.IGNORE_CASE,
                ),
                Regex(
                    pattern = """Please\s+rate\s+this\s+subtitle\s+at\s+.*\s+Help\s+other\s+users\s+to\s+choose\s+the\s+best\s+subtitles""",
                    option = RegexOption.IGNORE_CASE,
                ),
                Regex(
                    pattern = """Contact\s(www\.|)OpenSubtitles(\.org|)\s+today""",
                    option = RegexOption.IGNORE_CASE,
                ),
                Regex(
                    pattern = """Advertise\s+your\s+product\s+or\s+brand\s+here""",
                    option = RegexOption.IGNORE_CASE,
                ),
            )

        // val captionRegex = listOf(Regex("""(-\s?|)[\[({][\w\s]*?[])}]\s*"""))

        /**
         * Trim invisible characters from the start and non-breaking spaces from the end of the string
         * We use a custom regex to match all non-breaking spaces in unicode
         *
         * See:
         * - [https://emptycharacter.com/](https://emptycharacter.com/)
         * - [https://www.fileformat.info/info/unicode/char/200b/index.htm](https://www.fileformat.info/info/unicode/char/200b/index.htm)
         * */
        private fun String.trimStr(): String {
            val regex = Regex("[\u00A0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u205F]")

            return trimStart()
                .trim('\uFEFF', '\u200B')
                .replace(regex, " ")
        }
    }
}
