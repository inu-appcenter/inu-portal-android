package inu.appcenter.intip_android

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtil {

    // 전달받은 URI의 파일을 앱의 캐시 디렉토리에 복사하여 새로운 파일로 생성
    fun copyUriToCache(context: Context, uri: Uri): Uri {

        // URI로부터 입력 스트림 열기
        val inputStream = context.contentResolver.openInputStream(uri)

        // 캐시 디렉토리에 현재 시간을 기반으로 한 파일 생성
        val file = File(
            context.cacheDir,
            "picked_image_${System.currentTimeMillis()}.jpg"
        )

        // 파일 출력 스트림 생성
        val outputStream = FileOutputStream(file)

        // 입력 스트림의 내용을 출력 스트림으로 복사
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        // 생성된 캐시 파일의 URI 반환
        return Uri.fromFile(file)
    }
}