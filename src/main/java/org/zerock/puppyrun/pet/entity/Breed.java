package org.zerock.puppyrun.pet.entity;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zerock.puppyrun.common.exception.InvalidValueException;

@Getter
@RequiredArgsConstructor
public enum Breed {

    // --- 소형견 ---
    MALTESE("101", "말티즈", "#FFFFFF", 2.0, 4.0),            // 흰색
    POODLE("102", "푸들", "#A0522D", 3.0, 6.0),             // 갈색 (Sienna)
    POMERANIAN("103", "포메라니안", "#D2691E", 1.5, 3.5),      // 오렌지/초콜릿 (Chocolate)
    CHIHUAHUA("104", "치와와", "#F5DEB3", 1.5, 3.0),          // 황갈색 (Wheat)
    SHIH_TZU("105", "시츄", "#DEB887", 4.0, 7.5),            // 금색/갈색 (Burlywood)
    YORKSHIRE_TERRIER("106", "요크셔테리어", "#708090", 2.0, 3.2), // 스틸블루/회색 (SlateGray)
    BICHON_FRISE("107", "비숑 프리제", "#FFFFFF", 3.0, 6.0),   // 흰색
    DACHSHUND("108", "닥스훈트", "#8B4513", 4.0, 9.0),         // 갈색 (SaddleBrown)
    MINIATURE_SCHNAUZER("109", "미니어처 슈나우저", "#808080", 5.0, 8.0), // 회색 (Gray)
    MALTIPOO("110", "말티푸", "#F5DEB3", 2.5, 5.0),           // 크림색 (Wheat) - 믹스지만 인기 견종이라 추가

    // --- 중형견 ---
    WELSH_CORGI("201", "웰시코기", "#CD853F", 10.0, 14.0),     // 황갈색 (Peru)
    BEAGLE("202", "비글", "#8B4513", 9.0, 11.0),             // 갈색 (SaddleBrown)
    SHIBA_INU("203", "시바견", "#DAA520", 8.0, 11.0),          // 황금색 (GoldenRod)
    BORDER_COLLIE("204", "보더콜리", "#000000", 14.0, 20.0),    // 검은색 (Black)
    FRENCH_BULLDOG("205", "프렌치 불독", "#000000", 8.0, 13.0),  // 검은색/브린들
    COCKER_SPANIEL("206", "코카 스파니엘", "#D2B48C", 10.0, 15.0), // 황갈색 (Tan)
    JINDO_DOG("207", "진돗개", "#FFFFF0", 15.0, 23.0),         // 아이보리 (Ivory)
    SPITZ("208", "스피츠", "#FFFFFF", 6.0, 10.0),             // 흰색

    // --- 대형견 ---
    GOLDEN_RETRIEVER("301", "골든 리트리버", "#F0E68C", 25.0, 34.0), // 골드/크림 (Khaki)
    LABRADOR_RETRIEVER("302", "래브라도 리트리버", "#F5F5DC", 25.0, 36.0), // 베이지 (Beige)
    SAMOYED("303", "사모예드", "#FFFFFF", 16.0, 30.0),        // 흰색
    SIBERIAN_HUSKY("304", "시베리안 허스키", "#A9A9A9", 16.0, 27.0), // 회색 (DarkGray)
    GERMAN_SHEPHERD("305", "저먼 셰퍼드", "#553518", 30.0, 40.0), // 진한 갈색/검정
    ROTTWEILER("306", "로트와일러", "#000000", 35.0, 60.0),     // 검은색
    OLD_ENGLISH_SHEEPDOG("307", "올드 잉글리쉬 쉽독", "#D3D3D3", 27.0, 45.0), // 회색/흰색

    // --- 기타 ---
    OTHER("000", "기타", "#D3D3D3", 0.0, 0.0);               // 옅은 회색 (LightGray)

    private final String code;
    private final String koreanName;
    private final String BasicColorHex;     // 대표 색상 (Hex Code)
    private final double avgWeightMin; // 평균 최소 몸무게 (kg)
    private final double avgWeightMax; // 평균 최대 몸무게 (kg)


    /**
     * 문자열 코드값을 입력받아 해당하는 Breed Enum 상수를 반환합니다.
     *
     * @param code 견종 코드 (e.g., "101", "201")
     * @return 코드에 해당하는 Breed 상수
     * @throws InvalidValueException 정의되지 않은 코드일 경우 예외 발생
     */
    public static Breed fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("잘못된 견종 코드입니다: " + code));
    }
}
