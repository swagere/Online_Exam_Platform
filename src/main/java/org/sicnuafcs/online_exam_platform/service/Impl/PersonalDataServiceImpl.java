package org.sicnuafcs.online_exam_platform.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.sicnuafcs.online_exam_platform.config.exception.CustomException;
import org.sicnuafcs.online_exam_platform.config.exception.CustomExceptionType;
import org.sicnuafcs.online_exam_platform.dao.StudentRepository;
import org.sicnuafcs.online_exam_platform.dao.TeacherRepository;
import org.sicnuafcs.online_exam_platform.model.Student;
import org.sicnuafcs.online_exam_platform.model.Teacher;
import org.sicnuafcs.online_exam_platform.service.PersonalDataService;
import org.sicnuafcs.online_exam_platform.service.SendMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
@Slf4j
@Service
public class PersonalDataServiceImpl implements PersonalDataService {


    @Resource
    TeacherRepository teacherRepository;
    @Resource
    StudentRepository studentRepository;
    @Autowired
    SendMailService sendMailService;

    @Override
    public Optional<Teacher> getTeacherData(String ID) {
        Optional<Teacher> teacherList = teacherRepository.findById(ID);
        teacherList.get().setPassword(null);
        teacherList.get().setCode(null);
        return teacherList;
    }

    @Override
    public Optional<Student> getStudentData(String ID) {
        Optional<Student> studentList = studentRepository.findById(ID);
        studentList.get().setPassword(null);
        studentList.get().setCode(null);
        return studentList;
    }

    @Override
    public Teacher updateTeacherPassword(String ID,Map<String, Object> params) {
        Teacher teacher = teacherRepository.findById(ID).get();
        String newPassword = (String) params.get("newPassword"),
                newTelephone = (String) params.get("newTelephone"),
                code = (String) params.get("code");
        //邮箱验证：验证码和邮箱是否一致；
        if (!sendMailService.verification(teacher.getEmail(), code)) {
            log.info("邮箱验证不一致");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"邮箱验证失败");
        }
        if (newPassword.length() < 8) {
            log.info("密码位数过少");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"密码位数过少");
        }
