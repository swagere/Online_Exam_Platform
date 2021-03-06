package org.sicnuafcs.online_exam_platform.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.sicnuafcs.online_exam_platform.config.exception.AjaxResponse;
import org.sicnuafcs.online_exam_platform.config.exception.CustomException;
import org.sicnuafcs.online_exam_platform.config.exception.CustomExceptionType;
import org.sicnuafcs.online_exam_platform.dao.*;
import org.sicnuafcs.online_exam_platform.model.*;
import org.sicnuafcs.online_exam_platform.service.AuthorityCheckService;
import org.sicnuafcs.online_exam_platform.service.CourseSelectionService;
import org.sicnuafcs.online_exam_platform.service.ExamService;
import org.sicnuafcs.online_exam_platform.service.HomePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/course")
public class CourseController {
    @Autowired
    CourseSelectionService courseSelectionService;
    @Autowired
    ExamRepository examRepository;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    StuCoRepository stuCoRepository;
    @Autowired
    TeacherRepository teacherRepository;
    @Autowired
    AuthorityCheckService authorityCheckService;
    @Autowired
    ExamService examService;
    @Autowired
    TeaCoRepository teaCoRepository;
    @Autowired
    HomePageService homePageService;

    /**
     * 教师首页显示课程
     * @param str
     * @param httpServletRequest
     * @return
     */
    @RequestMapping("/tea/id")
    public @ResponseBody
    AjaxResponse findTeaById(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        List<CourseVO> json = homePageService.findTeaById(tea_id);
        return AjaxResponse.success(json);

    }

    /**
     * 学生
     * 获取选课中心页面需要展示的东西
     * 班级号（需要考虑到学院？） 专业名
     * 可选的课程
     * 已选的课程
     * 以<课程名，老师>确定一门课
     * @param map
     * @return
     */
    @RequestMapping("/selection/get")
    public @ResponseBody
    AjaxResponse getCourse(@RequestBody Map map, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkStudentAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String stu_id = map.values().toString().substring(1, map.values().toString().length()-1);
        if (stu_id.equals(null) || stu_id.equals("")) {
            log.info("学号为空");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR, "学号为空");

        }
        String class_id = courseSelectionService.getClass_id(stu_id);
        String major_id = courseSelectionService.getMajor_id(stu_id);
        String major = courseSelectionService.getMajorName(major_id);   //到major类中获取name

