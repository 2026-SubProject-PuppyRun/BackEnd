package org.zerock.puppyrun.care.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.puppyrun.care.controller.response.CareCalendarResponse;
import org.zerock.puppyrun.care.entity.AllergyRecord;
import org.zerock.puppyrun.care.entity.CareEventType;
import org.zerock.puppyrun.care.entity.MedicationRecord;
import org.zerock.puppyrun.care.entity.VaccinationRecord;
import org.zerock.puppyrun.care.repository.AllergyRecordRepository;
import org.zerock.puppyrun.care.repository.MedicationRecordRepository;
import org.zerock.puppyrun.care.repository.VaccinationRecordRepository;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.exception.InvalidValueException;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.entity.PetWeightLog;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.pet.repository.PetWeightLogRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareCalendarQueryService {

    private final PetRepository petRepository;
    private final PetWeightLogRepository petWeightLogRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final AllergyRecordRepository allergyRecordRepository;

    public CareCalendarResponse getCareCalendar(
            UserPrincipal userPrincipal,
            UUID petId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        Pet pet = petRepository.findByIdAndVerifyOwnership(petId, userPrincipal.id());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        List<CalendarEventCandidate> events = new ArrayList<>(getWeightEvents(petId, startDateTime, endExclusive));
        events.addAll(getVaccinationEvents(petId, startDate, endDate));
        events.addAll(getMedicationEvents(petId, startDateTime, endExclusive));
        events.addAll(getAllergyEvents(petId, startDate, endDate));

        List<CareCalendarResponse.CareEvent> sortedEvents = events.stream()
                .sorted(Comparator
                        .comparing(CalendarEventCandidate::sortDateTime)
                        .thenComparing(candidate -> candidate.event().eventType().name())
                        .thenComparing(candidate -> candidate.event().relatedId()))
                .map(CalendarEventCandidate::event)
                .toList();

        return CareCalendarResponse.of(pet, startDate, endDate, sortedEvents);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidValueException("조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private List<CalendarEventCandidate> getWeightEvents(
            UUID petId,
            LocalDateTime startDateTime,
            LocalDateTime endExclusive
    ) {
        return petWeightLogRepository
                .findAllByPetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        petId,
                        startDateTime,
                        endExclusive
                )
                .stream()
                .map(this::toWeightEvent)
                .toList();
    }

    private List<CalendarEventCandidate> getVaccinationEvents(UUID petId, LocalDate startDate, LocalDate endDate) {
        return vaccinationRecordRepository.findAllByPetIdAndVaccinatedAtBetweenOrderByVaccinatedAtAscCreatedAtAsc(
                        petId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toVaccinationEvent)
                .toList();
    }

    private List<CalendarEventCandidate> getMedicationEvents(
            UUID petId,
            LocalDateTime startDateTime,
            LocalDateTime endExclusive
    ) {
        return medicationRecordRepository
                .findAllByPetIdAndAdministeredAtGreaterThanEqualAndAdministeredAtLessThanOrderByAdministeredAtAscCreatedAtAsc(
                        petId,
                        startDateTime,
                        endExclusive
                )
                .stream()
                .map(this::toMedicationEvent)
                .toList();
    }

    private List<CalendarEventCandidate> getAllergyEvents(UUID petId, LocalDate startDate, LocalDate endDate) {
        return allergyRecordRepository
                .findAllByPetIdAndIdentifiedAtIsNotNullAndIdentifiedAtBetweenOrderByIdentifiedAtAscCreatedAtAsc(
                        petId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toAllergyEvent)
                .toList();
    }

    private CalendarEventCandidate toWeightEvent(PetWeightLog petWeightLog) {
        CareCalendarResponse.CareEvent event = CareCalendarResponse.CareEvent.builder()
                .date(petWeightLog.getCreatedAt().toLocalDate())
                .eventType(CareEventType.WEIGHT)
                .title("체중 기록")
                .relatedId(petWeightLog.getId())
                .displayText(petWeightLog.getWeight() + "kg")
                .build();

        return new CalendarEventCandidate(petWeightLog.getCreatedAt(), event);
    }

    private CalendarEventCandidate toVaccinationEvent(VaccinationRecord vaccinationRecord) {
        CareCalendarResponse.CareEvent event = CareCalendarResponse.CareEvent.builder()
                .date(vaccinationRecord.getVaccinatedAt())
                .eventType(CareEventType.VACCINATION)
                .title(vaccinationRecord.getVaccineName())
                .relatedId(vaccinationRecord.getId())
                .displayText(resolveVaccinationDisplayText(vaccinationRecord))
                .build();

        return new CalendarEventCandidate(vaccinationRecord.getVaccinatedAt().atStartOfDay(), event);
    }

    private CalendarEventCandidate toMedicationEvent(MedicationRecord medicationRecord) {
        CareCalendarResponse.CareEvent event = CareCalendarResponse.CareEvent.builder()
                .date(medicationRecord.getAdministeredAt().toLocalDate())
                .eventType(CareEventType.MEDICATION)
                .title(medicationRecord.getMedicationName())
                .relatedId(medicationRecord.getId())
                .displayText(medicationRecord.getDoseAmount() + " " + medicationRecord.getDoseUnit())
                .build();

        return new CalendarEventCandidate(medicationRecord.getAdministeredAt(), event);
    }

    private CalendarEventCandidate toAllergyEvent(AllergyRecord allergyRecord) {
        CareCalendarResponse.CareEvent event = CareCalendarResponse.CareEvent.builder()
                .date(allergyRecord.getIdentifiedAt())
                .eventType(CareEventType.ALLERGY)
                .title(allergyRecord.getAllergenName())
                .relatedId(allergyRecord.getId())
                .displayText(resolveAllergyDisplayText(allergyRecord))
                .build();

        return new CalendarEventCandidate(allergyRecord.getIdentifiedAt().atStartOfDay(), event);
    }

    private String resolveVaccinationDisplayText(VaccinationRecord vaccinationRecord) {
        if (vaccinationRecord.getMemo() != null && !vaccinationRecord.getMemo().isBlank()) {
            return vaccinationRecord.getMemo();
        }

        return vaccinationRecord.getHospitalName();
    }

    private String resolveAllergyDisplayText(AllergyRecord allergyRecord) {
        if (allergyRecord.getSymptom() != null && !allergyRecord.getSymptom().isBlank()) {
            return allergyRecord.getSymptom();
        }

        return allergyRecord.getMemo();
    }

    private record CalendarEventCandidate(
            LocalDateTime sortDateTime,
            CareCalendarResponse.CareEvent event
    ) {
    }
}
