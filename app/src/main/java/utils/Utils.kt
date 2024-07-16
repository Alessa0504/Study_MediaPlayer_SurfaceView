package utils
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object Utils {

    @JvmStatic
    fun copy(context: Context, assetsName: String, savePath: String, saveName: String) {
        val filename = "$savePath/$saveName"
        val dir = File(savePath)
        // 如果目录不存在，创建这个目录
        if (!dir.exists()) dir.mkdir()
        try {
            if (!File(filename).exists()) {
                val inputStream: InputStream = context.resources.assets.open(assetsName)
                val outputStream = FileOutputStream(filename)
                val buffer = ByteArray(7168)
                var count: Int
                while (inputStream.read(buffer).also { count = it } > 0) {
                    outputStream.write(buffer, 0, count)
                }
                outputStream.close()
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}