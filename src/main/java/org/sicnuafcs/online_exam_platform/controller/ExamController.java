package org.sicnuafcs.online_exam_platform.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.loadtime.Aj;
import org.sicnuafcs.online_exam_platform.config.DozerBeanMapperConfigure;
import org.sicnuafcs.online_exam_platform.config.exception.AjaxResponse;
import org.sicnuafcs.online_exam_platform.config.exception.CustomException;
import org.sicnuafcs.online_exam_platform.config.exception.CustomExceptionType;
import org.sicnuafcs.online_exam_platform.dao.QuestionRepository;
import org.sicnuafcs.online_exam_platform.dao.StuExamRepository;
import org.sicnuafcs.online_exam_platform.dao.*;
import org.sicnuafcs.online_exam_platform.model.*;
import org.sicnuafcs.online_exam_platform.service.ExamService;
import org.sicnuafcs.online_exam_platform.service.Impl.ExamServiceImpl;
import org.sicnuafcs.online_exam_platform.service.JudgeService;
import org.sicnuafcs.online_exam_platform.service.QuestionService;
import org.sicnuafcs.online_exam_platform.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import springfox.documentation.spring.web.json.Json;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;


@Slf4j
@Controller
@EnableAsync
@RequestMapping("/exam")
public class ExamController {
    @Autowired
    QuestionService questionService;
    @Autowired
    ExamService examService;
    @Autowired
    JudgeService judgeService;
    @Autowired
    StuExamRepository stuExamRepository;
    @Autowired
    QuestionRepository questionRepository;
    @Autowired
    ExamRepository examRepository;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    TeacherRepository teacherRepository;
    @Autowired
    DozerBeanMapperConfigure dozerBeanMapperConfigure;
    @Autowired
    RedisUtils redisUtils;

    @PostMapping("/addQuestion")
    public @ResponseBody
    AjaxResponse saveQuestion(@Valid @RequestBody GetQuestion getQuestion) throws Exception {
        //先判断是否为添加题目
        Long question_id = getQuestion.getQuestion_id();
        Future<String> future = null;
        if (question_id == null) {
            question_id = redisUtils.incr("question_id");   //添加题目 id不存在 就新建一个question_id
            getQuestion.setQuestion_id(question_id);


            //如果是编程题
            if (getQuestion.getType() == (GetQuestion.Type.Normal_Program) || getQuestion.getType() == (GetQuestion.Type.SpecialJudge_Program)) {
                //去question类中找到type
                int type = 0; //类型1:normal;类型2：special judge
                if (getQuestion.getType() == GetQuestion.Type.Normal_Program) {   //判断编程题目类型
                    type = 1;
                } else {
                    type = 2;
                }
                future = judgeService.writeFile(question_id, type, getQuestion);
            }
        }
        else {
            //如果为修改 而且是编程题 删除之前的文件并重新创建
            if (getQuestion.getType() == (GetQuestion.Type.Normal_Program) || getQuestion.getType() == (GetQuestion.Type.SpecialJudge_Program)) {
                //删除
                judgeService.deleteFile(getQuestion.getQuestion_id());

                //再创建 去question类中找到type
                int type = 0; //类型1:normal;类型2：special judge
                if (getQuestion.getType() == GetQuestion.Type.Normal_Program) {   //判断编程题目类型
                    type = 1;
                } else {
                    type = 2;
                }
                future = judgeService.writeFile(question_id, type, getQuestion);
            }
        }

        questionService.saveQuestion(getQuestion);  //保存到question表
        try {
            future.get();
        } catch (Exception e) {
            throw new CustomException(CustomExceptionType.OTHER_ERROR, e.getMessage());
        }

        log.info("题目 添加/更新 成功");
        if (getQuestion.getType() == (GetQuestion.Type.Normal_Program) || getQuestion.getType() == (GetQuestion.Type.SpecialJudge_Program)) {
            judgeService.addTestCase(getQuestion);   //保存到test_case表
            log.info("添加/更新 测试用例成功");
        }
        return AjaxResponse.success(question_id);
    }

    @PostMapping("/addExam")
    public @ResponseBody
    AjaxResponse saveToExam(@Valid @RequestBody Exam exam) throws Exception {
        long exam_id = examService.saveToExam(exam);
        log.info("添加/更新 试卷成功");
        return AjaxResponse.success(exam_id);
    }

    @PostMapping("/judgeProgram")
    public @ResponseBody
    AjaxResponse judge(@Valid @RequestBody Program program, HttpServletRequest request) throws Exception {
        Map userInfo = (Map) request.getSession().getAttribute("userInfo");
        String stu_id = String.valueOf(userInfo.get("id"));
        JSONObject json = judgeService.judge(program.getCode(), program.getLanguage(), program.getQuestion_id());
        log.info("判题成功");
        JudgeResult judgeResult = judgeService.transformToResult(json, stu_id, program.getCode(), program.getLanguage(), program.getQuestion_id(), program.getExam_id());
        return AjaxResponse.success(judgeResult);
    }

    @PostMapping("/addQuestionToExam")
    public @ResponseBody
    AjaxResponse saveQuestionToExam(@Valid @RequestBody ExamQuestion examQuestion) throws Exception {
        examService.saveQuestionToExam(examQuestion);
        log.info("添加/编辑 试题到试卷成功");
        return AjaxResponse.success();
    }

    @PostMapping("/getStuExam")
    public @ResponseBody
    AjaxResponse getStuExam(@RequestBody String str) throws Exception {
        //Long exam_id, String stu_id
        long exam_id =Long.parseLong(JSON.parseObject(str).get("exam_id").toString());
        String stu_id = JSON.parseObject(str).get("stu_id").toString();
//        System.out.println(str);
//        System.out.println(exam_id);
//        System.out.println(stu_id);
        ArrayList<StuExam> stuExamArrayList = new ArrayList<>();
        stuExamArrayList = stuExamRepository.getByExam_idAndStu_id(exam_id, stu_id);
        log.info("获取学号为：" + stu_id + "同学的试卷成功");
        return AjaxResponse.success(stuExamArrayList);
    }

    @PostMapping("/getQuestion")
    public @ResponseBody
    AjaxResponse getQuestion(@RequestBody String str) throws Exception {
        Question question;
        Long question_id = Long.parseLong(JSON.parseObject(str).get("question_id").toString());
        question = questionRepository.findById(question_id).get();
        log.info("获取questionid为：" + question_id + "的题目成功");
        return AjaxResponse.success(question);
    }

    @PostMapping("/stuExamInfo")
    public @ResponseBody
    AjaxResponse stuExamInfo(@RequestBody String str) throws Exception {
        long exam_id =Long.parseLong(JSON.parseObject(str).get("exam_id").toString());
        Exam exam = examRepository.findExamByExam_id(exam_id);
        log.info("exam:" + exam.toString());
        Map<String, Object> ret = new HashMap<>();
        ret.put("name", exam.getName());
        ret.put("co_name", courseRepository.getNameByCo_id(exam.getCo_id()));
        ret.put("tea_name", teacherRepository.getNameByTea_id(exam.getTea_id()));
        ret.put("begin_time", exam.getBegin_time());
        ret.put("last_time", exam.getLast_time());
        if (exam.getProgress_status() == Exam.ProgressStatus.ING) {
            ret.put("status", 1);
        } else if (exam.getProgress_status() == Exam.ProgressStatus.WILL) {
            ret.put("status", 0);
        } else{
            ret.put("status", -1);
        }
        return AjaxResponse.success(ret);
    }
}

