package org.zerock.puppyrun.notification.controller.response;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.zerock.puppyrun.notification.entity.NotificationSettings;
import org.zerock.puppyrun.notification.entity.NotificationType;

public record NotificationOptionsResponse(
        boolean isPushAgreed,
        List<NotificationOptions> notificationOptions
) {
    record NotificationOptions(
            String prefix,
            List<option> options
    ) {
        public static List<NotificationOptions> of(Set<NotificationType> optOuts) {
            // 모든 NotificationType을 순회하면서 허용/차단 여부를 매핑한 뒤,
            // typeCode의 접두사를 기준으로 그룹화(groupingBy)합니다.
            return Arrays.stream(NotificationType.values())
                    .map(type -> new option(
                            type.getCode(),
                            !optOuts.contains(type) // optOuts에 없어야(true) 알림 허용
                    ))
                    .collect(Collectors.groupingBy(
                            op -> op.optionCode().split("_")[0] // '_'를 기준으로 자른 뒤 첫 번째 값(접두사) 사용
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> new NotificationOptions(
                            entry.getKey(),
                            entry.getValue())
                    ).toList();
        }

    }

    record option(
            String optionCode,
            boolean enabled
    ) {
    }

    public static NotificationOptionsResponse of(NotificationSettings setting, Set<NotificationType> optOuts) {
        return new NotificationOptionsResponse(setting.isPushAgreed(), NotificationOptions.of(optOuts));
    }
}
