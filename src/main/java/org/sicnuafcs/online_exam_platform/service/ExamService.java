package org.sicnuafcs.online_exam_platform.service;

import org.sicnuafcs.online_exam_platform.model.Exam;
import org.sicnuafcs.online_exam_platform.model.ExamQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ExamService {
    long saveToExam(Exam exam) throws Exception;
    void saveQuestionToExam(ExamQuestion examQuestion) throws Exception;
    void distributeExamToStudent(long exma_id, String co_id) throws Exception;
    void saveToStuExam(String data, Long exam_id, String stu_id);
    Map getDiscussion(Long exam_id);
    Map getExamInfo(Long exam_id,int option);
    List<Long> getExam(String tea_id, String co_id);
}
