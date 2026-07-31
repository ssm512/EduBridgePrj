package com.edu.common.dto;

/**
 * 페이지네이션 바에 표시할 페이지 번호 그룹 (예: 1~10, 11~20 ...).
 *
 * PageResponse.totalPages 를 그대로 다 나열하면 데이터가 쌓여서 페이지 수가 늘어날 때
 * 페이징 바 한 줄에 번호가 계속 늘어나는 문제가 있어서, groupSize 단위로 잘라 시작/끝 페이지만
 * 화면(Thymeleaf)에 넘겨준다. 정수 나눗셈은 Thymeleaf(SpEL) 표현식보다 Java 쪽에서 계산하는 게
 * 타입 강제 관련 실수 없이 안전해서 여기서 처리함.
 *
 * @param start 현재 그룹의 첫 페이지 번호 (1부터 시작)
 * @param end   현재 그룹의 마지막 페이지 번호 (totalPages 를 넘지 않음)
 */
public record PageGroup(int start, int end) {

    public static PageGroup of(int currentPage, int totalPages, int groupSize) {
        if (totalPages <= 0) {
            return new PageGroup(1, 1);
        }
        int start = (currentPage - 1) / groupSize * groupSize + 1;
        int end = Math.min(start + groupSize - 1, totalPages);
        return new PageGroup(start, end);
    }
}
