package org.zerock.puppyrun.tracking.util;

public class PaceConverter {

    // 프론트엔드 Request ("10'30\"") -> DB 저장용 Double (630.0초)
    public static Double toDouble(String paceString) {
        if (paceString == null || paceString.isBlank() || !paceString.contains("'")) {
            return 0.0;
        }

        // 쌍따옴표 제거 및 분리 (10'30 -> [10, 30])
        String cleaned = paceString.replace("\"", "");
        String[] parts = cleaned.split("'");

        int minutes = Integer.parseInt(parts[0]);
        int seconds = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        return (double) (minutes * 60 + seconds);
    }

    // DB 조회용 Double (630.0초) -> 프론트엔드 Response ("10'30\"")
    public static String toString(Double paceSeconds) {
        if (paceSeconds == null || paceSeconds <= 0) {
            return "0'00";
        }

        int totalSeconds = (int) Math.round(paceSeconds);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%d'%02d", minutes, seconds);
    }
}
