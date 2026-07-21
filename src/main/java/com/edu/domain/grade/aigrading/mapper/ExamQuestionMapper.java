package com.edu.domain.grade.aigrading.mapper;

import com.edu.domain.grade.aigrading.vo.ExamQuestionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * exam_questions 매퍼 (AIG-03/04). XML: resources/mapper/grade/aigrading/ExamQuestionMapper.xml
 */
@Mapper
public interface ExamQuestionMapper {

    /** examId가 실제 존재하는 시험인지 확인 */
    boolean existsExam(@Param("examId") Long examId);

    /**
     * 이 시험에 학생 답안 제출(exam_submissions)이 이미 있는지 확인.
     * AIG-04는 전체 교체(delete+insert) 방식이라, 채점이 시작된 뒤 문항을 바꾸면
     * answer_results가 참조하는 question_id가 끊어질 수 있어 저장 전에 막는다.
     */
    boolean existsSubmissionForExam(@Param("examId") Long examId);

    /** AIG-03 시험 문항 목록 조회 (정렬 순서 기준) */
    List<ExamQuestionVo> selectByExamId(@Param("examId") Long examId);

    /** AIG-04 저장 전 기존 문항 전체 삭제 (전체 교체 방식) */
    int deleteByExamId(@Param("examId") Long examId);

    /** AIG-04 문항 등록 (useGeneratedKeys로 questionId가 채워진다) */
    int insertQuestion(ExamQuestionVo question);
}
