package inu.appcenter.intip_android.ui.login.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import inu.appcenter.intip_android.ui.utils.K

@Composable
fun AgreementText(
    onOpenUrl: (url: String) -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedText = buildAnnotatedString {
        append("로그인 시 ")

        // 이용약관 링크
        pushStringAnnotation(
            tag = "URL",
            annotation = K.TERMS_OF_USE_URL
        )
        withStyle(
            style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)
        ) {
            append("이용약관")
        }
        pop()

        append(" 및 ")

        // 개인정보 처리지침 링크
        pushStringAnnotation(
            tag = "URL",
            annotation = K.PRIVACY_POLICY_URL
        )
        withStyle(
            style = SpanStyle(color = Color.Magenta, textDecoration = TextDecoration.Underline)
        ) {
            append("개인정보 처리지침")
        }
        pop()

        append("에 동의한 것으로 간주됩니다.")
    }

    BasicText(
        text = annotatedText,
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offsetPosition ->
                textLayoutResult?.let { layoutResult ->
                    val position = layoutResult.getOffsetForPosition(offsetPosition)
                    annotatedText.getStringAnnotations(
                        tag = "URL",
                        start = position,
                        end = position
                    ).firstOrNull()?.let { annotation ->
                        onOpenUrl(annotation.item)
                    }
                }
            }
        }
    )
}
