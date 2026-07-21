package com.edu.domain.grade.aigrading.mapper;

import com.edu.domain.grade.aigrading.vo.ExamDocumentVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * exam_documents 매퍼 (AIG-01, AIG-02). XML: resources/mapper/grade/aigrading/ExamDocumentMapper.xml
 */
@Mapper
public interface ExamDocumentMapper {

    /** AIG-01 시험자료 메타데이터 등록. useGeneratedKeys로 examDocumentId가 채워진다 */
    int insertDocument(ExamDocumentVo document);

    /** 시험자료 단건 조회 (업로드 응답 재조회용) */
    ExamDocumentVo selectByDocumentId(@Param("examDocumentId") Long examDocumentId);

    /** AIG-02 문항 자동 추출용 - 이 시험에 업로드된 문제지/정답지 전체 조회 (page_no 순) */
    List<ExamDocumentVo> selectByExamId(@Param("examId") Long examId);

    /** examId가 실제 존재하는 시험인지 확인 (업로드 전 404 검증용) */
    boolean existsExam(@Param("examId") Long examId);
}
