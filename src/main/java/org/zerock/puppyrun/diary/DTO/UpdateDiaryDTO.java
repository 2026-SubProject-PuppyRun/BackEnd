package org.zerock.puppyrun.diary.DTO;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import org.zerock.puppyrun.weather.DTO.PrecipitationType;
import org.zerock.puppyrun.weather.DTO.SkyType;

@Builder
public record UpdateDiaryDTO(
        String title,
        String content,
        LocalDateTime writingTime,
        String temp,
        SkyType sky,
        PrecipitationType pty,
        List<String> images
) {
}
