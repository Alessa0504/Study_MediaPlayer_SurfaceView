package utils

/**
 * @Description:
 * @author zouji
 * @date 2024/7/11
 */
object PlayerUtils {
    /**
     * 转换毫秒数成“分、秒”，如“01:53”。若超过60分钟则显示“时、分、秒”，如“01:01:30”
     *
     * @param time 时间（毫秒）
     * @return 转换后的字符串
     */
    @JvmStatic
    fun convertLongTimeToStr(time: Long): String {
        val ss = 1000
        val mi = ss * 60
        val hh = mi * 60

        val hour = time / hh
        val minute = (time - hour * hh) / mi
        val second = (time - hour * hh - minute * mi) / ss

        val strHour = if (hour < 10) "0$hour" else "$hour"
        val strMinute = if (minute < 10) "0$minute" else "$minute"
        val strSecond = if (second < 10) "0$second" else "$second"

        return if (hour > 0) {
            "$strHour:$strMinute:$strSecond"
        } else {
            "$strMinute:$strSecond"
        }
    }
}