        Map<String, Object> map1 = new HashMap<>();
        map1.put("stu_id", stu_id);
        map1.put("class_id", class_id);
        map1.put("major", major);
        return AjaxResponse.success(map1);
    }

    /**
     * 学生查询已选课程和未选课程
     * @param getCourse
     * @param httpServletRequest
     * @return
     */
    @RequestMapping("/getByStu")
    public @ResponseBody
    AjaxResponse getAlternativeCourse(@RequestBody GetCourse getCourse, HttpServletRequest httpServletRequest) {
        //authorityCheckService.checkStudentAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String stu_id = getCourse.getStu_id();
        String major_id = courseSelectionService.getMajor_id(stu_id);
        List<StuCo> stuCos = stuCoRepository.getStu_coByStu_id(stu_id);
        if (getCourse.getOption() == 1) {    //查询已选课程
            return AjaxResponse.success(courseSelectionService.getChosenCoId_TeaId(stuCos));
        }
        else if (getCourse.getOption() == 0) { //查询未选择课程
            return AjaxResponse.success(courseSelectionService.getUnChooesCoId_TeaId(stuCos, major_id));
        }
        throw new CustomException(CustomExceptionType.USER_INPUT_ERROR, "选项错误");
    }


    /**
     * 老师添加/更新课程
     * @param str
     * @return
     */
    @RequestMapping("/add")
    public @ResponseBody
    AjaxResponse add(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        String co_id = JSON.parseObject(str).get("co_id").toString();
        if (tea_id == "") {
            AjaxResponse.error(new CustomException(CustomExceptionType.USER_INPUT_ERROR, "tea_id is null"));
        }
        if (co_id == "") {
            AjaxResponse.error(new CustomException(CustomExceptionType.USER_INPUT_ERROR, "co_id is null"));
        }
        TeaCo teaCo = new TeaCo();
        teaCo.setCo_id(co_id);
        teaCo.setTea_id(tea_id);
        teaCoRepository.save(teaCo);
        return AjaxResponse.success("success");
    }
    /**
     * 老师退课
     * @param str
     * @return
     */
    @RequestMapping("/remove")
    public @ResponseBody
    AjaxResponse remove(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        String co_id = JSON.parseObject(str).get("co_id").toString();
        TeaCo teaCo = new TeaCo();
        teaCo.setCo_id(co_id);
        teaCo.setTea_id(tea_id);
        teaCoRepository.delete(teaCo);
        return AjaxResponse.success("success");
    }
    /**
     * 老师添加/更新课程,获取未教授课程
     * @param str
     * @return
     */
    @RequestMapping("/getCourseNotTea")
    public @ResponseBody
    AjaxResponse getCourseNotTea(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        List<Object> ret = courseSelectionService.getCourseNotTea(tea_id);
        return AjaxResponse.success(ret);
    }

    /**
     * 老师的课程页面
     * @param str
     * @return
     */
    @RequestMapping("/getByTea")
    public @ResponseBody
    AjaxResponse getByTea(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        String co_id = JSON.parseObject(str).get("co_id").toString();
        if (tea_id.equals("") || co_id.equals("")) {
            log.info("course/getByTea:tea_id or co_id is null");
            return AjaxResponse.error(new CustomException(CustomExceptionType.USER_INPUT_ERROR, "tea_id or co_id is null"));
        }
        List<Exam> exams = examRepository.findExamsByCo_idAAndTea_id(co_id, tea_id);
        Course course = courseRepository.findCourseByCo_id(co_id);
        List<Map<String, Object>> examRet = new ArrayList<>();
        Map<String, Object> ret = new HashMap<>();
        if (course != null) {
            ret.put("name", course.getName());
            ret.put("stu_num", stuCoRepository.findStuNumByCo_id(co_id));
            ret.put("credit", course.getCredit());
            ret.put("school_hour", course.getSchool_hour());
        } else {
            return AjaxResponse.error(new CustomException(CustomExceptionType.USER_INPUT_ERROR, "do not exists course"));
        }
        for (Exam exam : exams) {
            Map<String, Object> examsInfo = new HashMap<>();
            examsInfo.put("exam_name",exam.getName());
            if (exam.getProgress_status().equals(Exam.ProgressStatus.WILL)) {
                if (exam.is_distribute() == false) {
                    examsInfo.put("sort_id",0);
                    examsInfo.put("status", 3);
                } else {
                    examsInfo.put("sort_id", 0);
                    examsInfo.put("status", 0);
                }

            } else if (exam.getProgress_status().equals(Exam.ProgressStatus.ING)) {
                examsInfo.put("sort_id",1);
                examsInfo.put("status", 1);
            } else {
                examsInfo.put("status", 2);
                if (exam.is_judge()) {
                    examsInfo.put("sort_id",3);
                }
                else examsInfo.put("sort_id",2);
            }
            examsInfo.put("begin_time", exam.getBegin_time());
            examsInfo.put("last_time", exam.getLast_time());
            examsInfo.put("is_judge", exam.is_judge());
            examsInfo.put("exam_id", exam.getExam_id());
            examRet.add(examsInfo);
        }

        //对结果进行排序
        Collections.sort(examRet, new Comparator<Map>() {
            @Override
            public int compare(Map m1, Map m2) {
                int diff = (int)m1.get("sort_id") - (int)m2.get("sort_id");
                if (diff > 0) return 1;
                else if (diff < 0) return -1;
                return 0;
            }
        });

        ret.put("exams", examRet);
        ret.put("tea_id", tea_id);
        ret.put("teacherName", teacherRepository.getNameByTea_id(tea_id));
        return AjaxResponse.success(ret);
    }

    /**
     * 教师课程详情页
     * 老师id 课程id
     * 考试名字 时间 时长 状态（考试未开始 考试中 考试结束未评分 考试结束已评分）
     */
    @PostMapping("/getExams")
    public @ResponseBody AjaxResponse getExams(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        String co_id = JSON.parseObject(str).get("co_id").toString();
        int option = 0;
        ArrayList<Map> res = new ArrayList<>();
        List<Long> exams = examService.getExam(co_id, tea_id);
        for (Long exam : exams) {
            res.add(examService.getExamInfo(exam, option));
        }
        return AjaxResponse.success(res);
    }

    /**
     * 学生选课
     * @param str
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/saveToStuCo")
    public @ResponseBody AjaxResponse saveToStuCo(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkStudentAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String stu_id = JSON.parseObject(str).get("stu_id").toString();
        String data = JSON.parseObject(str).get("data").toString();
        courseSelectionService.saveToStuCo(data, stu_id);
        return AjaxResponse.success("success");
    }

    /**
     * 学生退课
     * @param str
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/deleteCourse")
    public @ResponseBody AjaxResponse quitCourse(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkStudentAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String stu_id = JSON.parseObject(str).get("stu_id").toString();
        String co_id = JSON.parseObject(str).get("co_id").toString();
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        courseSelectionService.quitCourse(co_id, tea_id, stu_id);
        return AjaxResponse.success("success");
    }

    /**
     * 教师获取评完分的试卷
     * tea_id
     * {exam_id,exam_name,begin_time,last_time,co_name}
     */
    @PostMapping("/getDoneExam")
    public @ResponseBody AjaxResponse getDoneExam(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        String tea_id = JSON.parseObject(str).get("tea_id").toString();
        ArrayList res = courseSelectionService.getDoneExam(tea_id);
        return AjaxResponse.success(res);
    }

    /**
     * 教师获取本堂考试所有学生成绩
     * exam_id
     * exam_name, data:{stu_id,stu_name,score}
     */
    @PostMapping("/getStuExam")
    public @ResponseBody AjaxResponse getStuExam(@RequestBody String str, HttpServletRequest httpServletRequest) {
        authorityCheckService.checkTeacherAuthority(httpServletRequest.getSession().getAttribute("userInfo"));
        Long exam_id = Long.parseLong(JSON.parseObject(str).get("exam_id").toString());
        Map res = new HashMap();
        res.put("exam_name", examService.getExamName(exam_id));
        res.put("data", courseSelectionService.getStuExam(exam_id));
        return AjaxResponse.success(res);
    }
}