//原始密码相同？
        Optional<Teacher> teacherPhoneList = teacherRepository.findByTelephone(newTelephone);
        if ( teacherPhoneList.isPresent() && !teacherPhoneList.get().getTea_id().equals(ID)) {
            log.info("该电话已被注册");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"该电话已被注册");
        }
        try {
            teacher.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            log.info("密码加密成功");
        }catch (Exception e) {
            log.info("密码加密失败");
            throw new CustomException(CustomExceptionType.SYSTEM_ERROR,"密码加密失败");
        }
        if(newTelephone!=null)
            teacher.setTelephone(newTelephone);
        Teacher newTeacherData = teacherRepository.save(teacher);
            log.info(String.valueOf(newTeacherData));
        newTeacherData.setPassword(null);
        newTeacherData.setCode(null);
        return newTeacherData;
    }

    @Override
    public Student updateStudentPassword(String ID, Map<String, Object> params) {
        Student student = studentRepository.findById(ID).get();
        String newPassword = (String) params.get("newPassword"),
                newTelephone = (String) params.get("newTelephone"),
                code = (String) params.get("code");
        //邮箱验证：验证码和邮箱是否一致；
        if (!sendMailService.verification(student.getEmail(), code)) {
            log.info("邮箱验证不一致");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"邮箱验证失败");
        }
        if (newPassword.length() < 8) {
            log.info("密码位数过少");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"密码位数过少");
        }
        Optional<Student> studentPhoneList = studentRepository.findByTelephone(newTelephone);
        if ( studentPhoneList.isPresent() && !studentPhoneList.get().getStu_id().equals(ID)) {
                log.info("该电话已被注册");
                throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"该电话已被注册");
        }
        try {
            student.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            log.info("密码加密成功");
        }catch (Exception e) {
            log.info("密码加密失败");
            throw new CustomException(CustomExceptionType.SYSTEM_ERROR,"密码加密失败");
        }
        if(newTelephone!=null){
            student.setTelephone(newTelephone);
        }
        Student newStudentData = studentRepository.save(student);
            log.info(String.valueOf(newStudentData));
        newStudentData.setPassword(null);
        newStudentData.setCode(null);
        return newStudentData;
    }
    @Override
    public Teacher editTeacherBaseData(String ID,Teacher newTeacherData) {
        Teacher teacher = teacherRepository.findById(ID).get();

        teacher.setName(newTeacherData.getName());
        teacher.setQq(newTeacherData.getQq());
        teacher.setWeixin(newTeacherData.getWeixin());
        teacher.setSex(newTeacherData.getSex());

        Teacher teacher1 = teacherRepository.save(teacher);
        teacher1.setPassword(null);
        teacher1.setCode(null);
        return teacher1;
    }

    @Override
    public Student editStudentBaseData(String ID,Student newStudentData) {
        Student student = studentRepository.findById(ID).get();

        student.setName(newStudentData.getName());
        student.setQq(newStudentData.getQq());
        student.setSex(newStudentData.getSex());
        student.setWeixin(newStudentData.getWeixin());

        Student student1 = studentRepository.save(student);
        student1.setPassword(null);
        student1.setCode(null);
        return student1;
    }
    //更新教师邮箱
    @Override
    public Teacher updateTeacherEmail(String ID,Map<String, Object> params) {
        //邮箱验证：验证码和邮箱是否一致；
        String newEmail = (String) params.get("newEmail");
        String code = (String) params.get("code");
        if (!sendMailService.verification(newEmail, code)) {
            log.info("邮箱验证不一致");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"邮箱验证失败");
        }
        //邮箱验证：是否唯一
        Optional<Teacher> emailList = teacherRepository.findByEmail(newEmail);
        if (emailList.isPresent() && !emailList.get().getTea_id().equals(ID)) {
            log.info("该邮箱已被注册");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"该邮箱已被注册");
        }
        Teacher teacher = teacherRepository.findById(ID).get();
        teacher.setEmail(newEmail);
        Teacher newTeacher = teacherRepository.save(teacher);
        newTeacher.setPassword(null);
        newTeacher.setCode(null);
        return newTeacher;

    }
    //更新学生邮箱
    @Override
    public Student updateStudentEmail(String ID,Map<String, Object> params) {
        //邮箱验证：验证码和邮箱是否一致；
        String newEmail = (String) params.get("newEmail");
        String code = (String) params.get("code");
        if (!sendMailService.verification(newEmail, code)) {
            log.info("邮箱验证不一致");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"邮箱验证失败");
        }
        //邮箱验证：是否唯一
        Optional<Student> emailList = studentRepository.findByEmail(newEmail);
        if (emailList.isPresent() && !emailList.get().getStu_id().equals(ID)) {
            log.info("该邮箱已被注册");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"该邮箱已被注册");
        }
        Student student = studentRepository.findById(ID).get();
        student.setEmail(newEmail);
        Student newStudent =studentRepository.save(student);
        newStudent.setPassword(null);
        newStudent.setCode(null);
        return newStudent;
    }
    //确保本人操作 或者 验证新的邮箱是否使用
    @Override
    public void checkStudentEmail(String email) {
        if (!(email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+"))) {
            log.info("邮箱格式不正确");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"邮箱格式不正确");
        }
        try {
            sendMailService.sendEmail(email);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //确保本人操作 或者 验证新的邮箱是否使用
    @Override
    public void checkTeacherEmail(String email) {
        //如果邮箱格式不正确（正则表达式验证）
        String result = email.substring(email.length()-13,email.length());
        if (!(email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+") && result.equals("@sicnu.edu.cn"))) {
//            if (!(email.matches("[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+"))) {
            log.info("邮箱格式不正确");
            throw new CustomException(CustomExceptionType.USER_INPUT_ERROR,"邮箱格式不正确");
        }
        try {
            sendMailService.sendEmail(email);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
